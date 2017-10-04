package com.fleury.metrics.agent.transformer.visitors;

import static org.objectweb.asm.Opcodes.ACC_INTERFACE;
import static org.objectweb.asm.Opcodes.ACC_SYNTHETIC;
import static org.objectweb.asm.Opcodes.ASM5;

import com.fleury.metrics.agent.config.Configuration;
import org.objectweb.asm.ClassVisitor;
import org.objectweb.asm.MethodVisitor;


/**
 *
 * @author Will Fleury
 */
public class AnnotationClassVisitor extends ClassVisitor {

    private boolean isInterface;
    private String className;
    private Configuration config;

    public AnnotationClassVisitor(ClassVisitor cv, Configuration config) {
        super(ASM5, cv);
        this.config = config;
    }

    @Override
    public void visit(int version, int access, String name, String signature, String superName, String[] interfaces) {
        super.visit(version, access, name, signature, superName, interfaces);
        this.className = name;
        this.isInterface = (access & ACC_INTERFACE) != 0;
    }

    @Override
    public MethodVisitor visitMethod(int access, String name, String desc, String signature, String[] exceptions) {
        MethodVisitor mv = super.visitMethod(access, name, desc, signature, exceptions);

        boolean isSyntheticMethod = (access & ACC_SYNTHETIC) != 0;

        if (!isInterface && !isSyntheticMethod && mv != null &&
                config.isWhiteListed(className) && !config.isBlackListed(className)) {
            mv = new AnnotationMethodVisitor(mv, config, className, name, desc);
        }

        return mv;
    }
}
