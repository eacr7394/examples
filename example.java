public Uni<List<LoginEvent>> findByIp(String ip) {
    DynamoDbAsyncIndex<LoginEvent> ipIndex = table.index("IpIndex");

    QueryEnhancedRequest request = QueryEnhancedRequest.builder()
        .queryConditional(QueryConditional.keyEqualTo(k -> k.partitionValue(ip)))
        .build();

    SdkPublisher<Page<LoginEvent>> publisher = ipIndex.query(request).items();

    return Uni.createFrom().publisher(
        Multi.createFrom().publisher(publisher)
            .onItem().transformMulti(page -> Multi.createFrom().iterable(page.items()))
            .collect().asList()
    );
}