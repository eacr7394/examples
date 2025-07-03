else {
    if (Collection.class.isAssignableFrom(type)) {
        // Obtener tipo T desde List<T>
        Type genericType = field.getGenericType();
        if (genericType instanceof ParameterizedType parameterizedType) {
            Type elementType = parameterizedType.getActualTypeArguments()[0];
            if (elementType instanceof Class<?> elementClass) {
                StaticTableSchema<?> nestedSchema = getStaticTableSchema(elementClass);

                schema.addAttribute(
                    StaticAttribute.builder(
                        EnhancedType.listOf(
                            EnhancedType.documentOf(elementClass, nestedSchema)
                        )
                    )
                    .name(fieldName)
                    .getter(obj -> ReflectionUtil.get(field, obj))
                    .setter((obj, val) -> ReflectionUtil.set(field, obj, val))
                );
            }
        }
    } else {
        // Tipo complejo (objeto embebido no lista)
        StaticTableSchema<?> nestedSchema = getStaticTableSchema(type);

        schema.addAttribute(
            StaticAttribute.builder(
                EnhancedType.documentOf(type, nestedSchema)
            )
            .name(fieldName)
            .getter(obj -> ReflectionUtil.get(field, obj))
            .setter((obj, val) -> ReflectionUtil.set(field, obj, val))
        );
    }
}