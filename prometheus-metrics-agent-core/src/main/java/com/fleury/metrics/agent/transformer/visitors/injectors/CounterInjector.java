package com.fleury.metrics.agent.transformer.visitors.injectors;

import static com.fleury.metrics.agent.config.Configuration.staticFinalFieldName;

import com.fleury.metrics.agent.model.Metric;
import io.prometheus.client.Counter;
import org.objectweb.asm.Type;
import org.objectweb.asm.commons.AdviceAdapter;

/**
 *
 * @author Will Fleury
 */
public class CounterInjector extends AbstractInjector {

    private static final String METHOD = "recordCount";
    private static final String SIGNATURE = Type.getMethodDescriptor(
            Type.VOID_TYPE,
            Type.getType(Counter.class), Type.getType(String.class), Type.getType(String[].class));
    
    private final Metric metric;

    public CounterInjector(Metric metric, AdviceAdapter aa, String className, Type[] argTypes, int access) {
        super(aa, className, argTypes, access);
        this.metric = metric;
    }

    @Override
    public void injectAtMethodEnter() {
        aa.visitFieldInsn(GETSTATIC, className, staticFinalFieldName(metric), Type.getDescriptor(Counter.class));
        injectNameAndLabelToStack(metric);

        aa.visitMethodInsn(INVOKESTATIC, METRIC_REPORTER_CLASSNAME, METHOD, SIGNATURE, false);
    }

}
