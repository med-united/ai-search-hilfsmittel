package de.service.health.hilfsmittel.server;

import de.service.health.hilfsmittel.server.equipment.EquipmentServiceAsync;
import de.service.health.hilfsmittel.xsd.HMVPRODUKTCtp;
import io.quarkus.test.junit.QuarkusTest;
import io.quarkus.test.junit.TestProfile;
import jakarta.inject.Inject;
import org.junit.jupiter.api.Test;

import java.util.Collection;
import java.util.Map;

import static de.service.health.hilfsmittel.server.equipment.EquipmentService.XML_RESOURCE_PATH;
import static io.restassured.RestAssured.given;
import static org.hamcrest.Matchers.containsString;

@QuarkusTest
@TestProfile(AiTestProfile.class)
public class UploadVectorAsyncIT {

    @Inject
    EquipmentServiceAsync equipmentService;

    @Test
    public void equipmentUploadedAndCanBeFound() throws Exception {
        Collection<HMVPRODUKTCtp> products = equipmentService.prepareVectors(XML_RESOURCE_PATH, 100, false);

        products.forEach(product -> given()
            .queryParams(Map.of("q", "Welches medizinische Gerät kann ich zum Absaugen von Sekreten verwenden?"))
            .when()
            .get("/vector/search")
            .then()
            .body(containsString("Ratiomed Sekret-Absauggerät AC20; Art.-Nr.: 8710401"))
            .statusCode(200)
        );
    }

}
