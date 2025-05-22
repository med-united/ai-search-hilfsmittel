package de.service.health.hilfsmittel.server;

import de.service.health.hilfsmittel.server.equipment.EquipmentServiceAsync;
import de.service.health.hilfsmittel.xsd.HMVPRODUKTCtp;
import io.quarkus.test.junit.QuarkusTest;
import io.quarkus.test.junit.TestProfile;
import jakarta.inject.Inject;
import org.junit.jupiter.api.Test;

import java.util.List;
import java.util.Map;

import static de.service.health.hilfsmittel.server.equipment.EquipmentService.XML_RESOURCE_PATH;
import static io.restassured.RestAssured.given;
import static org.hamcrest.Matchers.containsString;
import static org.junit.jupiter.api.Assertions.assertFalse;

@QuarkusTest
@TestProfile(AiTestProfile.class)
public class UploadVectorAsyncIT {

    @Inject
    EquipmentServiceAsync equipmentService;

    @Test
    public void equipmentUploadedAndCanBeFound() throws Exception {
        List<HMVPRODUKTCtp> products = equipmentService.loadProducts(XML_RESOURCE_PATH, 100);
        assertFalse(products.isEmpty());

        equipmentService.prepareVectors(products, true);

        given()
            .queryParams(Map.of("q", "Welches medizinische Gerät kann ich zum Absaugen von Sekreten verwenden?"))
            .when()
            .get("/equipment")
            .then()
            .body(containsString("Servoport 3000 s Absauggerät netzabhängig"))
            .statusCode(200);
    }
}