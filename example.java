Perfecto. Vamos a implementar un repositorio estilo Panache pero reactivo para DynamoDB usando Quarkus, aunque AWS SDK v2 para DynamoDB no es nativamente reactivo. Pero podemos hacerlo pseudo-reactivo usando Mutiny y los ejecutores asíncronos de Quarkus.


---

✅ Objetivo:

Crear una clase base ReactiveDynamoRepository<T> que:

Use DynamoDbAsyncClient de AWS SDK v2.

Exponga métodos Uni<T> (Mutiny).

Soporte save, findById, delete.



---

📦 Paso 1: Dependencias

Agrega en tu pom.xml:

<dependency>
    <groupId>software.amazon.awssdk</groupId>
    <artifactId>dynamodb</artifactId>
</dependency>
<dependency>
    <groupId>io.quarkus</groupId>
    <artifactId>quarkus-mutiny</artifactId>
</dependency>


---

🧠 Paso 2: Clase genérica reactiva

import io.smallrye.mutiny.Uni;
import jakarta.enterprise.context.ApplicationScoped;
import software.amazon.awssdk.enhanced.dynamodb.*;
import software.amazon.awssdk.enhanced.dynamodb.model.*;
import software.amazon.awssdk.services.dynamodb.*;
import software.amazon.awssdk.services.dynamodb.model.*;

import java.util.concurrent.CompletableFuture;

@ApplicationScoped
public abstract class ReactiveDynamoRepository<T> {

    private final DynamoDbAsyncClient asyncClient;
    private final DynamoDbEnhancedAsyncClient enhancedClient;
    private final DynamoDbAsyncTable<T> table;

    protected ReactiveDynamoRepository(Class<T> clazz, String tableName) {
        this.asyncClient = DynamoDbAsyncClient.create();
        this.enhancedClient = DynamoDbEnhancedAsyncClient.builder()
            .dynamoDbClient(asyncClient)
            .build();
        this.table = enhancedClient.table(tableName, TableSchema.fromBean(clazz));
    }

    public Uni<Void> save(T entity) {
        CompletableFuture<Void> future = table.putItem(entity);
        return Uni.createFrom().completionStage(future);
    }

    public Uni<T> findById(String partitionKey, String sortKey) {
        Key key = Key.builder()
            .partitionValue(partitionKey)
            .sortValue(sortKey)
            .build();
        return Uni.createFrom().completionStage(table.getItem(r -> r.key(key)));
    }

    public Uni<Void> delete(String partitionKey, String sortKey) {
        Key key = Key.builder()
            .partitionValue(partitionKey)
            .sortValue(sortKey)
            .build();
        return Uni.createFrom().completionStage(table.deleteItem(r -> r.key(key))).replaceWithVoid();
    }
}


---

🧪 Paso 3: Crear tu entidad y repositorio

Modelo Java:

import software.amazon.awssdk.enhanced.dynamodb.mapper.annotations.*;

@DynamoDbBean
public class LoginEvent {
    private String userId;
    private String timestamp;
    private String ip;

    @DynamoDbPartitionKey
    public String getUserId() { return userId; }
    public void setUserId(String userId) { this.userId = userId; }

    @DynamoDbSortKey
    public String getTimestamp() { return timestamp; }
    public void setTimestamp(String timestamp) { this.timestamp = timestamp; }

    public String getIp() { return ip; }
    public void setIp(String ip) { this.ip = ip; }
}


---

Repositorio:

import jakarta.enterprise.context.ApplicationScoped;

@ApplicationScoped
public class LoginEventRepository extends ReactiveDynamoRepository<LoginEvent> {
    public LoginEventRepository() {
        super(LoginEvent.class, "UsersLogins");
    }
}


---

🧑‍💻 Uso en un recurso REST:

import jakarta.inject.Inject;
import jakarta.ws.rs.*;
import jakarta.ws.rs.core.MediaType;
import io.smallrye.mutiny.Uni;

@Path("/login-events")
@Produces(MediaType.APPLICATION_JSON)
@Consumes(MediaType.APPLICATION_JSON)
public class LoginEventResource {

    @Inject
    LoginEventRepository repo;

    @POST
    public Uni<Void> logEvent(LoginEvent event) {
        return repo.save(event);
    }

    @GET
    @Path("/{userId}/{timestamp}")
    public Uni<LoginEvent> getEvent(@PathParam("userId") String userId, @PathParam("timestamp") String ts) {
        return repo.findById(userId, ts);
    }
}


---

🧩 ¿Qué logramos?

Modelo limpio con anotaciones.

Repositorio genérico que puedes reusar.

Código reactivo compatible con Quarkus usando Mutiny.

Uso real de DynamoDbEnhancedAsyncClient (más expresivo que usar solo el cliente básico).



---

¿Deseas que también agreguemos soporte a búsquedas por GSI o paginación reactiva?

