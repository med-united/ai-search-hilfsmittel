package de.service.health.hilfsmittel.server;

import io.quarkus.test.junit.QuarkusTestProfile;

public class AiTestProfile implements QuarkusTestProfile {

    @Override
    public String getConfigProfile() {
        return "ai";
    }

    @Override
    public boolean disableApplicationLifecycleObservers() {
        return true;
    }
}
