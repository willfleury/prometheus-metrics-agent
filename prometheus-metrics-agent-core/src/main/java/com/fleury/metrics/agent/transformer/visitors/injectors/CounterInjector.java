package com.fleury.metrics.agent.transformer.visitors.injectors;

import static com.fleury.metrics.agent.config.Configuration.staticFinalFieldName;
import static com.fleury.metrics.agent.model.MetricType.Counted;

import com.fleury.metrics.agent.model.Metric;
import org.objectweb.asm.Type;
import org.objectweb.asm.commons.AdviceAdapter;

/**
 * Transforms from
 *
 * <pre>
 * public void someMethod() {
 *     //original method code
 * }
 * </pre>
 *
 * To
 *
 * <pre>
 * public void someMethod() {
 *     PrometheusMetricSystem.recordCount(COUNTER, labels);
 *
 *     //original method code
 * }
 * </pre>
 *
 * @author Will Fleury
 */
public class CounterInjector extends AbstractInjector {

    private static final String METHOD = "recordCount";
    private static final String SIGNATURE = Type.getMethodDescriptor(
            Type.VOID_TYPE,
            Type.getType(Counted.getCoreType()), Type.getType(String[].class));
    
    private final Metric metric;

    public CounterInjector(Metric metric, AdviceAdapter aa, String className, Type[] argTypes, int access) {
        super(aa, className, argTypes, access);
        this.metric = metric;
    }

    @Override
    public void injectAtMethodEnter() {
        aa.visitFieldInsn(GETSTATIC, className, staticFinalFieldName(metric), Type.getDescriptor(Counted.getCoreType()));
        injectLabelsToStack(metric);

        aa.visitMethodInsn(INVOKESTATIC, METRIC_REPORTER_CLASSNAME, METHOD, SIGNATURE, false);
    }

}
