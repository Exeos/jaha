package me.exeos.jaha;

import me.exeos.jaha.annotations.Apply;
import me.exeos.jaha.annotations.Hook;
import me.exeos.jaha.runtime.MemberAccessor;
import me.exeos.jaha.runtime.NativeLoader;
import me.exeos.jaha.runtime.UnsafeUtil;
import me.exeos.jaha.util.ASMUtil;
import me.exeos.jaha.util.DefineUtil;
import me.exeos.jaha.util.NativeDefine;
import org.objectweb.asm.ClassReader;
import org.objectweb.asm.ClassWriter;
import org.objectweb.asm.Opcodes;
import org.objectweb.asm.tree.ClassNode;
import org.objectweb.asm.tree.MethodInsnNode;
import org.objectweb.asm.tree.MethodNode;
import org.objectweb.asm.tree.VarInsnNode;

import java.io.IOException;
import java.lang.instrument.Instrumentation;
import java.lang.instrument.UnmodifiableClassException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.util.HashMap;
import java.util.Map;

public class Jaha {

    public static final int ASM_VERSION = Opcodes.ASM9;

    private static final Map<String, ClassNode> hookSources = new HashMap<>();

    private static final MethodCloner methodCloner = new MethodCloner();

    public static void load(Instrumentation inst) {
        inst.addTransformer((loader, className, classBeingRedefined, protectionDomain, classfileBuffer) -> {
            if (isContainerClass(className)) {
                return classfileBuffer;
            }

            try {
                ClassNode hookSource = hookSources.get(className);
                if (hookSource == null) {
                    return classfileBuffer;
                }

                Map<String, MethodNode> hookedMethods = new HashMap<>();
                for (MethodNode methodNode : hookSource.methods) {
                    if (ASMUtil.hasAnnotation(methodNode, Apply.class.getName().replace(".", "/"))) {
                        hookedMethods.put(methodNode.name + methodNode.desc, methodNode);
                    }
                }

                ClassNode classNode = new ClassNode(ASM_VERSION);
                ClassReader cr = new ClassReader(classfileBuffer);
                cr.accept(classNode, 0);

                ClassNode container = methodCloner.getOrCreateContainer(loader);

                for (MethodNode methodNode : classNode.methods.toArray(new MethodNode[0])) {
                    String methodId = methodNode.name + methodNode.desc;
                    boolean isStatic = ASMUtil.hasAccess(methodNode.access, Opcodes.ACC_STATIC);

                    if (hookedMethods.containsKey(methodId)) {
                        MethodNode hookedMethod = hookedMethods.get(methodId);
                        MethodNode originalMethodClone = methodCloner.cloneMethod(loader, className, methodNode);

                        ASMUtil.loop(hookedMethod.instructions, insnNode -> {
                            if (!(insnNode instanceof MethodInsnNode)) {
                                return;
                            }

                            MethodInsnNode methodInsnNode = (MethodInsnNode) insnNode;
                            if (!methodInsnNode.owner.equals(Jaha.class.getName().replace(".", "/")) || !methodInsnNode.name.startsWith("callOriginal")) {
                                return;
                            }

                            if (!isStatic) {
                                hookedMethod.instructions.insertBefore(insnNode, new VarInsnNode(Opcodes.ALOAD, 0));
                            }
                            hookedMethod.instructions.insertBefore(insnNode, new MethodInsnNode(Opcodes.INVOKESTATIC, container.name, originalMethodClone.name, originalMethodClone.desc));
                            hookedMethod.instructions.remove(insnNode);
                        });

                        classNode.methods.remove(methodNode);
                        classNode.methods.add(hookedMethod);
                    }
                }

                ClassWriter cw = new ClassWriter(ClassWriter.COMPUTE_MAXS | ClassWriter.COMPUTE_FRAMES);
                classNode.accept(cw);

                return cw.toByteArray();
            } catch (Throwable t) {
                t.printStackTrace();
            }

            return classfileBuffer;
        }, true);

        for (String className : hookSources.keySet()) {
            try {
                inst.retransformClasses(Class.forName(className.replace("/", ".")));
            } catch (UnmodifiableClassException | ClassNotFoundException e) {
                throw new RuntimeException(e);
            }
        }

        for (Map.Entry<ClassLoader, ClassNode> entry : methodCloner.getContainers().entrySet()) {
            ClassLoader targetLoader = entry.getKey();
            ClassNode container = entry.getValue();

            ClassWriter cloneContainerWriter = new ClassWriter(ClassWriter.COMPUTE_MAXS | ClassWriter.COMPUTE_FRAMES);
            container.accept(cloneContainerWriter);
            byte[] cloneContainerData = cloneContainerWriter.toByteArray();

            Path path = Paths.get(container.name);
            try {
                Files.write(path, cloneContainerData);
            } catch (IOException e) {
                throw new RuntimeException(e);
            }
            NativeDefine.defineBootstrapClass(container.name, cloneContainerData, targetLoader);
            // define runtime classes with bootstrap cl
        }

        // define runtime classes with bootstrap cl
        DefineUtil.define(NativeDefine.class, null);
        DefineUtil.define(NativeLoader.class, null);
        DefineUtil.define(UnsafeUtil.class, null);
        DefineUtil.define(MemberAccessor.class, null);
    }

    public static void register(Class<?> hookSource) {
        if (!hookSource.isAnnotationPresent(Hook.class)) {
            throw new IllegalArgumentException(hookSource.getName() + " must be annotated with @Hook");
        }

        String targetClassName = hookSource.getDeclaredAnnotation(Hook.class).target();
        if (targetClassName.isEmpty()) {
            throw new IllegalArgumentException(hookSource.getName() + " doesn’t define class to be hooked");
        }

        ClassNode classNode = new ClassNode(ASM_VERSION);
        try {
            ClassReader cr = new ClassReader(hookSource.getName());
            cr.accept(classNode, ClassReader.SKIP_DEBUG);
        } catch (IOException e) {
            throw new RuntimeException("Failed to parse ClassNode from hook source: " + hookSource.getName(), e);
        }

        hookSources.put(targetClassName, classNode);
    }

    public static Object callOriginalObjectMethod(Object... params) {
        throw new IllegalStateException("This should never be called");
    }

    private static boolean isContainerClass(String className) {
        for (ClassNode container : methodCloner.getContainers().values()) {
            if (container.name.equals(className)) {
                return true;
            }
        }
        return false;
    }
}
