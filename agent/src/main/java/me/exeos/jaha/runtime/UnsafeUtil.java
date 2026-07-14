package me.exeos.jaha.runtime;

import sun.misc.Unsafe;

import java.lang.reflect.Field;

public class UnsafeUtil {

    public static final Unsafe theUnsafe = getUnsafe();

    private static Unsafe getUnsafe() {
        try {
            Field unsafeField = Unsafe.class.getDeclaredField("theUnsafe");
            unsafeField.setAccessible(true);

            return (Unsafe) unsafeField.get(null);
        } catch (NoSuchFieldException | IllegalAccessException e) {
            throw new RuntimeException(e);
        }
    }
}
