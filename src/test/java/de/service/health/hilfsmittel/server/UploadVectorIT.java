package de.service.health.hilfsmittel.server;

import de.service.health.hilfsmittel.server.equipment.EquipmentService;
import de.service.health.hilfsmittel.xsd.HMVPRODUKTCtp;
import io.quarkus.test.junit.QuarkusTest;
import jakarta.inject.Inject;
import org.junit.jupiter.api.Test;

import java.util.Collection;
import java.util.Map;

import static io.restassured.RestAssured.given;
import static java.lang.Integer.MAX_VALUE;
import static org.hamcrest.Matchers.containsString;
import static org.junit.jupiter.api.Assertions.assertTrue;

@QuarkusTest
public class UploadVectorIT {

    @Inject
    EquipmentService equipmentService;

    String XML_RESOURCE_PATH = "/20250228_HMV/20250228_HMV.xml";

    @Test
    public void equipmentUploadedAndCanBeFound() throws Exception {
        Collection<HMVPRODUKTCtp> products = equipmentService.loadFromResource(XML_RESOURCE_PATH, MAX_VALUE);

        products.forEach(product -> {
            int status = equipmentService.prepareSearchBase(product);
            assertTrue(status != 500);

            given()
                .queryParams(Map.of("q", "Welches medizinische Gerät kann ich zum Absaugen von Sekreten verwenden?"))
                .when()
                .get("/vector/search")
                .then()
                .body(containsString(product.getBEZEICHNUNG()))
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
