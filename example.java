public Uni<List<T>> findBy(DynamoColumn column) {
    if (column == null || column.getValue() == null)
        return Uni.createFrom().failure(new IllegalArgumentException("Column is required"));

    // Si es índice, usamos index
    if (column.isIndex()) {
        DynamoDbAsyncIndex<T> index = table.index(column.getName());

        Key.Builder keyBuilder = Key.builder();

        if (column.isPk()) {
            keyBuilder.partitionValue(column.getValue());
        }

        if (column.isSk()) {
            keyBuilder.sortValue(column.getValue()); // O usar beginsWith, según necesidad
        }

        QueryEnhancedRequest request = QueryEnhancedRequest.builder()
            .queryConditional(QueryConditional.keyEqualTo(keyBuilder.build()))
            .build();

        return Multi.createFrom().publisher(index.query(request))
            .onItem().transformToMulti(p -> Multi.createFrom().iterable(p.items()))
            .collect().asList();
    }

    // Si no es índice, hacemos un scan + filtro por reflexión (más costoso)
    return findAll().onItem().transform(list ->
        list.stream()
            .filter(item -> {
                try {
                    String getterName = "get" + column.getName().substring(0, 1).toUpperCase() + column.getName().substring(1);
                    Object value = item.getClass().getMethod(getterName).invoke(item);
                    return value != null && value.toString().equals(column.getValue());
                } catch (Exception e) {
                    return false;
                }
            })
            .collect(Collectors.toList())
    );
}