package de.service.health.hilfsmittel.server.equipment;

import de.service.health.hilfsmittel.xsd.HMVPRODUKTCtp;
import jakarta.xml.bind.JAXBContext;
import jakarta.xml.bind.Unmarshaller;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.io.InputStream;
import java.io.StringReader;
import java.util.Arrays;
import java.util.List;
import java.util.Objects;

import static de.service.health.hilfsmittel.server.utils.Utils.getOriginalCause;

public abstract class AbstractEquipmentService {

    protected final Logger log = LoggerFactory.getLogger(getClass().getName());

    public static final String XML_RESOURCE_PATH = "/20250228_HMV/20250228_HMV.xml";

    private static final String HEADER = "<?xml version=\"1.0\" encoding=\"utf-8\"?><root xmlns:hv=\"GI4X:/xml-schema/ESOL-HMV/1.0\">";
    private static final String PRODUKT_START_TOKEN = "<hv:HMV_PRODUKT>";
    private static final String PRODUKT_END_TOKEN = "</hv:HMV_PRODUKT>";
    private static final String MERKMALE_TOKEN = "hv:MERKMALE";

    private final Unmarshaller unmarshaller;

    public AbstractEquipmentService() throws Exception {
        unmarshaller = JAXBContext.newInstance(Root.class).createUnmarshaller();
    }

    public List<HMVPRODUKTCtp> loadProducts(String path, int limit) throws Exception {
        try (InputStream is = EquipmentService.class.getResourceAsStream(path)) {
            if (is == null) {
                throw new IllegalArgumentException("20250228_HMV.xml is not found");
            }
            String source = new String(is.readAllBytes());
            return Arrays.stream(source.split(PRODUKT_START_TOKEN))
                .filter(rawProduct -> rawProduct.contains(MERKMALE_TOKEN))
                .limit(limit)
                .map(rawProduct -> {
                    String body = rawProduct.split(PRODUKT_END_TOKEN)[0].replace("\r\n", "").replace("\n", "").replace("\r", "");
                    String rootSource = HEADER + PRODUKT_START_TOKEN + body + PRODUKT_END_TOKEN + "</root>";
                    try {
                        Root root = (Root) unmarshaller.unmarshal(new StringReader(rootSource));
                        return root.getHmvProdukt();
                    } catch (Exception e) {
                        Throwable cause = getOriginalCause(e);
                        log.error("Error while parsing RAW product, error=%s source=%s".formatted(cause.getMessage(), rootSource));
                        return null;
                    }
                })
                .filter(Objects::nonNull)
                .toList();
        }
    }
}
