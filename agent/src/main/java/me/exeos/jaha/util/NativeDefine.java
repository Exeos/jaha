package me.exeos.jaha.util;

import me.exeos.jaha.runtime.NativeLoader;

public class NativeDefine {

    static {
        NativeLoader.ensureLoaded();
    }

    public static native Class<?> defineClass(String name, byte[] data, ClassLoader loader);
}
