package me.exeos.jaha.util;

import org.objectweb.asm.Type;
import org.objectweb.asm.tree.*;

import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.function.Consumer;

public class ASMUtil {

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

    public static void loop(InsnList insnList, Consumer<AbstractInsnNode> visitor) {
        AbstractInsnNode current = insnList.getFirst();

        while (current != null) {
            AbstractInsnNode next = current.getNext();
            visitor.accept(current);
            current = next;
        }
    }

    public static boolean hasAccess(int access, int toCheck) {
        return (access & toCheck) != 0;
    }

    public static InsnList clone(InsnList source) {
        Map<LabelNode, LabelNode> labelMap = new HashMap<>();
        for (AbstractInsnNode insn : source.toArray()) {
            if (insn instanceof LabelNode) {
                labelMap.put((LabelNode) insn, new LabelNode());
            }
        }

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
}
