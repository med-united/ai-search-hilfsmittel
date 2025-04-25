package de.service.health.hilfsmittel.server.rest;

import de.service.health.hilfsmittel.server.openai.EmbeddingService;
import de.service.health.hilfsmittel.server.openai.PineconeService;
import de.service.health.hilfsmittel.xsd.HMVPRODUKTCtp;
import jakarta.enterprise.context.RequestScoped;
import jakarta.inject.Inject;
import jakarta.ws.rs.GET;
import jakarta.ws.rs.Path;
import jakarta.ws.rs.Produces;
import jakarta.ws.rs.QueryParam;
import jakarta.ws.rs.core.MediaType;

import java.util.List;

@RequestScoped
@Path("/equipment")
public class Equipment {

    @Inject
    EmbeddingService embeddingService;

    @Inject
    PineconeService pineconeService;

    @GET
    @Produces(MediaType.APPLICATION_JSON)
    public List<HMVPRODUKTCtp> search(@QueryParam("q") String query) throws Exception {
        List<Double> embedding = embeddingService.getEmbedding(query);
        return pineconeService.query(embedding, 5);
    }
}
