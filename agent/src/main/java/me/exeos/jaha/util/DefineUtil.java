package me.exeos.jaha.util;

import org.objectweb.asm.ClassReader;

import java.io.IOException;

public class DefineUtil {

    public static void define(Class<?> clazz, ClassLoader loader) {
        try {
            ClassReader cr = new ClassReader(clazz.getName());
            NativeDefine.defineBootstrapClass(clazz.getName().replace(".", "/"), cr.b, loader);
        } catch (IOException e) {
            throw new RuntimeException(e);
        }
    }
}
