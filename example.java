package com.example;

import jakarta.enterprise.context.ApplicationScoped; import jakarta.inject.Inject; import jakarta.ws.rs.; import jakarta.ws.rs.core.MediaType; import software.amazon.awssdk.enhanced.dynamodb.; import software.amazon.awssdk.enhanced.dynamodb.model.; import software.amazon.awssdk.services.dynamodb.DynamoDbAsyncClient; import software.amazon.awssdk.services.dynamodb.model.; import io.smallrye.mutiny.Uni; import io.smallrye.mutiny.Multi;

import java.time.Instant; import java.util.*; import java.util.concurrent.CompletableFuture; import java.util.stream.Collectors;

@Path("/events") @Produces(MediaType.APPLICATION_JSON) @Consumes(MediaType.APPLICATION_JSON) public class LoginEventResource {

@Inject
LoginEventRepository repository;

@GET
public Uni<List<LoginEvent>> getAll() {
    return repository.findAll();
}

@GET
@Path("/first")
public Uni<LoginEvent> getFirst() {
    return repository.findAll().onItem().transform(list -> list.isEmpty() ? null : list.get(0));
}

@GET
@Path("/{id}")
public Uni<LoginEvent> getById(@PathParam("id") String id) {
    return repository.findById(id);
}

@GET
@Path("/by")
public Uni<List<LoginEvent>> getBy(@QueryParam("field") String field, @QueryParam("value") String value) {
    return repository.findByField(field, value);
}

@POST
public Uni<Void> add(LoginEvent event) {
    return repository.saveOrUpdate(event);
}

@POST
@Path("/batch")
public Uni<Void> addRange(List<LoginEvent> events) {
    return repository.saveAll(events);
}

@DELETE
@Path("/{id}")
public Uni<Void> delete(@PathParam("id") String id) {
    return repository.delete(id);
}

}

@ApplicationScoped class LoginEventRepository {

private final DynamoDbAsyncTable<LoginEvent> table;

@Inject
public LoginEventRepository(DynamoDbAsyncClient client) {
    DynamoDbEnhancedAsyncClient enhancedClient = DynamoDbEnhancedAsyncClient.builder()
        .dynamoDbClient(client)
        .build();

    this.table = enhancedClient.table("LoginEventTable", TableSchema.fromBean(LoginEvent.class));
}

public Uni<Void> saveOrUpdate(LoginEvent event) {
    return Uni.createFrom().completionStage(table.putItem(event));
}

public Uni<Void> saveAll(List<LoginEvent> events) {
    List<CompletableFuture<Void>> futures = events.stream()
        .map(table::putItem)
        .collect(Collectors.toList());
    return Uni.createFrom().completionStage(CompletableFuture.allOf(futures.toArray(new CompletableFuture[0])));
}

public Uni<LoginEvent> findById(String id) {
    Key key = Key.builder().partitionValue(id).build();
    return Uni.createFrom().completionStage(table.getItem(r -> r.key(key)));
}

public Uni<Void> delete(String id) {
    Key key = Key.builder().partitionValue(id).build();
    return Uni.createFrom().completionStage(table.deleteItem(r -> r.key(key))).replaceWithVoid();
}

public Uni<List<LoginEvent>> findAll() {
    return Multi.createFrom().publisher(table.scan().items())
        .collect().asList();
}

public Uni<List<LoginEvent>> findByField(String fieldName, String value) {
    // Simple scan + filter (not efficient, for demo)
    return findAll().onItem().transform(list ->
        list.stream()
            .filter(item -> {
                try {
                    Object field = LoginEvent.class.getMethod("get" + capitalize(fieldName)).invoke(item);
                    return value.equals(String.valueOf(field));
                } catch (Exception e) {
                    return false;
                }
            })
            .collect(Collectors.toList())
    );
}

private String capitalize(String str) {
    return str.substring(0, 1).toUpperCase() + str.substring(1);
}

}

@DynamoDbBean class LoginEvent { private String id; private String user; private Instant timestamp;

@DynamoDbPartitionKey
public String getId() { return id; }
public void setId(String id) { this.id = id; }

public String getUser() { return user; }
public void setUser(String user) { this.user = user; }

public Instant getTimestamp() { return timestamp; }
public void setTimestamp(Instant timestamp) { this.timestamp = timestamp; }

}

