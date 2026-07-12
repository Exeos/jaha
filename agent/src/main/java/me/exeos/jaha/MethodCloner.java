package me.exeos.jaha;

import me.exeos.jaha.util.ASMUtil;
import org.objectweb.asm.Opcodes;
import org.objectweb.asm.tree.ClassNode;
import org.objectweb.asm.tree.MethodNode;
import org.objectweb.asm.util.Textifier;
import org.objectweb.asm.util.TraceMethodVisitor;

import java.io.PrintWriter;
import java.io.StringWriter;
import java.util.UUID;

public class MethodCloner implements Opcodes {

    private final ClassNode cloneContainer;
    private int counter = 0;

    public MethodCloner() {
        cloneContainer = new ClassNode(Opcodes.ASM9);
        cloneContainer.visit(Opcodes.V1_8, ACC_PUBLIC, UUID.randomUUID().toString(), null, "java/lang/Object", null);
    }

    public MethodNode cloneMethod(String methodOwner, MethodNode methodNode) {
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

        Textifier textifier = new Textifier();
        TraceMethodVisitor tmv = new TraceMethodVisitor(textifier);
        clone.accept(tmv);

        StringWriter out = new StringWriter();
        textifier.print(new PrintWriter(out));
        System.out.println(out);

        cloneContainer.methods.add(clone);

        return clone;
    }

    public ClassNode getCloneContainer() {
        return cloneContainer;
    }
}
