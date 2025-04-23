package de.service.health.hilfsmittel.server;

import de.service.health.hilfsmittel.server.openai.EmbeddingService;
import io.quarkus.test.junit.QuarkusTest;
import io.quarkus.test.junit.TestProfile;
import jakarta.inject.Inject;
import org.junit.jupiter.api.Test;

import java.util.List;

import static org.junit.jupiter.api.Assertions.assertFalse;

@QuarkusTest
@TestProfile(AiTestProfile.class)
public class EmbeddingIT {

    @Inject
    EmbeddingService embeddingService;

    @Test
    public void embeddingIsCreated() throws Exception {
        String merkmale = "Netzunabhängiges AbsauggerätArtikel: Tracheoport® ProGröße (H x B x T): 243 x 286 x 118mm mit SekretbehälterGewicht: 3,3 kgSaugleistung: 27 l/min ± 3 l/minEndvakuum: -80 kPa ± 5 KPa (-800 mbar ± 50 mbar/ -600 mmHg ± 37,5 mmHg)Vakuumanzeige: Manometer (- 1...0 bar (± 2,5 % vom Endwert))Betriebszeit: Intervallbetrieb (max. 30 Minuten \"AN\" ; min. 30 Minuten \"AUS\")Anschlussspannung: 100 - 240 V~ ± 10 %; 50/60 HzNetzteil: Steckernetzteil | Hersteller: GlobTek, Inc. Modell: GTM46402-3713.4Lieferumfang: 1 x Grundgerät mit Sekretbehälter, 2 x Bakterien- und Virenfilter, 1 x Absaugschlauch (TRACHFLOW® Line Pro) mit Fingertip, 1 x Schlauchadapter, 1 x Steckernetzteil,  1 x Gebrauchsanweisung";
        List<Double> embedding = embeddingService.getEmbedding(merkmale);
        assertFalse(embedding.isEmpty());
    }
}
