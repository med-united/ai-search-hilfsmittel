package de.service.health.hilfsmittel.server.rest;

import de.service.health.hilfsmittel.server.openai.EmbeddingService;
import de.service.health.hilfsmittel.server.openai.PineconeService;
import jakarta.enterprise.context.RequestScoped;
import jakarta.inject.Inject;
import jakarta.ws.rs.GET;
import jakarta.ws.rs.Path;
import jakarta.ws.rs.Produces;
import jakarta.ws.rs.QueryParam;
import jakarta.ws.rs.core.MediaType;
import jakarta.ws.rs.core.Response;

import java.util.List;

@RequestScoped
@Path("/vector")
public class Vector {

    @Inject
    EmbeddingService embeddingService;

    @Inject
    PineconeService pineconeService;

    @GET
    @Path("/search")
    @Produces(MediaType.APPLICATION_JSON)
    public Response search(@QueryParam("q") String query) throws Exception {
        List<Double> embedding = embeddingService.getEmbedding(query);
        List<String> results = pineconeService.query(embedding, 5);
        return Response.ok(results).build();
    }
}
