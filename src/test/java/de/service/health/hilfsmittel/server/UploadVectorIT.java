package de.service.health.hilfsmittel.server;

import de.service.health.hilfsmittel.server.equipment.EquipmentService;
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
public class UploadVectorIT {

    @Inject
    EquipmentService equipmentService;

    @Test
    public void equipmentUploadedAndCanBeFound() throws Exception {
        Collection<HMVPRODUKTCtp> products = equipmentService.prepareVectors(XML_RESOURCE_PATH, 3, false);

        products.forEach(product -> {
            given()
                .queryParams(Map.of("q", "Welches medizinische Gerät kann ich zum Absaugen von Sekreten verwenden?"))
                .when()
                .get("/vector/search")
                .then()
                .body(containsString(product.getBEZEICHNUNG()))
                .body(containsString("HICO-Rapidovac 791-16; Art.-Nr.: 490060"))
                .body(containsString("Vacumaster M 20; Art.-Nr.: M 201"))
                .statusCode(200);
        });
    }

    @Test
    public void equipmentSearchWorks() {
        given()
            .queryParams(Map.of("q", "Gerät zum Absaugen von Körperflüssigkeiten im medizinischen Bereich"))
            .when()
            .get("/vector/search")
            .then()
            .body(containsString("HICO-Rapidovac"))
            .statusCode(200);

        given()
            .queryParams(Map.of("q", "Welches medizinische Gerät kann ich zum Absaugen von Sekreten verwenden?"))
            .when()
            .get("/vector/search")
            .then()
            .body(containsString("HICO-Rapidovac"))
            .statusCode(200);
    }
}
