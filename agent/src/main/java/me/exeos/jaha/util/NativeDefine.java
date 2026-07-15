package me.exeos.jaha.util;

import me.exeos.jaha.runtime.NativeLoader;
import org.objectweb.asm.tree.ClassNode;

public class NativeDefine {

    static {
        NativeLoader.ensureLoaded();
    }

    /**
     * Define Class with provided ClassLoader via a native bridge
     *
     * @param name   Internal name of the Class to be defined
     * @param loader ClassLoader to use
     */
    public static native Class<?> define(String name, byte[] data, ClassLoader loader);

    /**
     * Define Class with provided ClassLoader via a native bridge
     *
     * @param clazz  Class to be defined
     * @param loader ClassLoader to use
     */
    public static void defineClass(Class<?> clazz, ClassLoader loader) {
        define(ASMUtil.getInternalName(clazz), ASMUtil.getClassBytes(clazz), loader);
    }

    public static void defineClassNode(ClassNode classNode, ClassLoader loader) {
        define(classNode.name, ASMUtil.getCNBytes(classNode), loader);
    }
}
