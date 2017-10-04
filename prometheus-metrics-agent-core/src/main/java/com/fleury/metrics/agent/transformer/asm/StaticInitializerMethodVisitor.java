package com.fleury.metrics.agent.transformer.asm;

import static com.fleury.metrics.agent.config.Configuration.staticFinalFieldName;
import static com.fleury.metrics.agent.model.LabelUtil.getLabelNames;
import static com.fleury.metrics.agent.transformer.asm.util.CollectionUtil.isNotEmpty;

import com.fleury.metrics.agent.model.Metric;
import com.fleury.metrics.agent.reporter.PrometheusMetricSystem;
import com.fleury.metrics.agent.transformer.asm.util.OpCodeUtil;
import java.util.List;
import org.objectweb.asm.MethodVisitor;
import org.objectweb.asm.Type;
import org.objectweb.asm.commons.AdviceAdapter;

public class StaticInitializerMethodVisitor extends AdviceAdapter {

    private final List<Metric> classMetrics;
    private final String className;

    public StaticInitializerMethodVisitor(MethodVisitor mv, List<Metric> classMetrics, String className, int access, String name, String desc) {
        super(ASM5, mv, access, name, desc);

        this.className = className;
        this.classMetrics = classMetrics;
    }

    @Override
    public void visitCode() {
        super.visitCode();

        for (Metric metric : classMetrics) {
            addMetric(metric);
        }
    }

    private void addMetric(Metric metric) {
        // load name
        super.visitLdcInsn(metric.getName());

        // load labels
        if (isNotEmpty(metric.getLabels())) {
            super.visitInsn(OpCodeUtil.getIConstOpcodeForInteger(metric.getLabels().size()));
            super.visitTypeInsn(ANEWARRAY, Type.getType(String.class).getInternalName());

            List<String> labelNames = getLabelNames(metric.getLabels());
            for (int i = 0; i < labelNames.size(); i++) {
                super.visitInsn(DUP);
                super.visitInsn(OpCodeUtil.getIConstOpcodeForInteger(i));
                super.visitLdcInsn(labelNames.get(i));
                super.visitInsn(AASTORE);
            }
        }
        // or null if non labels
        else {
            super.visitInsn(ACONST_NULL);
        }

        // load doc
        super.visitLdcInsn(metric.getDoc() == null ? "empty doc" : metric.getDoc());

        // call PrometheusMetricSystem.createAndRegisterCounted/Timed/Gauged(...)
        super.visitMethodInsn(INVOKESTATIC, Type.getInternalName(PrometheusMetricSystem.class),
                "createAndRegister" + metric.getType().name(),
                Type.getMethodDescriptor(
                        Type.getType(metric.getType().getPrometheusMetric()),
                        Type.getType(String.class), Type.getType(String[].class), Type.getType(String.class)),
                false);

        // store metric in class static field
        super.visitFieldInsn(PUTSTATIC, className, staticFinalFieldName(metric),
                Type.getDescriptor(metric.getType().getPrometheusMetric()));
    }
}
