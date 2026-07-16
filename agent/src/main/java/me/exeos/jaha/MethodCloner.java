package me.exeos.jaha;

import me.exeos.jaha.runtime.MemberAccessor;
import me.exeos.jaha.util.ASMUtil;
import me.exeos.jaha.util.NativeDefine;
import me.exeos.jaha.util.TypeUtil;
import org.objectweb.asm.Opcodes;
import org.objectweb.asm.Type;
import org.objectweb.asm.tree.*;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.util.HashMap;
import java.util.Map;
import java.util.UUID;
import java.util.concurrent.atomic.AtomicInteger;

public class MethodCloner implements Opcodes {

    /**
     * Classes containing cloned methods, grouped by the {@link ClassLoader} of the original method.
     */
    private final Map<ClassLoader, ClassNode> containers = new HashMap<>();

    /**
     * Counter used for unique cloned method names.
     */
    private int clonedMethodCount = 0;

    public void cloneMethod(ClassLoader ownerLoader, String methodOwner, MethodNode originalMethod, MethodNode hookedMethod) {
        boolean isStatic = ASMUtil.hasAccess(originalMethod.access, ACC_STATIC);
        ClassNode container = getOrCreateContainer(ownerLoader);
        MethodNode clone = new MethodNode(
                ACC_PUBLIC | ACC_STATIC,
                String.valueOf(clonedMethodCount++),
                isStatic
                        ? originalMethod.desc
                        : originalMethod.desc.replace(")", "L" + methodOwner + ";)"),
                null,
                originalMethod.exceptions.toArray(new String[0])
        );
        clone.instructions = ASMUtil.clone(originalMethod.instructions);

        int lastArgSlot = ASMUtil.getLastArgumentSlot(clone.desc);
        if (!isStatic) {
            int argumentSize = ASMUtil.getArgumentsSize(clone.desc);
            // because method is no longer static, slot 0 no longer represents "this"
            ASMUtil.loop(clone.instructions, insnNode -> {
                if (insnNode instanceof VarInsnNode) {
                    VarInsnNode varInsnNode = (VarInsnNode) insnNode;
                    if (varInsnNode.var == 0) {
                        varInsnNode.var = lastArgSlot;
                    } else if (varInsnNode.var < argumentSize) {
                        varInsnNode.var--;
                    }
                }

                if (insnNode instanceof IincInsnNode) {
                    IincInsnNode iincInsnNode = (IincInsnNode) insnNode;
                    if (iincInsnNode.var < argumentSize) {
                        iincInsnNode.var--;
                    }
                }
            });
        }
        fixMemberAccess(clone);
        replaceCallsToDummyOriginal(container, hookedMethod, clone, isStatic, lastArgSlot);

        container.methods.add(clone);

        try {
            ClassNode cn = new ClassNode(ASMUtil.ASM_VERSION);
            cn.visit(Opcodes.V1_8, ACC_PUBLIC, methodOwner, null, "java/lang/Object", null);
            cn.methods.add(originalMethod);
            cn.methods.add(clone);

            Path path = Paths.get(System.getProperty("user.dir"), cn.name + "_" + container.name);
            System.out.println(path);
            Files.write(path, ASMUtil.getCNBytes(cn));
        } catch (IOException e) {
            throw new RuntimeException(e);
        }
    }

    /**
     * Defines all generated container classes using their associated {@link ClassLoader}.
     */
    public void defineContainerClasses() {
        for (Map.Entry<ClassLoader, ClassNode> entry : containers.entrySet()) {
            NativeDefine.defineClassNode(
                    entry.getValue(), // container class
                    entry.getKey()    // original method's ClassLoader
            );
        }
    }

    /**
     * Rewrites member access instructions inside cloned methods.
     * This is done to avoid calling private members,
     * which would break because cloned methods live in a different class than original.
     *
     * @param clonedMethod Method to rewrite
     */
    private void fixMemberAccess(MethodNode clonedMethod) {
        AtomicInteger newLocalStart = new AtomicInteger(ASMUtil.getFirstFreeLocalSlot(clonedMethod));
        int objArrSlot = newLocalStart.getAndAdd(1);

        ASMUtil.loop(clonedMethod.instructions, insnNode -> {
            int opcode = insnNode.getOpcode();
            if (insnNode instanceof FieldInsnNode) {
                FieldInsnNode fieldInsnNode = (FieldInsnNode) insnNode;
                Type fieldType = Type.getType(fieldInsnNode.desc);
                boolean isGet = opcode == GETSTATIC || opcode == GETFIELD;

                String methodName = (isGet ? "get" : "set") + TypeUtil.getTypeName(fieldType) + "Field";
                String methodDesc = "("
                        + "Ljava/lang/Object;" // Object ownerInstance
                        + (!isGet ? fieldType.getDescriptor() : "") // For set?Field: ? value
                        + "Ljava/lang/String;" // String owner
                        + "Ljava/lang/String;" // String name
                        + ")"
                        + (isGet ? fieldType.getDescriptor() : "V");

                InsnList replacement = new InsnList();
                // push null onto stack if access is static. Swap stack if type is PUT because null needs to be below value, that is already on stack
                if (opcode == GETSTATIC || opcode == PUTSTATIC) {
                    replacement.add(new InsnNode(ACONST_NULL));
                    if (opcode == PUTSTATIC) {
                        if (fieldType.getSize() == 2) {
                            replacement.add(new InsnNode(DUP_X2));
                            replacement.add(new InsnNode(POP));
                        } else {
                            replacement.add(new InsnNode(SWAP));
                        }
                    }
                }

                replacement.add(new LdcInsnNode(fieldInsnNode.owner));
                replacement.add(new LdcInsnNode(fieldInsnNode.name));
                replacement.add(new MethodInsnNode(
                        INVOKESTATIC,
                        ASMUtil.getInternalName(MemberAccessor.class),
                        methodName,
                        methodDesc)
                );


                clonedMethod.instructions.insertBefore(insnNode, replacement);
                clonedMethod.instructions.remove(insnNode);
            }

            if (insnNode instanceof MethodInsnNode) {
                MethodInsnNode methodInsnNode = (MethodInsnNode) insnNode;
                if (ASMUtil.isSpecial(methodInsnNode.name)) {
                    return;
                }

                Type methodType = Type.getMethodType(methodInsnNode.desc);
                Type methodReturnType = methodType.getReturnType();
                boolean returnsPrimitive = TypeUtil.isPrimitive(methodReturnType);

                String methodName = "call" + TypeUtil.getTypeName(methodReturnType) + "Method";
                String methodDesc = "("
                        + "Ljava/lang/Object;" // Object ownerInstance
                        + "[Ljava/lang/Object;" // Object[] args
                        + "Ljava/lang/Class;" // Class<?> owner
                        + "Ljava/lang/String;" // String name
                        + "Ljava/lang/String;" // String desc
                        + ")"
                        + (returnsPrimitive ? methodReturnType.getDescriptor() : "Ljava/lang/Object;");

                Type[] argTypes = methodType.getArgumentTypes();
                int[] paramLocals = new int[argTypes.length];
                for (int i = 0; i < argTypes.length; i++) {
                    paramLocals[i] = newLocalStart.getAndAdd(argTypes[i].getSize());
                }

                InsnList replacement = new InsnList();

                // store arguments, that are currently on the stack into locals to avoid hacky stack manipulation
                for (int i = argTypes.length - 1; i >= 0; i--) {
                    replacement.add(new VarInsnNode(argTypes[i].getOpcode(ISTORE), paramLocals[i]));
                }

                // put arguments stored in locals into Object[]
                replacement.add(ASMUtil.getIntPush(argTypes.length));
                replacement.add(new TypeInsnNode(ANEWARRAY, "java/lang/Object"));
                replacement.add(new VarInsnNode(ASTORE, objArrSlot));
                for (int i = 0; i < argTypes.length; i++) {
                    Type arguemtType = methodType.getArgumentTypes()[i];

                    replacement.add(new VarInsnNode(ALOAD, objArrSlot));
                    replacement.add(ASMUtil.getIntPush(i));
                    replacement.add(new VarInsnNode(arguemtType.getOpcode(ILOAD), paramLocals[i]));
                    if (TypeUtil.isPrimitive(arguemtType)) {
                        String primitiveClassName = TypeUtil.getTypeClassInternalName(arguemtType);
                        replacement.add(new MethodInsnNode(
                                INVOKESTATIC,
                                primitiveClassName,
                                "valueOf",
                                "("
                                        + arguemtType.getDescriptor()
                                        + ")L"
                                        + primitiveClassName
                                        + ";"
                        ));
                    }
                    replacement.add(new InsnNode(AASTORE));
                }

                if (opcode == INVOKESTATIC) {
                    replacement.add(new InsnNode(ACONST_NULL));
                }
                // stack: null if static call, instance object if virtual

                // push argument Object[]
                replacement.add(new VarInsnNode(ALOAD, objArrSlot));

                replacement.add(new LdcInsnNode(Type.getObjectType(methodInsnNode.owner)));
                replacement.add(new LdcInsnNode(methodInsnNode.name));
                replacement.add(new LdcInsnNode(methodInsnNode.desc));
                replacement.add(new MethodInsnNode(INVOKESTATIC, ASMUtil.getInternalName(MemberAccessor.class), methodName, methodDesc));
                if (!returnsPrimitive) {
                    replacement.add(new TypeInsnNode(CHECKCAST, methodType.getReturnType().getInternalName()));
                }

                clonedMethod.instructions.insertBefore(insnNode, replacement);
                clonedMethod.instructions.remove(insnNode);
            }
        });
    }

    /**
     * Replaces Jaha.callOriginal* placeholder in the hooked method with calls to the cloned original method.
     *
     * @param container    Class containing the cloned method
     * @param hookedMethod Method being rewritten
     * @param clone        Generated clone that should be called instead
     */
    private void replaceCallsToDummyOriginal(ClassNode container, MethodNode hookedMethod, MethodNode clone, boolean isStatic, int thisSlot) {
        ASMUtil.loop(hookedMethod.instructions, insnNode -> {
            if (!(insnNode instanceof MethodInsnNode)) {
                return;
            }

            MethodInsnNode methodInsnNode = (MethodInsnNode) insnNode;
            if (!methodInsnNode.owner.equals(ASMUtil.getInternalName(Jaha.class)) || !methodInsnNode.name.startsWith("callOriginal")) {
                return;
            }

            if (!isStatic) {
                hookedMethod.instructions.insertBefore(insnNode, new VarInsnNode(Opcodes.ALOAD, thisSlot));
            }
            hookedMethod.instructions.insertBefore(insnNode, new MethodInsnNode(Opcodes.INVOKESTATIC, container.name, clone.name, clone.desc));
            hookedMethod.instructions.remove(insnNode);
        });
    }

    /**
     * Returns the container associated with the given {@link ClassLoader},
     * creating and registering one if it does not exist.
     *
     * @param loader the target class loader that will own/define the container class
     * @return container class bound to {@code loader}
     */
    private ClassNode getOrCreateContainer(ClassLoader loader) {
        return containers.computeIfAbsent(loader, l -> {
            ClassNode containerClass = new ClassNode(ASMUtil.ASM_VERSION);
            containerClass.visit(Opcodes.V1_8, ACC_PUBLIC, UUID.randomUUID().toString(), null, "java/lang/Object", null);
            return containerClass;
        });
    }
}
