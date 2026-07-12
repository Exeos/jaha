package me.exeos.jaha.util;

import org.objectweb.asm.tree.*;

import java.util.List;

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
}
