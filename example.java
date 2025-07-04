import com.fasterxml.jackson.databind.ObjectMapper;
import software.amazon.awssdk.services.dynamodb.DynamoDbAsyncClient;
import software.amazon.awssdk.services.dynamodb.model.AttributeValue;
import software.amazon.awssdk.services.dynamodb.model.ScanRequest;
import software.amazon.awssdk.services.dynamodb.model.ScanResponse;

import java.util.*;
import java.util.concurrent.CompletableFuture;
import java.util.concurrent.atomic.AtomicReference;
import java.util.function.Consumer;

public class PaginatedAsyncDynamoScanner<T> {

    private final DynamoDbAsyncClient client;
    private final ScanRequest baseRequest;
    private final Class<T> entityClass;
    private final ObjectMapper objectMapper;

    public PaginatedAsyncDynamoScanner(DynamoDbAsyncClient client, ScanRequest baseRequest,
                                       Class<T> entityClass, ObjectMapper objectMapper) {
        this.client = client;
        this.baseRequest = baseRequest;
        this.entityClass = entityClass;
        this.objectMapper = objectMapper;
    }

    public CompletableFuture<Void> scanAllAsync(Consumer<T> onItem) {
        AtomicReference<Map<String, AttributeValue>> lastKey = new AtomicReference<>(null);
        CompletableFuture<Void> future = new CompletableFuture<>();

        scanPage(lastKey.get(), onItem, future, lastKey);
        return future;
    }

    private void scanPage(Map<String, AttributeValue> startKey,
                          Consumer<T> onItem,
                          CompletableFuture<Void> future,
                          AtomicReference<Map<String, AttributeValue>> lastKeyRef) {

        ScanRequest.Builder requestBuilder = baseRequest.toBuilder();
        if (startKey != null && !startKey.isEmpty()) {
            requestBuilder.exclusiveStartKey(startKey);
        }

        client.scan(requestBuilder.build()).whenComplete((response, throwable) -> {
            if (throwable != null) {
                future.completeExceptionally(throwable);
                return;
            }

            try {
                for (Map<String, AttributeValue> item : response.items()) {
                    T value = objectMapper.convertValue(item, entityClass);
                    onItem.accept(value);
                }

                Map<String, AttributeValue> lastEvaluatedKey = response.lastEvaluatedKey();
                if (lastEvaluatedKey == null || lastEvaluatedKey.isEmpty()) {
                    future.complete(null);
                } else {
                    lastKeyRef.set(lastEvaluatedKey);
                    scanPage(lastEvaluatedKey, onItem, future, lastKeyRef);
                }
            } catch (Exception e) {
                future.completeExceptionally(e);
            }
        });
    }
} 


Ya tienes la clase PaginatedAsyncDynamoScanner<T> que escanea DynamoDB asincrónicamente, página por página, mapeando cada ítem a tu clase POJO (T) usando Jackson.


---

✅ ¿Cómo usarla?

ScanRequest request = ScanRequest.builder()
    .tableName("MiTabla")
    .limit(100)
    .build();

PaginatedAsyncDynamoScanner<MiEntidad> scanner = new PaginatedAsyncDynamoScanner<>(
    dynamoDbAsyncClient,
    request,
    MiEntidad.class,
    objectMapper
);

scanner.scanAllAsync(item -> {
    System.out.println("Item: " + item);
}).join(); // Esperas que termine


---

¿Quieres que devuelva un Uni<Void> (usando Mutiny de Quarkus) en lugar de CompletableFuture<Void>? Puedo adaptarlo para integración nativa con reactive programming.

