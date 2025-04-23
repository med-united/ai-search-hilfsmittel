package de.service.health.hilfsmittel.server.equipment;

import com.fasterxml.jackson.databind.node.ArrayNode;
import com.fasterxml.jackson.databind.node.ObjectNode;
import de.service.health.hilfsmittel.server.openai.EmbeddingService;
import de.service.health.hilfsmittel.server.openai.OpenAIConfig;
import de.service.health.hilfsmittel.server.openai.PineconeService;
import de.service.health.hilfsmittel.server.utils.ChecksumFile;
import de.service.health.hilfsmittel.xsd.HMVPRODUKTCtp;
import io.quarkus.runtime.StartupEvent;
import jakarta.enterprise.context.ApplicationScoped;
import jakarta.enterprise.event.Observes;
import jakarta.inject.Inject;
import lombok.Data;
import lombok.EqualsAndHashCode;
import org.apache.commons.lang3.time.StopWatch;

import java.util.ArrayList;
import java.util.Collection;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Objects;
import java.util.Set;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.Future;
import java.util.concurrent.atomic.AtomicInteger;
import java.util.stream.Collectors;

import static de.service.health.hilfsmittel.server.utils.Utils.OBJECT_MAPPER;
import static de.service.health.hilfsmittel.server.utils.Utils.asString;
import static de.service.health.hilfsmittel.server.utils.Utils.createArrayNode;
import static de.service.health.hilfsmittel.server.utils.Utils.createObjectNode;
import static de.service.health.hilfsmittel.server.utils.Utils.getOriginalCause;
import static java.lang.Integer.MAX_VALUE;
import static java.nio.charset.StandardCharsets.UTF_8;

@ApplicationScoped
public class EquipmentServiceAsync extends AbstractEquipmentService {

    @Inject
    OpenAIConfig openAIConfig;

    @Inject
    EmbeddingService embeddingService;

    @Inject
    PineconeService pineconeService;

    private ExecutorService productExecutor;

    public EquipmentServiceAsync() throws Exception {
        super();
    }

    void onStart(@Observes StartupEvent ev) {
        if (openAIConfig.isOpenaiAsync()) {
            productExecutor = Executors.newFixedThreadPool(openAIConfig.getBatchSize());
            try {
                prepareVectors(XML_RESOURCE_PATH, MAX_VALUE, true);
            } catch (Exception e) {
                log.error("Error while preparing search base for " + XML_RESOURCE_PATH, e);
            }
        }
    }

    @Data
    @EqualsAndHashCode(of = {"id"})
    private static class ProductInfo {
        String id;
        HMVPRODUKTCtp product;
        List<Double> embedding;
        String error;

        public ProductInfo(String id, HMVPRODUKTCtp product) {
            this.id = id;
            this.product = product;
        }
    }

    public Collection<HMVPRODUKTCtp> prepareVectors(String path, int limit, boolean failFast) throws Exception {
        ChecksumFile checksumFile = new ChecksumFile();
        Set<String> checksums = checksumFile.getChecksums();
        List<HMVPRODUKTCtp> hmvproduktCtps = loadProducts(path, limit);
        List<ProductInfo> products = hmvproduktCtps.stream().map(product -> {
            String merkmale = product.getMERKMALE();
            byte[] merkmaleBytes = merkmale.getBytes(UTF_8);
            String checksum = checksumFile.calculateChecksum(merkmaleBytes);
            try {
                if (checksums.contains(checksum)) {
                    return null;
                } else {
                    return new ProductInfo(checksum, product);
                }
            } catch (Exception e) {
                throw new RuntimeException(e);
            }
        }).filter(Objects::nonNull).toList();

        StopWatch watch = StopWatch.createStarted();
        AtomicInteger counter = new AtomicInteger(checksums.size());
        int size = products.size();
        int batchSize = openAIConfig.getBatchSize();
        for (int i = 0; i < size; i += batchSize) {
            int toIndex = Math.min(i + batchSize, size);
            Set<ProductInfo> batchSet = new HashSet<>(products.subList(i, toIndex));
            String error = processProductBatchAsync(batchSet, checksumFile, counter);
            if (error != null) {
                String msg = "Vector processing error: %s".formatted(error);
                log.error(msg);
                if (failFast) {
                    break;
                }
            }
        }
        log.info("Async uploading %d products from 20250228_HMV.xml to Pinecone took %s".formatted(counter.get(), watch.formatTime()));
        return products.stream().map(productInfo -> productInfo.product).toList();
    }

    private Set<ProductInfo> prepareEmbeddings(Set<ProductInfo> batchList) {
        List<Future<ProductInfo>> futures = new ArrayList<>();
        for (ProductInfo product : batchList) {
            futures.add(productExecutor.submit(() -> {
                try {
                    String merkmale = product.product.getMERKMALE();
                    List<Double> embedding = embeddingService.getEmbedding(merkmale);
                    product.setEmbedding(embedding);
                    log.info("%s embeddings received".formatted(product.id));
                } catch (Exception e) {
                    product.setError(e.getMessage());
                }
                return product;
            }));
        }

        Set<ProductInfo> embedded = new HashSet<>();
        for (Future<ProductInfo> future : futures) {
            try {
                embedded.add(future.get());
            } catch (Exception e) {
                log.error("Error while getting embeddings", e);
            }
        }
        return embedded;
    }

    private String processProductBatchAsync(
        Set<ProductInfo> batchList,
        ChecksumFile checksumFile,
        AtomicInteger counter
    ) {
        if (batchList.isEmpty()) {
            return null;
        }
        try {
            Set<ProductInfo> embedded = prepareEmbeddings(batchList);
            String error = embedded.stream().map(ProductInfo::getError).filter(Objects::nonNull).collect(Collectors.joining(";"));
            if (!error.isEmpty()) {
                return error;
            }
            List<ObjectNode> vectorInfos = embedded.stream().map(p -> {
                try {
                    ArrayNode arrayNode = OBJECT_MAPPER.createArrayNode();
                    for (double dbl : p.embedding) {
                        arrayNode.add(dbl);
                    }
                    ObjectNode node = OBJECT_MAPPER.createObjectNode();
                    node.put("id", p.id);
                    node.set("values", arrayNode);
                    node.set("metadata", createObjectNode(Map.of("text", asString(p.product))));
                    return node;

                } catch (Exception e) {
                    log.error("Error while serializing ProductInfo", e);
                    return null;
                }
            }).filter(Objects::nonNull).toList();

            ArrayNode arrayNode = createArrayNode(vectorInfos);
            int statusCode = pineconeService.upsert(asString(arrayNode));
            if (statusCode != 200) {
                return "https://pinecone.io [statusCode = %d]".formatted(statusCode);
            }
            checksumFile.appendChecksumFor(embedded.stream().map(ProductInfo::getId).collect(Collectors.toSet()), counter);
            return null;
        } catch (Exception e) {
            Throwable cause = getOriginalCause(e);
            return cause == null ? "Unknown error" : cause.getMessage();
        }
    }
}
