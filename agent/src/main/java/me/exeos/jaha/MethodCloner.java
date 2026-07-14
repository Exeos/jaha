package me.exeos.jaha;

import me.exeos.jaha.runtime.MemberAccessor;
import me.exeos.jaha.util.ASMUtil;
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
        fixMemberAccess(clone.instructions);

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

    private void fixMemberAccess(InsnList insns) {
        ASMUtil.loop(insns, insnNode -> {
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


                insns.insertBefore(insnNode, replacement);
                insns.remove(insnNode);
            }

            if (insnNode instanceof MethodInsnNode) {
                MethodInsnNode methodInsnNode = (MethodInsnNode) insnNode;
            }
        });
    }

    public Map<ClassLoader, ClassNode> getContainers() {
        return containers;
    }
}
