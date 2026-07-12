package me.exeos.jaha;

import me.exeos.jaha.annotations.Apply;
import me.exeos.jaha.annotations.Hook;
import me.exeos.jaha.util.ASMUtil;
import org.objectweb.asm.ClassReader;
import org.objectweb.asm.ClassWriter;
import org.objectweb.asm.Opcodes;
import org.objectweb.asm.tree.ClassNode;
import org.objectweb.asm.tree.MethodNode;

import java.io.IOException;
import java.lang.instrument.Instrumentation;
import java.lang.instrument.UnmodifiableClassException;
import java.util.HashMap;
import java.util.Map;

public class Jaha {

    private static final Map<String, ClassNode> hooks = new HashMap<>();

    public static void load(Instrumentation inst) {
        inst.addTransformer((loader, className, classBeingRedefined, protectionDomain, classfileBuffer) -> {
            ClassNode hook = hooks.get(className);
            if (hook == null) {
                return classfileBuffer;
            }

            Map<String, MethodNode> toBeReplaced = new HashMap<>();
            for (MethodNode methodNode : hook.methods) {
                if (ASMUtil.hasAnnotation(methodNode, Apply.class.getName().replace(".", "/"))) {
                    toBeReplaced.put(methodNode.name + methodNode.desc, methodNode);
                }
            }

            ClassNode classNode = new ClassNode(Opcodes.ASM9);
            ClassReader cr = new ClassReader(classfileBuffer);
            cr.accept(classNode, 0);

            for (MethodNode methodNode : classNode.methods.toArray(new MethodNode[0])) {
                if (toBeReplaced.containsKey(methodNode.name + methodNode.desc)) {
                    classNode.methods.remove(methodNode);
                    classNode.methods.add(toBeReplaced.get(methodNode.name + methodNode.desc));
                }
            }

            ClassWriter cw = new ClassWriter(0);
            classNode.accept(cw);

            return cw.toByteArray();
        }, true);

        for (String className : hooks.keySet()) {
            try {
                inst.retransformClasses(Class.forName(className.replace("/", ".")));
            } catch (UnmodifiableClassException | ClassNotFoundException e) {
                throw new RuntimeException(e);
            }
        }
    }

    public static void register(Class<?> hook) {
        if (!hook.isAnnotationPresent(Hook.class)) {
            throw new IllegalArgumentException(hook.getName() + " must be annotated with @Hook");
        }

        String hookedClassName = hook.getDeclaredAnnotation(Hook.class).className();
        if (hookedClassName.isEmpty()) {
            throw new IllegalArgumentException(hook.getName() + " doesn’t define class to be hooked");
        }

        ClassNode classNode = new ClassNode(Opcodes.ASM9);
        try {
            ClassReader cr = new ClassReader(hook.getName());
            cr.accept(classNode, 0);
        } catch (IOException e) {
            throw new RuntimeException("Failed to parse hook", e);
        }

        hooks.put(hookedClassName, classNode);
    }
}
