public static <T> StaticTableSchema<T> getStaticTableSchema(Class<T> clazz) {
    StaticTableSchema.Builder<T> schema = StaticTableSchema.builder(clazz)
        .newItemSupplier(() -> {
            try {
                return clazz.getDeclaredConstructor().newInstance();
            } catch (Exception e) {
                throw new RuntimeException("No se pudo instanciar " + clazz.getName(), e);
            }
        });

    for (var field : clazz.getDeclaredFields()) {
        field.setAccessible(true); // por si es private
        String fieldName = field.getName();
        Class<?> type = field.getType();

        // Necesitamos castear explícitamente a Class<X>
        @SuppressWarnings("unchecked")
        Class<Object> castedType = (Class<Object>) type;

        schema.addAttribute(castedType, a -> a
            .name(fieldName)
            .getter(obj -> {
                try {
                    field.setAccessible(true);
                    return field.get(obj);
                } catch (IllegalAccessException e) {
                    throw new RuntimeException(e);
                }
            })
            .setter((obj, val) -> {
                try {
                    field.setAccessible(true);
                    field.set(obj, val);
                } catch (IllegalAccessException e) {
                    throw new RuntimeException(e);
                }
            })
        );
    }

    return schema.build();
}