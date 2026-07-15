package me.exeos.jaha.util;

import org.objectweb.asm.Type;

import java.util.HashMap;
import java.util.Map;

public class TypeUtil {

    private static final Map<Integer, Class<?>> TYPE_TO_CLASS = new HashMap<>();

    static {
        TYPE_TO_CLASS.put(Type.VOID, Void.class);
        TYPE_TO_CLASS.put(Type.BYTE, Byte.class);
        TYPE_TO_CLASS.put(Type.SHORT, Short.class);
        TYPE_TO_CLASS.put(Type.INT, Integer.class);
        TYPE_TO_CLASS.put(Type.LONG, Long.class);
        TYPE_TO_CLASS.put(Type.FLOAT, Float.class);
        TYPE_TO_CLASS.put(Type.DOUBLE, Double.class);
        TYPE_TO_CLASS.put(Type.BOOLEAN, Boolean.class);
        TYPE_TO_CLASS.put(Type.CHAR, Character.class);
        TYPE_TO_CLASS.put(Type.ARRAY, Object.class);
        TYPE_TO_CLASS.put(Type.OBJECT, Object.class);
    }

    public static boolean isPrimitive(Type type) {
        switch (type.getSort()) {
            case Type.BYTE:
            case Type.SHORT:
            case Type.INT:
            case Type.LONG:
            case Type.FLOAT:
            case Type.DOUBLE:
            case Type.BOOLEAN:
            case Type.CHAR:
            case Type.VOID:
                return true;
            default:
                return false;
        }
    }

    public static String getTypeClassInternalName(Type type) {
        if (!TYPE_TO_CLASS.containsKey(type.getSort())) {
            throw new IllegalArgumentException("Provided Type isn't mapped");
        }

        return ASMUtil.getInternalName(TYPE_TO_CLASS.get(type.getSort()));
    }

    public static String getTypeName(Type type) {
        if (!TYPE_TO_CLASS.containsKey(type.getSort())) {
            throw new IllegalArgumentException("Provided Type isn't mapped");
        }

        return TYPE_TO_CLASS.get(type.getSort()).getSimpleName();
    }
}
