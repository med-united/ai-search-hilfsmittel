package de.service.health.hilfsmittel.server.utils;

import jakarta.xml.bind.DatatypeConverter;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.io.File;
import java.io.FileOutputStream;
import java.security.MessageDigest;
import java.security.NoSuchAlgorithmException;
import java.util.HashSet;
import java.util.Set;
import java.util.concurrent.atomic.AtomicInteger;
import java.util.concurrent.locks.ReentrantReadWriteLock;
import java.util.stream.Collectors;

import static com.google.common.io.Files.readLines;
import static java.nio.charset.StandardCharsets.ISO_8859_1;

public class ChecksumFile {

    private static final Logger log = LoggerFactory.getLogger(ChecksumFile.class.getName());

    public static final String CHECKSUM_FILE_NAME = "sha256checksums";

    private final static ReentrantReadWriteLock lock = new ReentrantReadWriteLock();

    private final ThreadLocal<MessageDigest> digestThreadLocal;
    private final File file;

    public ChecksumFile() throws Exception {
        file = new File(CHECKSUM_FILE_NAME);
        digestThreadLocal = ThreadLocal.withInitial(() -> {
            try {
                return MessageDigest.getInstance("SHA-256");
            } catch (NoSuchAlgorithmException e) {
                throw new RuntimeException(e);
            }
        });
        if (!file.exists()) {
            log.info(String.format("Creating 'sha256checksums' in the folder '%s'", file.getAbsolutePath()));
            file.createNewFile();
        }
    }

    public Set<String> getChecksums() throws Exception {
        lock.readLock().lock();
        try {
            return new HashSet<>(readLines(file, ISO_8859_1));
        } finally {
            lock.readLock().unlock();
        }
    }

    public boolean contains(String checksum) throws Exception {
        lock.readLock().lock();
        try {
            HashSet<String> checksums = new HashSet<>(readLines(file, ISO_8859_1));
            return checksums.contains(checksum);
        } finally {
            lock.readLock().unlock();
        }
    }

    public String appendChecksumFor(String checksum) {
        lock.writeLock().lock();
        try {
            HashSet<String> checksums = new HashSet<>(readLines(file, ISO_8859_1));
            if (checksums.contains(checksum)) {
                return null;
            }
            try (FileOutputStream os = new FileOutputStream(file, true)) {
                String newLine = checksums.isEmpty() ? checksum : "\n" + checksum;
                os.write(newLine.getBytes());
            }
            return checksum;
        } catch (Exception e) {
            log.error("Unable to append 'sha256checksums' file", e);
        } finally {
            lock.writeLock().unlock();
        }
        return null;
    }

    public void appendChecksumFor(Set<String> checksumsList, AtomicInteger counter) {
        lock.writeLock().lock();
        try {
            HashSet<String> checksums = new HashSet<>(readLines(file, ISO_8859_1));
            String toSave = checksumsList.stream()
                .filter(checksum -> !checksums.contains(checksum))
                .peek(s -> log.info("[Produkt\t%d]\t\tadded".formatted(counter.incrementAndGet())))
                .collect(Collectors.joining("\n"));
            try (FileOutputStream os = new FileOutputStream(file, true)) {
                String newLine = checksums.isEmpty() ? toSave : "\n" + toSave;
                os.write(newLine.getBytes());
            }
        } catch (Exception e) {
            log.error("Unable to append 'sha256checksums' file", e);
        } finally {
            lock.writeLock().unlock();
        }
    }

    public String calculateChecksum(byte[] bytes) {
        MessageDigest digest = digestThreadLocal.get();
        try {
            digest.update(bytes);
            byte[] digestBytes = digest.digest();
            return DatatypeConverter.printHexBinary(digestBytes);
        } finally {
            digest.reset();
        }
    }
}
