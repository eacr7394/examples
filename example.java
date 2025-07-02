import software.amazon.awssdk.enhanced.dynamodb.*;
import software.amazon.awssdk.services.dynamodb.DynamoDbAsyncClient;

public abstract class BaseDynamoRepository<T> {

    protected final DynamoDbEnhancedAsyncClient enhancedClient;
    protected final DynamoDbAsyncTable<T> table;

    protected BaseDynamoRepository(
        DynamoDbAsyncClient dynamoDbAsyncClient,
        Class<T> entityClass,
        String tableName
    ) {
        this.enhancedClient = DynamoDbEnhancedAsyncClient.builder()
            .dynamoDbClient(dynamoDbAsyncClient)
            .build();

        this.table = enhancedClient.table(tableName, TableSchema.fromBean(entityClass));
    }

    // Aquí vendrían tus métodos findBy(), findAll(), delete(), etc.
}