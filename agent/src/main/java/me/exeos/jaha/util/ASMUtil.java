package me.exeos.jaha.util;

import org.objectweb.asm.ClassReader;
import org.objectweb.asm.ClassWriter;
import org.objectweb.asm.Opcodes;
import org.objectweb.asm.Type;
import org.objectweb.asm.tree.*;

import java.io.IOException;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.function.Consumer;

public class ASMUtil implements Opcodes {

    public static final int ASM_VERSION = Opcodes.ASM9;

    /**
     * Check if methodNode has visible or invisible annotation
     *
     * @param methodNode     MethodNode to check
     * @param annotationName Internal name of the annotation
     * @return true if it has, else false
     */
    public static boolean hasAnnotation(MethodNode methodNode, String annotationName) {
        String annotationDesc = "L" + annotationName + ";";
        return hasAnnotation(methodNode.visibleAnnotations, annotationDesc) || hasAnnotation(methodNode.invisibleAnnotations, annotationDesc);
    }

    public static boolean hasAnnotation(List<AnnotationNode> annotations, String descriptor) {
        if (annotations == null) return false;
        for (AnnotationNode an : annotations) {
            if (an.desc.equals(descriptor)) {
                return true;
            }
        }
        return false;
    }

    /**
     * Safely loop trough InsnList, without causing brakeage when modifying InsnList
     *
     * @param insnList InsnList to loop trough
     * @param visitor  Consumer to be consumed on each instruction
     */
    public static void loop(InsnList insnList, Consumer<AbstractInsnNode> visitor) {
        AbstractInsnNode current = insnList.getFirst();

        while (current != null) {
            AbstractInsnNode next = current.getNext();
            visitor.accept(current);
            current = next;
        }
    }

    /**
     * Checks if access code bitmask contains toCheck
     *
     * @param access  Access code bitmask
     * @param toCheck Access code to check
     * @return true if access has toCheck, else false
     */
    public static boolean hasAccess(int access, int toCheck) {
        return (access & toCheck) != 0;
    }

    public static void cloneMethodInsn(MethodNode source, MethodNode target) {
        Map<LabelNode, LabelNode> labelMap = labelMapForClone(source.instructions);
        target.instructions = clone(source.instructions, labelMap);
        for (TryCatchBlockNode tryCatchBlock : source.tryCatchBlocks) {
            target.tryCatchBlocks.add(new TryCatchBlockNode(
                    labelMap.get(tryCatchBlock.start),
                    labelMap.get(tryCatchBlock.end),
                    labelMap.get(tryCatchBlock.handler),
                    tryCatchBlock.type
            ));
        }
    }

    public static Map<LabelNode, LabelNode> labelMapForClone(InsnList source) {
        Map<LabelNode, LabelNode> labelMap = new HashMap<>();
        for (AbstractInsnNode insn : source.toArray()) {
            if (insn instanceof LabelNode) {
                labelMap.put((LabelNode) insn, new LabelNode());
            }
        }

        return labelMap;
    }

    /**
     * Clone InsnList, so you keep the instructions without causing breakage to related Objects
     *
     * @param source InsnList to clone
     * @return Cloned InsnList
     */
    public static InsnList clone(InsnList source) {
        return clone(source, labelMapForClone(source));
    }

    /**
     * Clone InsnList, so you keep the instructions without causing breakage to related Objects
     *
     * @param source InsnList to clone
     * @return Cloned InsnList
     */
    public static InsnList clone(InsnList source, Map<LabelNode, LabelNode> labelMap) {
        InsnList copy = new InsnList();
        for (AbstractInsnNode insn : source.toArray()) {
            copy.add(insn.clone(labelMap));
        }

        return copy;
    }

    public static int getArgumentsSize(String methodDescriptor) {
        int size = 0;
        Type[] types = Type.getArgumentTypes(methodDescriptor);
        for (Type type : types) {
            size += type.getSize();
        }

        return size;
    }

    public static int getLastArgumentSlot(String methodDescriptor) {
        int slot = 0;
        Type[] types = Type.getArgumentTypes(methodDescriptor);

        for (int i = 0; i < types.length - 1; i++) {
            slot += types[i].getSize();
        }

        return slot;
    }

    public static int getArgumentSlot(Type[] args, int argIndex) {
        int slot = 0;

        for (int i = 0; i < args.length; i++) {
            if (i == argIndex)
                break;

            slot += args[i].getSize();
        }

        return slot;
    }

    /**
     * Remaps indexes of locals so they don't collide with newly added params
     *
     * @param container Instructions to remap
     * @param threshold The index threshold marking the end of the method params
     */
    public static void remapLocals(InsnList container, int threshold) {
        loop(container, insnNode -> {
            if (insnNode instanceof VarInsnNode) {
                VarInsnNode varInsnNode = (VarInsnNode) insnNode;
                if (varInsnNode.var >= threshold) {
                    varInsnNode.var++;
                }
            }

            if (insnNode instanceof IincInsnNode) {
                IincInsnNode iincInsnNode = (IincInsnNode) insnNode;
                if (iincInsnNode.var >= threshold) {
                    iincInsnNode.var++;
                }
            }
        });
    }

    public static AbstractInsnNode getIntPush(int value) {
        if (value >= Short.MIN_VALUE && value <= Short.MAX_VALUE)
            return getShortPush((short) value);

        return new LdcInsnNode(value);
    }

    public static AbstractInsnNode getShortPush(short value) {
        if (isIConstPush(ICONST_0 + value)) {
            return getIConstPush(value);
        }

        if (value >= Byte.MIN_VALUE && value <= Byte.MAX_VALUE) {
            return getBytePush((byte) value);
        }

        return new IntInsnNode(SIPUSH, value);
    }

    public static AbstractInsnNode getBytePush(byte value) {
        if (isIConstPush(ICONST_0 + value)) {
            return getIConstPush(value);
        }

        return new IntInsnNode(BIPUSH, value);
    }

    public static AbstractInsnNode getIConstPush(int value) {
        if (value < -1 || value > 5)
            throw new IllegalStateException("Value: " + value + " isn't in required bound: -1 to +5");

        return new InsnNode(ICONST_0 + value);
    }

    public static boolean isIConstPush(int opcode) {
        return opcode >= ICONST_M1 && opcode <= ICONST_5;
    }

    public static ClassNode cnFromBytes(byte[] data) {
        ClassNode classNode = new ClassNode(ASM_VERSION);
        ClassReader cr = new ClassReader(data);
        cr.accept(classNode, 0);

        return classNode;
    }

    public static ClassNode cnFromClass(Class<?> clazz) {
        ClassNode classNode = new ClassNode(ASM_VERSION);
        try {
            ClassReader cr = new ClassReader(clazz.getName());
            cr.accept(classNode, ClassReader.SKIP_DEBUG);
        } catch (IOException e) {
            throw new RuntimeException("Failed to parse ClassNode from Class: " + clazz.getName(), e);
        }

        return classNode;
    }

    public static byte[] getCNBytes(ClassNode classNode) {
        ClassWriter cw = new ClassWriter(ClassWriter.COMPUTE_MAXS | ClassWriter.COMPUTE_FRAMES);
        classNode.accept(cw);

        return cw.toByteArray();
    }

    public static byte[] getClassBytes(Class<?> clazz) {
        ClassReader cr;
        try {
            cr = new ClassReader(clazz.getName());
        } catch (IOException e) {
            throw new RuntimeException("Failed to read class: " + clazz.getName(), e);
        }

        return cr.b;
    }

    public static String getInternalName(Class<?> clazz) {
        return clazz.getName().replace(".", "/");
    }

    public static boolean isSpecial(String methodName) {
        return methodName.equals("<init>") || methodName.equals("<clinit>");
    }

    public static int getFirstFreeLocalSlot(MethodNode methodNode) {
        int max = getArgumentsSize(methodNode.desc);
        for (AbstractInsnNode insnNode : methodNode.instructions) {
            if (insnNode instanceof VarInsnNode) {
                VarInsnNode varInsn = (VarInsnNode) insnNode;
                int size = isWide(varInsn.getOpcode()) ? 2 : 1;
                max = Math.max(max, varInsn.var + size);
            }

            if (insnNode instanceof IincInsnNode) {
                max = Math.max(max, ((IincInsnNode) insnNode).var + 1);
            }
        }

        return max;
    }

    public static boolean isWide(int opcode) {
        switch (opcode) {
            case Opcodes.LLOAD:
            case Opcodes.LSTORE:
            case Opcodes.DLOAD:
            case Opcodes.DSTORE:
                return true;
            default:
                return false;
        }
    }
}
