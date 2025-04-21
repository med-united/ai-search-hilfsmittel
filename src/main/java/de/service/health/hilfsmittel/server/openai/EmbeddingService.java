package de.service.health.hilfsmittel.server.openai;

import jakarta.enterprise.context.ApplicationScoped;
import jakarta.inject.Inject;
import jakarta.json.Json;
import jakarta.json.JsonArray;
import jakarta.json.JsonNumber;
import jakarta.json.JsonObject;

import java.io.StringReader;
import java.net.URI;
import java.net.http.HttpClient;
import java.net.http.HttpRequest;
import java.net.http.HttpResponse;
import java.util.List;

@ApplicationScoped
public class EmbeddingService {

    @Inject
    OpenAIConfig openAIConfig;

    HttpClient client = HttpClient.newHttpClient();

    public List<Double> getEmbedding(String text) throws Exception {
        String body = """
        {
          "input": "%s",
          "model": "%s"
        }
        """.formatted(text, openAIConfig.getOpenaiModel());

        HttpRequest request = HttpRequest.newBuilder()
            .uri(URI.create("https://api.openai.com/v1/embeddings"))
            .header("Authorization", "Bearer " + openAIConfig.getOpenaiKey())
            .header("Content-Type", "application/json")
            .POST(HttpRequest.BodyPublishers.ofString(body))
            .build();

        HttpResponse<String> response = client.send(request, HttpResponse.BodyHandlers.ofString());
        int statusCode = response.statusCode();
        if (statusCode == 200) {
            JsonObject json = Json.createReader(new StringReader(response.body())).readObject();
            JsonArray arr = json.getJsonArray("data").getJsonObject(0).getJsonArray("embedding");
            return arr.stream().map(v -> ((JsonNumber) v).doubleValue()).toList();
        }
        throw new IllegalStateException("https://api.openai.com/v1/embeddings [statusCode = %d]".formatted(statusCode));
    }
}
