package de.service.health.hilfsmittel.server.equipment;

import com.fasterxml.jackson.databind.ObjectMapper;
import de.service.health.hilfsmittel.server.openai.EmbeddingService;
import de.service.health.hilfsmittel.server.openai.PineconeService;
import de.service.health.hilfsmittel.server.utils.ChecksumFile;
import de.service.health.hilfsmittel.xsd.HMVPRODUKTCtp;
import io.quarkus.runtime.StartupEvent;
import jakarta.enterprise.context.ApplicationScoped;
import jakarta.enterprise.event.Observes;
import jakarta.inject.Inject;
import jakarta.xml.bind.JAXBContext;
import jakarta.xml.bind.Unmarshaller;
import jakarta.xml.bind.annotation.XmlAccessType;
import jakarta.xml.bind.annotation.XmlAccessorType;
import jakarta.xml.bind.annotation.XmlElement;
import jakarta.xml.bind.annotation.XmlRootElement;
import lombok.Data;
import org.apache.commons.lang3.time.StopWatch;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.io.InputStream;
import java.io.StringReader;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collection;
import java.util.List;

import static java.lang.Integer.MAX_VALUE;
import static java.nio.charset.StandardCharsets.UTF_8;

@ApplicationScoped
public class EquipmentService {

    private static final Logger log = LoggerFactory.getLogger(EquipmentService.class.getName());

    public static final String XML_RESOURCE_PATH = "/20250228_HMV/20250228_HMV.xml";

    private static final ObjectMapper OBJECT_MAPPER = new ObjectMapper();
    
    private static final String HEADER = "<?xml version=\"1.0\" encoding=\"utf-8\"?><root xmlns:hv=\"GI4X:/xml-schema/ESOL-HMV/1.0\">";
    private static final String PRODUKT_START_TOKEN = "<hv:HMV_PRODUKT>";
    private static final String PRODUKT_END_TOKEN = "</hv:HMV_PRODUKT>";
    private static final String MERKMALE_TOKEN = "hv:MERKMALE";

    @Inject
    EmbeddingService embeddingService;

    @Inject
    PineconeService pineconeService;

    private final Unmarshaller unmarshaller;

    public EquipmentService() throws Exception {
        unmarshaller = JAXBContext.newInstance(Root.class).createUnmarshaller();
    }

    @Data
    @XmlRootElement(name = "root")
    @XmlAccessorType(XmlAccessType.FIELD)
    public static class Root {
        @XmlElement(name = "HMV_PRODUKT", namespace = "GI4X:/xml-schema/ESOL-HMV/1.0")
        private HMVPRODUKTCtp hmvProdukt;
    }

    void onStart(@Observes StartupEvent ev) {
        try {
            prepareVectors(XML_RESOURCE_PATH, MAX_VALUE, true);
        } catch (Exception e) {
            log.error("Error while preparing search base for " + XML_RESOURCE_PATH, e);
        }
    }

    public Collection<HMVPRODUKTCtp> prepareVectors(String path, int limit, boolean failFast) throws Exception {
        ChecksumFile checksumFile = new ChecksumFile();
        List<HMVPRODUKTCtp> products = new ArrayList<>();
        try (InputStream is = EquipmentService.class.getResourceAsStream(path)) {
            if (is == null) {
                throw new IllegalArgumentException("20250228_HMV.xml is not found");
            }
            String source = new String(is.readAllBytes());
            StopWatch watch = StopWatch.createStarted();
            List<String> rawProducts = Arrays.stream(source.split(PRODUKT_START_TOKEN))
                .filter(rawProduct -> rawProduct.contains(MERKMALE_TOKEN))
                .limit(limit).toList();
            for (String rawProduct : rawProducts) {
                String body = rawProduct.split(PRODUKT_END_TOKEN)[0].replace("\r\n", "").replace("\n", "").replace("\r", "");
                String rootSource = HEADER + PRODUKT_START_TOKEN + body + PRODUKT_END_TOKEN + "</root>";
                String error = processProduct(products, checksumFile, rootSource);
                if (error != null) {
                    log.error("Error while vector processing product, error=%s source=%s".formatted(error, rootSource));
                    if (failFast) {
                        break;
                    }
                }
            }
            log.info("Uploading %d products from 20250228_HMV.xml to Pinecone took %s".formatted(products.size(), watch.formatTime()));
        }
        return products;
    }

    private String processProduct(List<HMVPRODUKTCtp> products, ChecksumFile checksumFile, String rootSource) {
        int statusCode;
        try {
            Root root = (Root) unmarshaller.unmarshal(new StringReader(rootSource));
            HMVPRODUKTCtp product = root.getHmvProdukt();
            String merkmale = product.getMERKMALE();
            byte[] merkmaleBytes = merkmale.getBytes(UTF_8);
            String checksum = checksumFile.calculateChecksum(merkmaleBytes);
            boolean contains = checksumFile.contains(checksum);
            if (!contains) {
                List<Double> embedding = embeddingService.getEmbedding(merkmale);
                String metadata = OBJECT_MAPPER.writeValueAsString(product);
                statusCode = pineconeService.upsert(checksum, embedding, metadata);
                if (statusCode != 200) {
                    throw new IllegalStateException("https://pinecone.io [statusCode = %d]".formatted(statusCode));
                }
                checksumFile.appendChecksumFor(checksum);
            }
            products.add(product);
            return null;
        } catch (Exception e) {
            Throwable cause = getOriginalCause(e);
            return cause == null ? "Unknown error" : cause.getMessage();
        }
    }

    public static Throwable getOriginalCause(Exception exception) {
        Throwable cause = exception;
        while (cause.getCause() != null) {
            cause = cause.getCause();
        }
        return cause;
    }
}
