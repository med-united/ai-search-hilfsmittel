package de.service.health.hilfsmittel.server.openai;

import de.service.health.hilfsmittel.xsd.HMVPRODUKTCtp;
import jakarta.enterprise.context.ApplicationScoped;
import jakarta.inject.Inject;
import jakarta.json.Json;
import jakarta.json.JsonArray;
import jakarta.json.JsonObject;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.io.StringReader;
import java.net.URI;
import java.net.http.HttpClient;
import java.net.http.HttpRequest;
import java.net.http.HttpResponse;
import java.util.List;
import java.util.Objects;
import java.util.stream.Collectors;

import static de.service.health.hilfsmittel.server.utils.Utils.as;

@ApplicationScoped
public class PineconeService {

    private static final Logger log = LoggerFactory.getLogger(PineconeService.class.getName());

    @Inject
    OpenAIConfig openAIConfig;

    HttpClient client = HttpClient.newHttpClient();

    public int upsert(String id, List<Double> embedding, String metadata) throws Exception {
        String vectorJson = embedding.stream()
            .map(Object::toString)
            .collect(Collectors.joining(","));

        String body = """
            {
              "vectors": [
                {
                  "id": "%s",
                  "values": [%s],
                  "metadata": { "text": "%s" }
                }
              ]
            }
            """.formatted(id, vectorJson, metadata.replace("\"", "\\\""));

        HttpRequest request = HttpRequest.newBuilder()
            .uri(URI.create(openAIConfig.getPineconeIndexUrl() + "/vectors/upsert"))
            .header("Api-Key", openAIConfig.getPineconeApiKey())
            .header("Content-Type", "application/json")
            .POST(HttpRequest.BodyPublishers.ofString(body))
            .build();

        HttpResponse<String> response = client.send(request, HttpResponse.BodyHandlers.ofString());
        return response.statusCode();
    }

    public int upsert(String vectorArray) throws Exception {
        String body = """
            {
              "vectors": %s
            }
            """.formatted(vectorArray);

        HttpRequest request = HttpRequest.newBuilder()
            .uri(URI.create(openAIConfig.getPineconeIndexUrl() + "/vectors/upsert"))
            .header("Api-Key", openAIConfig.getPineconeApiKey())
            .header("Content-Type", "application/json")
            .POST(HttpRequest.BodyPublishers.ofString(body))
            .build();

        HttpResponse<String> response = client.send(request, HttpResponse.BodyHandlers.ofString());
        int statusCode = response.statusCode();
        if (statusCode == 200) {
            log.info(response.body());
        }
        return statusCode;
    }

    public List<HMVPRODUKTCtp> query(List<Double> embedding, int topK) throws Exception {
        String vectorJson = embedding.stream()
            .map(Object::toString)
            .collect(Collectors.joining(","));

        String body = """
            {
              "vector": [%s],
              "topK": %d,
              "includeMetadata": true
            }
            """.formatted(vectorJson, topK);

        HttpRequest request = HttpRequest.newBuilder()
            .uri(URI.create(openAIConfig.getPineconeIndexUrl() + "/query"))
            .header("Api-Key", openAIConfig.getPineconeApiKey())
            .header("Content-Type", "application/json")
            .POST(HttpRequest.BodyPublishers.ofString(body))
            .build();

        HttpResponse<String> response = client.send(request, HttpResponse.BodyHandlers.ofString());
        JsonObject json = Json.createReader(new StringReader(response.body())).readObject();
        JsonArray matches = json.getJsonArray("matches");

        return matches.stream()
            .map(v -> ((JsonObject) v).getJsonObject("metadata").getString("text"))
            .map(source -> {
                try {
                    return as(source, HMVPRODUKTCtp.class);
                } catch (Exception e) {
                    log.error("Error while parsing %s -> %s".formatted(source, e.getMessage()));
                    return null;
                }
            })
            .filter(Objects::nonNull)
            .toList();
    }
}
