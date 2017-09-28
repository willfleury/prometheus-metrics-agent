package com.fleury.metrics.agent.transformer.asm;

import static com.fleury.metrics.agent.config.Configuration.staticFinalFieldName;
import static org.objectweb.asm.Opcodes.ACC_FINAL;
import static org.objectweb.asm.Opcodes.ACC_INTERFACE;
import static org.objectweb.asm.Opcodes.ACC_PUBLIC;
import static org.objectweb.asm.Opcodes.ACC_STATIC;
import static org.objectweb.asm.Opcodes.ACC_SYNTHETIC;
import static org.objectweb.asm.Opcodes.ASM5;
import static org.objectweb.asm.Opcodes.RETURN;

import com.fleury.metrics.agent.config.Configuration;
import com.fleury.metrics.agent.model.Metric;
import java.util.List;
import org.objectweb.asm.ClassVisitor;
import org.objectweb.asm.MethodVisitor;
import org.objectweb.asm.Type;
import org.objectweb.asm.commons.JSRInlinerAdapter;

/**
 *
 * @author Will Fleury
 */
public class MetricClassVisitor extends ClassVisitor {

    private boolean isInterface;
    private String className;
    private int classVersion;
    private boolean visitedStaticBlock = false;
    private Configuration config;
    private List<Metric> classMetrics;

    public MetricClassVisitor(ClassVisitor cv, Configuration config) {
        super(ASM5, cv);
        this.config = config;
    }

    @Override
    public void visit(int version, int access, String name, String signature, String superName, String[] interfaces) {
        super.visit(version, access, name, signature, superName, interfaces);
        this.classVersion = version;
        this.className = name;
        this.isInterface = (access & ACC_INTERFACE) != 0;

        this.classMetrics = config.findMetrics(className);

        for (Metric metric : classMetrics) {
            super.visitField(
                    ACC_PUBLIC + ACC_FINAL + ACC_STATIC,
                    staticFinalFieldName(metric),
                    Type.getDescriptor(metric.getType().getPrometheusMetric()), null, null).visitEnd();
        }
    }

    @Override
    public MethodVisitor visitMethod(int access, String name, String desc, String signature, String[] exceptions) {
        MethodVisitor mv = super.visitMethod(access, name, desc, signature, exceptions);

        boolean isSyntheticMethod = (access & ACC_SYNTHETIC) != 0;
        boolean isStaticMethod = (access & ACC_STATIC) != 0;

        if (!isInterface && !isSyntheticMethod && mv != null) {
            List<Metric> metadata = config.findMetrics(className, name + desc);

            mv = new MetricAdapter(mv, className, access, name, desc, metadata);
            mv = new JSRInlinerAdapter(mv, access, name, desc, signature, exceptions);
        }

        if (name.equals("<clinit>") && isStaticMethod && mv != null) {
            visitedStaticBlock = true;

            mv = new StaticBlockMethodVisitor(mv, classMetrics, className, access, name, desc);
        }

        return mv;
    }

    @Override
    public void visitEnd() {
        if (!visitedStaticBlock) {
            MethodVisitor mv = super.visitMethod(ACC_STATIC, "<clinit>", "()V", null, null);
            mv = new StaticBlockMethodVisitor(mv, classMetrics, className, ACC_STATIC, "<clinit>", "()V");

            mv.visitCode();
            mv.visitInsn(RETURN);
            mv.visitMaxs(0, 0);
            mv.visitEnd();
        }

        super.visitEnd();
    }
}
