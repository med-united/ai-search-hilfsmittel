package de.service.health.hilfsmittel.server.equipment;

import com.fasterxml.jackson.databind.ObjectMapper;
import de.service.health.hilfsmittel.server.openai.EmbeddingService;
import de.service.health.hilfsmittel.server.openai.PineconeService;
import de.service.health.hilfsmittel.xsd.HMVPRODUKTCtp;
import jakarta.enterprise.context.ApplicationScoped;
import jakarta.inject.Inject;
import jakarta.xml.bind.JAXBContext;
import jakarta.xml.bind.Unmarshaller;
import jakarta.xml.bind.annotation.XmlAccessType;
import jakarta.xml.bind.annotation.XmlAccessorType;
import jakarta.xml.bind.annotation.XmlElement;
import jakarta.xml.bind.annotation.XmlRootElement;
import lombok.Data;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.io.InputStream;
import java.io.StringReader;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collection;
import java.util.List;
import java.util.UUID;

@ApplicationScoped
public class EquipmentService {

    private static final Logger log = LoggerFactory.getLogger(EquipmentService.class.getName());

    private static final ObjectMapper OBJECT_MAPPER = new ObjectMapper();

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

    public Collection<HMVPRODUKTCtp> loadFromResource(String path, int limit) throws Exception {
        ArrayList<HMVPRODUKTCtp> products = new ArrayList<>();
        try (InputStream is = EquipmentService.class.getResourceAsStream(path)) {
            if (is == null) {
                throw new IllegalArgumentException("Source XML is absent");
            }

            String header = "<?xml version=\"1.0\" encoding=\"utf-8\"?><root xmlns:hv=\"GI4X:/xml-schema/ESOL-HMV/1.0\">";
            String source = new String(is.readAllBytes());

            Arrays.stream(source.split("<hv:HMV_PRODUKT>"))
                .filter(rawProduct -> rawProduct.contains("hv:MERKMALE"))
                .limit(limit)
                .forEach(rawProduct -> {
                    try {
                        String body = rawProduct.split("</hv:HMV_PRODUKT>")[0].replace("\r\n", "").replace("\n", "").replace("\r", "");
                        String rootSource = header + "<hv:HMV_PRODUKT>" + body + "</hv:HMV_PRODUKT></root>";
                        Root root = (Root) unmarshaller.unmarshal(new StringReader(rootSource));
                        products.add(root.getHmvProdukt());
                    } catch (Exception e) {
                        log.error("XML parsing is stopped", e);
                    }
                });
        }
        return products;
    }

    public int prepareSearchBase(HMVPRODUKTCtp product) {
        try {
            List<Double> embedding = embeddingService.getEmbedding(product.getMERKMALE());
            return pineconeService.upsert(UUID.randomUUID().toString(), embedding, OBJECT_MAPPER.writeValueAsString(product));
        } catch (Exception e) {
            log.error("Error while upserting vector", e);
            return 500;
        }
    }
}
