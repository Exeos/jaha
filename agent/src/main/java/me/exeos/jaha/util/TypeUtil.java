package me.exeos.jaha.util;

import org.objectweb.asm.Type;

import java.util.HashMap;
import java.util.Map;

public class TypeUtil {

    private static final Map<Type, String> PRIMITIVE_TO_CLASS = new HashMap<>();

    static {
        PRIMITIVE_TO_CLASS.put(Type.BYTE_TYPE, "java/lang/Byte");
        PRIMITIVE_TO_CLASS.put(Type.SHORT_TYPE, "java/lang/Short");
        PRIMITIVE_TO_CLASS.put(Type.INT_TYPE, "java/lang/Integer");
        PRIMITIVE_TO_CLASS.put(Type.LONG_TYPE, "java/lang/Long");
        PRIMITIVE_TO_CLASS.put(Type.FLOAT_TYPE, "java/lang/Float");
        PRIMITIVE_TO_CLASS.put(Type.DOUBLE_TYPE, "java/lang/Double");
        PRIMITIVE_TO_CLASS.put(Type.BOOLEAN_TYPE, "java/lang/Boolean");
        PRIMITIVE_TO_CLASS.put(Type.CHAR_TYPE, "java/lang/Character");
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
                return true;
            default:
                return false;
        }
    }

    public static String getPrimitiveClassInternalName(Type type) {
        if (!isPrimitive(type)) {
            throw new IllegalArgumentException("Provided Type needs to be a primitive");
        }
        return PRIMITIVE_TO_CLASS.get(type);
    }
}
