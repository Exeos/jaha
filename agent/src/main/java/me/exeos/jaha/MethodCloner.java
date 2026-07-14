package me.exeos.jaha;

import me.exeos.jaha.runtime.MemberAccessor;
import me.exeos.jaha.util.ASMUtil;
import me.exeos.jaha.util.TypeUtil;
import org.objectweb.asm.Opcodes;
import org.objectweb.asm.Type;
import org.objectweb.asm.tree.*;
import org.objectweb.asm.util.Textifier;
import org.objectweb.asm.util.TraceMethodVisitor;

import java.io.PrintWriter;
import java.io.StringWriter;
import java.util.HashMap;
import java.util.Map;
import java.util.UUID;

public class MethodCloner implements Opcodes {

    private final Map<ClassLoader, ClassNode> containers = new HashMap<>();
    private final String memberAccessorName = MemberAccessor.class.getName().replace(".", "/");
    private int counter = 0;

    public ClassNode getOrCreateContainer(ClassLoader loader) {
        return containers.computeIfAbsent(loader, l -> {
            ClassNode cn = new ClassNode(Opcodes.ASM9);
            cn.visit(Opcodes.V1_8, ACC_PUBLIC, UUID.randomUUID().toString(), null, "java/lang/Object", null);
            return cn;
        });
    }

    public MethodNode cloneMethod(ClassLoader ownerLoader, String methodOwner, MethodNode methodNode) {
        ClassNode container = getOrCreateContainer(ownerLoader);

        MethodNode clone = new MethodNode(
                ACC_PUBLIC | ACC_STATIC,
                String.valueOf(counter++),
                methodNode.desc.replace(")", "L" + methodOwner + ";)"),
                null,
                methodNode.exceptions.toArray(new String[0])
        );

        clone.instructions = ASMUtil.clone(methodNode.instructions);

        if (!ASMUtil.hasAccess(methodNode.access, ACC_STATIC)) {
            ASMUtil.remapLocals(clone.instructions, ASMUtil.getArgumentsSize(clone.desc));
        }
        fixMemberAccess(clone);

        // debug
        Textifier textifier = new Textifier();
        TraceMethodVisitor tmv = new TraceMethodVisitor(textifier);
        clone.accept(tmv);

        StringWriter out = new StringWriter();
        textifier.print(new PrintWriter(out));
        System.out.println(methodOwner + "." + methodNode.name);
        System.out.println(out);
        // end debug

        container.methods.add(clone);

        return clone;
    }

    private void fixMemberAccess(MethodNode methodNode) {
        ASMUtil.loop(methodNode.instructions, insnNode -> {
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
                        memberAccessorName,
                        methodName,
                        methodDesc)
                );


                methodNode.instructions.insertBefore(insnNode, replacement);
                methodNode.instructions.remove(insnNode);
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
                methodNode.maxLocals++;
                for (int i = 0; i < argTypes.length; i++) {
                    tmpLocal[i] = methodNode.maxLocals;
                    methodNode.maxLocals += argTypes[i].getSize();
                }

                InsnList replacement = new InsnList();

                for (int i = argTypes.length - 1; i >= 0; i--) {
                    replacement.add(new VarInsnNode(argTypes[i].getOpcode(ISTORE), tmpLocal[i]));
                }

                int objArrSlot = methodNode.maxLocals++;
                replacement.add(ASMUtil.getIntPush(argTypes.length));
                replacement.add(new TypeInsnNode(ANEWARRAY, "java/lang/Object"));
                replacement.add(new VarInsnNode(ASTORE, objArrSlot));

                for (int i = 0; i < argTypes.length; i++) {
                    Type arguemtType = methodType.getArgumentTypes()[i];

                    replacement.add(new VarInsnNode(ALOAD, objArrSlot));
                    replacement.add(ASMUtil.getIntPush(i));
                    replacement.add(new VarInsnNode(arguemtType.getOpcode(ILOAD), tmpLocal[i]));
                    if (TypeUtil.isPrimitive(arguemtType)) {
                        String primitiveClassName = TypeUtil.getPrimitiveClassName(arguemtType);
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

//                replacement.add(new LdcInsnNode(methodInsnNode.owner));
                replacement.add(new LdcInsnNode(Type.getObjectType(methodInsnNode.owner)));
                replacement.add(new LdcInsnNode(methodInsnNode.name));
                replacement.add(new LdcInsnNode(methodInsnNode.desc));
                replacement.add(new MethodInsnNode(INVOKESTATIC, memberAccessorName, methodName, methodDesc));
                if (!isPrimitiveReturn) {
                    replacement.add(new TypeInsnNode(CHECKCAST, methodType.getReturnType().getInternalName()));
                }

                methodNode.instructions.insertBefore(insnNode, replacement);
                methodNode.instructions.remove(insnNode);
            }
        });
    }

    public Map<ClassLoader, ClassNode> getContainers() {
        return containers;
    }
}
