package me.exeos.jaha;

import me.exeos.jaha.runtime.MemberAccessor;
import me.exeos.jaha.util.ASMUtil;
import me.exeos.jaha.util.NativeDefine;
import me.exeos.jaha.util.TypeUtil;
import org.objectweb.asm.Opcodes;
import org.objectweb.asm.Type;
import org.objectweb.asm.tree.*;

import java.util.HashMap;
import java.util.Map;
import java.util.UUID;

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
                ASMUtil.hasAccess(originalMethod.access, ACC_STATIC)
                        ? originalMethod.desc
                        : originalMethod.desc.replace(")", "L" + methodOwner + ";)"),
                null,
                originalMethod.exceptions.toArray(new String[0])
        );
        clone.instructions = ASMUtil.clone(originalMethod.instructions);

        if (!isStatic) {
            // locals need to be remapped, because we added a parameter
            ASMUtil.remapLocals(clone.instructions, ASMUtil.getArgumentsSize(clone.desc));
        }
        fixMemberAccess(clone);
        replaceCallsToDummyOriginal(container, hookedMethod, clone, isStatic);

        container.methods.add(clone);
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
        ASMUtil.loop(clonedMethod.instructions, insnNode -> {
            int opcode = insnNode.getOpcode();
            if (insnNode instanceof FieldInsnNode) {
                FieldInsnNode fieldInsnNode = (FieldInsnNode) insnNode;
                Type fieldType = Type.getType(fieldInsnNode.desc);
                boolean isGet = opcode == GETSTATIC || opcode == GETFIELD;

                InsnList replacement = new InsnList();
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

                String methodName;

                switch (fieldType.getSort()) {
                    case Type.BYTE:
                        methodName = isGet ? "getByteField" : "setByteField";
                        break;
                    case Type.SHORT:
                        methodName = isGet ? "getShortField" : "setShortField";
                        break;
                    case Type.INT:
                        methodName = isGet ? "getIntField" : "setIntField";
                        break;
                    case Type.LONG:
                        methodName = isGet ? "getLongField" : "setLongField";
                        break;
                    case Type.FLOAT:
                        methodName = isGet ? "getFloatField" : "setFloatField";
                        break;
                    case Type.DOUBLE:
                        methodName = isGet ? "getDoubleField" : "setDoubleField";
                        break;
                    case Type.BOOLEAN:
                        methodName = isGet ? "getBooleanField" : "setBooleanField";
                        break;
                    case Type.CHAR:
                        methodName = isGet ? "getCharField" : "setCharField";
                        break;
                    case Type.OBJECT:
                        methodName = isGet ? "getObjectField" : "setObjectField";
                        break;
                    default:
                        throw new IllegalStateException("Failed to convert member access to runtime wrapper. Invalid fieldType: " + fieldType.getSort());
                }

                String methodDesc = "("
                        + "Ljava/lang/Object;"
                        + (!isGet ? fieldType.getDescriptor() : "")
                        + "Ljava/lang/String;Ljava/lang/String;"
                        + ")"
                        + (isGet ? fieldType.getDescriptor() : "V");


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
                Type methodType = Type.getMethodType(methodInsnNode.desc);
                boolean isPrimitiveReturn = TypeUtil.isPrimitive(methodType.getReturnType());
                String methodName;
                String methodDesc = "("
                        + "Ljava/lang/Object;" // Object ownerInstance
                        + "[Ljava/lang/Object;" // Object[] args
                        + "Ljava/lang/Class;" // Class<?> owner
                        + "Ljava/lang/String;" // String name
                        + "Ljava/lang/String;" // String desc
                        + ")"
                        + (isPrimitiveReturn ? methodType.getReturnType().getDescriptor() : "Ljava/lang/Object;");
                switch (methodType.getReturnType().getSort()) {
                    case Type.VOID:
                        methodName = "callVoidMethod";
                        break;
                    case Type.BOOLEAN:
                        methodName = "callBooleanMethod";
                        break;
                    case Type.CHAR:
                        methodName = "callCharMethod";
                        break;
                    case Type.BYTE:
                        methodName = "callByteMethod";
                        break;
                    case Type.SHORT:
                        methodName = "callShortMethod";
                        break;
                    case Type.INT:
                        methodName = "callIntMethod";
                        break;
                    case Type.FLOAT:
                        methodName = "callFloatMethod";
                        break;
                    case Type.LONG:
                        methodName = "callLongMethod";
                        break;
                    case Type.DOUBLE:
                        methodName = "callDoubleMethod";
                        break;
                    case Type.ARRAY:
                    case Type.OBJECT:
                        methodName = "callObjectMethod";
                        break;
                    default:
                        throw new IllegalStateException("Failed to convert member access to runtime wrapper. Invalid methodType return sort: " + methodType.getReturnType().getSort());
                }

                Type[] argTypes = methodType.getArgumentTypes();
                int[] tmpLocal = new int[argTypes.length];
                clonedMethod.maxLocals++;
                for (int i = 0; i < argTypes.length; i++) {
                    tmpLocal[i] = clonedMethod.maxLocals;
                    clonedMethod.maxLocals += argTypes[i].getSize();
                }

                InsnList replacement = new InsnList();

                for (int i = argTypes.length - 1; i >= 0; i--) {
                    replacement.add(new VarInsnNode(argTypes[i].getOpcode(ISTORE), tmpLocal[i]));
                }

                int objArrSlot = clonedMethod.maxLocals++;
                replacement.add(ASMUtil.getIntPush(argTypes.length));
                replacement.add(new TypeInsnNode(ANEWARRAY, "java/lang/Object"));
                replacement.add(new VarInsnNode(ASTORE, objArrSlot));

                for (int i = 0; i < argTypes.length; i++) {
                    Type arguemtType = methodType.getArgumentTypes()[i];

                    replacement.add(new VarInsnNode(ALOAD, objArrSlot));
                    replacement.add(ASMUtil.getIntPush(i));
                    replacement.add(new VarInsnNode(arguemtType.getOpcode(ILOAD), tmpLocal[i]));
                    if (TypeUtil.isPrimitive(arguemtType)) {
                        String primitiveClassName = TypeUtil.getPrimitiveClassInternalName(arguemtType);
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
                replacement.add(new VarInsnNode(ALOAD, objArrSlot));

                replacement.add(new LdcInsnNode(Type.getObjectType(methodInsnNode.owner)));
                replacement.add(new LdcInsnNode(methodInsnNode.name));
                replacement.add(new LdcInsnNode(methodInsnNode.desc));
                replacement.add(new MethodInsnNode(INVOKESTATIC, ASMUtil.getInternalName(MemberAccessor.class), methodName, methodDesc));
                if (!isPrimitiveReturn) {
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
    private void replaceCallsToDummyOriginal(ClassNode container, MethodNode hookedMethod, MethodNode clone, boolean isStatic) {
        ASMUtil.loop(hookedMethod.instructions, insnNode -> {
            if (!(insnNode instanceof MethodInsnNode)) {
                return;
            }

            MethodInsnNode methodInsnNode = (MethodInsnNode) insnNode;
            if (!methodInsnNode.owner.equals(ASMUtil.getInternalName(Jaha.class)) || !methodInsnNode.name.startsWith("callOriginal")) {
                return;
            }

            if (!isStatic) {
                hookedMethod.instructions.insertBefore(insnNode, new VarInsnNode(Opcodes.ALOAD, 0));
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
            ClassNode containerClass = new ClassNode(Opcodes.ASM9);
            containerClass.visit(Opcodes.V1_8, ACC_PUBLIC, UUID.randomUUID().toString(), null, "java/lang/Object", null);
            return containerClass;
        });
    }
}
