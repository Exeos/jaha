package me.exeos.jaha.runtime;

import java.lang.reflect.Field;
import java.lang.reflect.Modifier;
import java.util.HashMap;
import java.util.Map;

public class MemberAccessor {

    private final static Map<String, Long> offsetCache = new HashMap<>();

    static {
        NativeLoader.ensureLoaded();
    }

    public static byte getByteField(Object ownerInstance, String owner, String name) {
        return UnsafeUtil.theUnsafe.getByte(ownerInstance, offset(owner, name));
    }

    public static short getShortField(Object ownerInstance, String owner, String name) {
        return UnsafeUtil.theUnsafe.getShort(ownerInstance, offset(owner, name));
    }

    public static int getIntField(Object ownerInstance, String owner, String name) {
        return UnsafeUtil.theUnsafe.getInt(ownerInstance, offset(owner, name));
    }

    public static long getLongField(Object ownerInstance, String owner, String name) {
        return UnsafeUtil.theUnsafe.getLong(ownerInstance, offset(owner, name));
    }

    public static float getFloatField(Object ownerInstance, String owner, String name) {
        return UnsafeUtil.theUnsafe.getFloat(ownerInstance, offset(owner, name));
    }

    public static double getDoubleField(Object ownerInstance, String owner, String name) {
        return UnsafeUtil.theUnsafe.getDouble(ownerInstance, offset(owner, name));
    }

    public static boolean getBooleanField(Object ownerInstance, String owner, String name) {
        return UnsafeUtil.theUnsafe.getBoolean(ownerInstance, offset(owner, name));
    }

    public static char getCharField(Object ownerInstance, String owner, String name) {
        return UnsafeUtil.theUnsafe.getChar(ownerInstance, offset(owner, name));
    }

    public static Object getObjectField(Object ownerInstance, String owner, String name) {
        return UnsafeUtil.theUnsafe.getObject(ownerInstance, offset(owner, name));
    }

    public static void setByteField(Object ownerInstance, byte value, String owner, String name) {
        UnsafeUtil.theUnsafe.putByte(ownerInstance, offset(owner, name), value);
    }

    public static void setShortField(Object ownerInstance, short value, String owner, String name) {
        UnsafeUtil.theUnsafe.putShort(ownerInstance, offset(owner, name), value);
    }

    public static void setIntField(Object ownerInstance, int value, String owner, String name) {
        UnsafeUtil.theUnsafe.putInt(ownerInstance, offset(owner, name), value);
    }

    public static void setLongField(Object ownerInstance, long value, String owner, String name) {
        UnsafeUtil.theUnsafe.putLong(ownerInstance, offset(owner, name), value);
    }

    public static void setFloatField(Object ownerInstance, float value, String owner, String name) {
        UnsafeUtil.theUnsafe.putFloat(ownerInstance, offset(owner, name), value);
    }

    public static void setDoubleField(Object ownerInstance, double value, String owner, String name) {
        UnsafeUtil.theUnsafe.putDouble(ownerInstance, offset(owner, name), value);
    }

    public static void setBooleanField(Object ownerInstance, boolean value, String owner, String name) {
        UnsafeUtil.theUnsafe.putBoolean(ownerInstance, offset(owner, name), value);
    }

    public static void setCharField(Object ownerInstance, char value, String owner, String name) {
        UnsafeUtil.theUnsafe.putChar(ownerInstance, offset(owner, name), value);
    }

    public static void setObjectField(Object ownerInstance, Object value, String owner, String name) {
        UnsafeUtil.theUnsafe.putObject(ownerInstance, offset(owner, name), value);
    }

    public static native void callVoidMethod(Object ownerInstance, Object[] args, String owner, String name, String desc);

    public static native Object callObjectMethod(Object ownerInstance, Object[] args, Class<?> ownerClass, String name, String desc);

    public static native byte callByteMethod(Object ownerInstance, Object[] args, String owner, String name, String desc);

    public static native short callShortMethod(Object ownerInstance, Object[] args, String owner, String name, String desc);

    public static native int callIntMethod(Object ownerInstance, Object[] args, String owner, String name, String desc);

    public static native long callLongMethod(Object ownerInstance, Object[] args, String owner, String name, String desc);

    public static native float callFloatMethod(Object ownerInstance, Object[] args, String owner, String name, String desc);

    public static native double callDoubleMethod(Object ownerInstance, Object[] args, String owner, String name, String desc);

    public static native boolean callBooleanMethod(Object ownerInstance, Object[] args, String owner, String name, String desc);

    public static native char callCharMethod(Object ownerInstance, Object[] args, String owner, String name, String desc);

    private static long offset(String owner, String name) {
        String cacheKey = owner + name;
        if (offsetCache.containsKey(cacheKey)) {
            return offsetCache.get(cacheKey);
        }

        Field f = getField(owner, name);
        long offset = Modifier.isStatic(f.getModifiers())
                ? UnsafeUtil.theUnsafe.staticFieldOffset(f)
                : UnsafeUtil.theUnsafe.objectFieldOffset(f);

        offsetCache.put(cacheKey, offset);
        return offset;
    }

    private static Field getField(String owner, String name) {
        try {
            return Class.forName(owner.replace("/", ".")).getDeclaredField(name);
        } catch (NoSuchFieldException | ClassNotFoundException e) {
            throw new RuntimeException(e);
        }
    }
}
