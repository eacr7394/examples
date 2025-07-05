public class PrimitiveDefaults {
    public static Object get(Class<?> clazz) {
        if (clazz == boolean.class || clazz == Boolean.class) return false;
        if (clazz == byte.class || clazz == Byte.class) return (byte) 0;
        if (clazz == short.class || clazz == Short.class) return (short) 0;
        if (clazz == char.class || clazz == Character.class) return '\u0000';
        if (clazz == int.class || clazz == Integer.class) return 0;
        if (clazz == long.class || clazz == Long.class) return 0L;
        if (clazz == float.class || clazz == Float.class) return 0.0f;
        if (clazz == double.class || clazz == Double.class) return 0.0d;
        return null;
    }
}