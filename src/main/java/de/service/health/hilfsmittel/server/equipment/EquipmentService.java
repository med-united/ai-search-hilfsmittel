package de.service.health.hilfsmittel.server.equipment;

import de.service.health.hilfsmittel.server.openai.EmbeddingService;
import de.service.health.hilfsmittel.server.openai.OpenAIConfig;
import de.service.health.hilfsmittel.server.openai.PineconeService;
import de.service.health.hilfsmittel.server.utils.ChecksumFile;
import de.service.health.hilfsmittel.xsd.HMVPRODUKTCtp;
import io.quarkus.runtime.StartupEvent;
import jakarta.enterprise.context.ApplicationScoped;
import jakarta.enterprise.event.Observes;
import jakarta.inject.Inject;
import org.apache.commons.lang3.time.StopWatch;

import java.io.File;
import java.util.Collection;
import java.util.List;
import java.util.Set;
import java.util.concurrent.atomic.AtomicInteger;

import static de.service.health.hilfsmittel.server.utils.Utils.asString;
import static de.service.health.hilfsmittel.server.utils.Utils.getOriginalCause;
import static java.lang.Integer.MAX_VALUE;
import static java.nio.charset.StandardCharsets.UTF_8;

@ApplicationScoped
public class EquipmentService extends AbstractEquipmentService {

    @Inject
    OpenAIConfig openAIConfig;

    @Inject
    EmbeddingService embeddingService;

    @Inject
    PineconeService pineconeService;

    public EquipmentService() throws Exception {
        super();
    }

    void onStart(@Observes StartupEvent ev) {
        if (openAIConfig.isOpenaiAsync()) {
            return;
        }
        try {
            prepareVectors(XML_RESOURCE_PATH, MAX_VALUE, true);
        } catch (Exception e) {
            log.error("Error while preparing search base for " + XML_RESOURCE_PATH, e);
        }
    }

    public Collection<HMVPRODUKTCtp> prepareVectors(String path, int limit, boolean failFast) throws Exception {
        ChecksumFile checksumFile = new ChecksumFile();
        List<HMVPRODUKTCtp> products = loadProducts(path, limit);

        StopWatch watch = StopWatch.createStarted();
        AtomicInteger counter = new AtomicInteger(0);
        for (HMVPRODUKTCtp product : products) {
            String error = processProduct(checksumFile, product, counter);
            if (error != null) {
                String msg = "Vector processing error: %s product=%s".formatted(error, asString(product));
                log.error(msg);
                if (failFast) {
                    break;
                }
            }
        }
        log.info("Uploading %d products from 20250228_HMV.xml to Pinecone took %s".formatted(products.size(), watch.formatTime()));
        return products;
    }

    private String processProduct(
        ChecksumFile checksumFile,
        HMVPRODUKTCtp product,
        AtomicInteger counter
    ) {
        int statusCode;
        try {
            counter.incrementAndGet();
            String merkmale = product.getMERKMALE();
            byte[] merkmaleBytes = merkmale.getBytes(UTF_8);
            String checksum = checksumFile.calculateChecksum(merkmaleBytes);
            boolean contains = checksumFile.contains(checksum);
            if (contains) {
                log.info("[Produkt\t%d]\t\talready exists".formatted(counter.get()));
            } else {
                List<Double> embedding = embeddingService.getEmbedding(merkmale);
                statusCode = pineconeService.upsert(checksum, embedding, asString(product));
                if (statusCode != 200) {
                    String msg = "https://pinecone.io [statusCode = %d]".formatted(statusCode);
                    throw new IllegalStateException(msg);
                }
                checksumFile.appendChecksumFor(checksum);
                log.info("[Produkt\t%d]\t\tadded".formatted(counter.get()));
            }
            return null;
        } catch (Exception e) {
            Throwable cause = getOriginalCause(e);
            return cause == null ? "Unknown error" : cause.getMessage();
        }
    }
}
