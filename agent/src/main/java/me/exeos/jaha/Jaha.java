package me.exeos.jaha;

import me.exeos.jaha.annotations.Apply;
import me.exeos.jaha.annotations.Hook;
import me.exeos.jaha.runtime.MemberAccessor;
import me.exeos.jaha.runtime.NativeLoader;
import me.exeos.jaha.runtime.UnsafeAccess;
import me.exeos.jaha.util.ASMUtil;
import me.exeos.jaha.util.NativeDefine;
import org.objectweb.asm.tree.ClassNode;
import org.objectweb.asm.tree.MethodNode;

import java.lang.instrument.Instrumentation;
import java.lang.instrument.UnmodifiableClassException;
import java.util.HashMap;
import java.util.Map;

public class Jaha {

    private static final Map<String, ClassNode> hookSources = new HashMap<>();

    private static final MethodCloner methodCloner = new MethodCloner();

    public static void applyHooks(Instrumentation inst) {
        inst.addTransformer((loader, className, classBeingRedefined, protectionDomain, classfileBuffer) -> {
            ClassNode hookSource = hookSources.get(className);
            if (hookSource == null) {
                return classfileBuffer;
            }

            // parse class to be hooked into class node so we can transform it
            ClassNode targetClass = ASMUtil.cnFromBytes(classfileBuffer);
            // find all methods in hookSource annotated with @Apply
            Map<String, MethodNode> methodHookSources = getMethodHookSources(hookSource);

            for (MethodNode methodNode : targetClass.methods.toArray(new MethodNode[0])) {
                String methodId = methodNode.name + methodNode.desc;
                if (!methodHookSources.containsKey(methodId)) {
                    continue;
                }

                MethodNode hookMethodSource = methodHookSources.get(methodId);
                // create a clone of @methodNode, replace calls to callOriginal* in @hookMethodSource to cloned method
                methodCloner.cloneMethod(loader, className, methodNode, hookMethodSource);

                targetClass.methods.remove(methodNode);
                targetClass.methods.add(hookMethodSource);
            }

            return ASMUtil.getCNBytes(targetClass);
        }, true);

        // retransform hooked classes in case they were loaded before transformer was added
        for (String className : hookSources.keySet()) {
            try {
                inst.retransformClasses(Class.forName(className.replace("/", ".")));
            } catch (UnmodifiableClassException | ClassNotFoundException e) {
                throw new RuntimeException(e);
            }
        }

        defineRuntimeClasses();
    }

    public static void register(Class<?> hookSource) {
        if (!hookSource.isAnnotationPresent(Hook.class)) {
            throw new IllegalArgumentException(hookSource.getName() + " must be annotated with @Hook");
        }

        String targetClassName = hookSource.getDeclaredAnnotation(Hook.class).target();
        if (targetClassName.isEmpty()) {
            throw new IllegalArgumentException(hookSource.getName() + " doesn’t define class to be hooked");
        }

        hookSources.put(targetClassName, ASMUtil.cnFromClass(hookSource));
    }

    public static Object callOriginalObjectMethod(Object... params) {
        throw new IllegalStateException("This should never be called");
    }

    /**
     * Defines classes required @runtime using the bootstrap classloader
     */
    private static void defineRuntimeClasses() {
        methodCloner.defineContainerClasses();
        NativeDefine.defineClass(NativeLoader.class, null);
        NativeDefine.defineClass(UnsafeAccess.class, null);
        NativeDefine.defineClass(MemberAccessor.class, null);
    }

    private static Map<String, MethodNode> getMethodHookSources(ClassNode hookSource) {
        Map<String, MethodNode> hookedMethods = new HashMap<>();
        for (MethodNode methodNode : hookSource.methods) {
            if (ASMUtil.hasAnnotation(methodNode, ASMUtil.getInternalName(Apply.class))) {
                hookedMethods.put(methodNode.name + methodNode.desc, methodNode);
            }
        }

        return hookedMethods;
    }
}
