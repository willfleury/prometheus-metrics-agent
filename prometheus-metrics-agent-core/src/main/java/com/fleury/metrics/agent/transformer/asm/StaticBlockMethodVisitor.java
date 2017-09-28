package com.fleury.metrics.agent.transformer.asm;

import static com.fleury.metrics.agent.config.Configuration.metricStaticFieldName;
import static com.fleury.metrics.agent.model.LabelUtil.getLabelNames;
import static com.fleury.metrics.agent.transformer.asm.util.CollectionUtil.isNotEmpty;

import com.fleury.metrics.agent.model.Metric;
import com.fleury.metrics.agent.reporter.PrometheusMetricSystem;
import com.fleury.metrics.agent.transformer.asm.util.OpCodeUtil;
import io.prometheus.client.SimpleCollector;
import java.util.List;
import org.objectweb.asm.MethodVisitor;
import org.objectweb.asm.Type;
import org.objectweb.asm.commons.AdviceAdapter;

public class StaticBlockMethodVisitor extends AdviceAdapter {

    private final List<Metric> classMetrics;
    private final String className;

    public StaticBlockMethodVisitor(MethodVisitor mv, List<Metric> classMetrics, String className, int access, String name, String desc) {
        super(ASM5, mv, access, name, desc);

        this.className = className;
        this.classMetrics = classMetrics;
    }

    @Override
    public void visitCode() {
        super.visitCode();

        for (Metric metric : classMetrics) {
            addMetricV2(metric);
        }
    }

    private void addMetricV2(Metric metric) {
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
                Type.getMethodDescriptor(Type.getType(metric.getType().getPrometheusMetric()),
                        Type.getType(String.class), Type.getType(String[].class), Type.getType(String.class)),
                false);

        // store metric in class static field
        super.visitFieldInsn(PUTSTATIC, className, metricStaticFieldName(metric),
                Type.getDescriptor(metric.getType().getPrometheusMetric()));
    }


    private void addMetric(Metric metric, Class metricClazz) {
        String baseClass = Type.getInternalName(metricClazz);
        String builderClass = baseClass + "$Builder";

        super.visitMethodInsn(INVOKESTATIC, baseClass, "build", "()" + asDesc(builderClass), false);
        super.visitLdcInsn(metric.getName());
        super.visitMethodInsn(INVOKEVIRTUAL, builderClass, "name",
                asArg(Type.getDescriptor(String.class)) + Type.getDescriptor(SimpleCollector.Builder.class), false);
        super.visitTypeInsn(CHECKCAST, builderClass);


        super.visitLdcInsn(metric.getDoc() == null ? "doc" : metric.getDoc());
        super.visitMethodInsn(INVOKEVIRTUAL, builderClass, "help",
                asArg(Type.getDescriptor(String.class)) + Type.getDescriptor(SimpleCollector.Builder.class), false);
        super.visitTypeInsn(CHECKCAST, builderClass);

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

            super.visitMethodInsn(INVOKEVIRTUAL, builderClass, "labelNames",
                    asArg(Type.getDescriptor(String[].class)) + Type.getDescriptor(SimpleCollector.Builder.class), false);
            super.visitTypeInsn(CHECKCAST, builderClass);
        }

        super.visitMethodInsn(INVOKEVIRTUAL, builderClass, "register", "()" + Type.getDescriptor(SimpleCollector.class), false);
        super.visitTypeInsn(CHECKCAST, baseClass);
        super.visitFieldInsn(PUTSTATIC, className, metricStaticFieldName(metric), asDesc(baseClass));
    }

    private String asDesc(String className) {
        return "L" + className + ";";
    }

    private String asArg(String... args) {
        String str = "(";

        for (String arg : args) {
            str += arg;
        }

        return str + ")";
    }
}
