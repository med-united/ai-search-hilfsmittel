package de.service.health.hilfsmittel.server.openai;

import jakarta.enterprise.context.ApplicationScoped;
import lombok.Getter;
import org.eclipse.microprofile.config.inject.ConfigProperty;

@Getter
@ApplicationScoped
public class OpenAIConfig {

    @ConfigProperty(name = "openai.api.key")
    String openaiKey;

    @ConfigProperty(name = "openai.model")
    String openaiModel;

    @ConfigProperty(name = "pinecone.api.key")
    String pineconeApiKey;

    @ConfigProperty(name = "pinecone.index.url")
    String pineconeIndexUrl;
}