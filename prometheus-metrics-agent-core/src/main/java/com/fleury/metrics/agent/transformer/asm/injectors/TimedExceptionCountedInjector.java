package com.fleury.metrics.agent.transformer.asm.injectors;

import static com.fleury.metrics.agent.config.Configuration.metricStaticFieldName;

import com.fleury.metrics.agent.model.Metric;
import io.prometheus.client.Counter;
import io.prometheus.client.Histogram;
import org.objectweb.asm.Label;
import org.objectweb.asm.Type;
import org.objectweb.asm.commons.AdviceAdapter;

/**
 *
 * @author Will Fleury
 */
public class TimedExceptionCountedInjector extends AbstractInjector {
    
    private static final String EXCEPTION_COUNT_METHOD = "recordCount";
    private static final String EXCEPTION_COUNT_SIGNATURE = Type.getMethodDescriptor(
            Type.VOID_TYPE,
            Type.getType(Counter.class), Type.getType(String.class), Type.getType(String[].class));
    
    private static final String TIMER_METHOD = "recordTime";
    private static final String TIMER_SIGNATURE = Type.getMethodDescriptor(
            Type.VOID_TYPE,
            Type.getType(Histogram.class), Type.getType(String.class), Type.getType(String[].class), Type.LONG_TYPE);
    
    private final Metric timerMetric;
    private final Metric exceptionMetric;
    
    private int startTimeVar;
    private Label startFinally;
    
    public TimedExceptionCountedInjector(Metric timerMetric, Metric exceptionMetric, AdviceAdapter aa,
                                         String className, Type[] argTypes, int access) {
        super(aa, className, argTypes, access);
        this.timerMetric = timerMetric;
        this.exceptionMetric = exceptionMetric;
    }

    @Override
    public void injectAtMethodEnter() {
        startFinally = new Label();
        startTimeVar = aa.newLocal(Type.LONG_TYPE);
        aa.visitMethodInsn(INVOKESTATIC, "java/lang/System", "nanoTime", "()J", false);
        aa.visitVarInsn(LSTORE, startTimeVar);
        aa.visitLabel(startFinally);
    }

    @Override
    public void injectAtVisitMaxs(int maxStack, int maxLocals) {
        Label endFinally = new Label();
        aa.visitTryCatchBlock(startFinally, endFinally, endFinally, null);
        aa.visitLabel(endFinally);

        aa.visitFieldInsn(GETSTATIC, className, metricStaticFieldName(exceptionMetric), Type.getDescriptor(Counter.class));
        injectNameAndLabelToStack(exceptionMetric);
        aa.visitMethodInsn(INVOKESTATIC, METRIC_REPORTER_CLASSNAME, EXCEPTION_COUNT_METHOD, 
                EXCEPTION_COUNT_SIGNATURE, false);
        
        onFinally(ATHROW);
        aa.visitInsn(ATHROW);
    }

    @Override
    public void injectAtMethodExit(int opcode) {
        if (opcode != ATHROW) {
            onFinally(opcode);
        }
    }

    private void onFinally(int opcode) {
        aa.visitFieldInsn(GETSTATIC, className, metricStaticFieldName(timerMetric), Type.getDescriptor(Histogram.class));
        injectNameAndLabelToStack(timerMetric);

        aa.visitMethodInsn(INVOKESTATIC, "java/lang/System", "nanoTime", "()J", false);
        aa.visitVarInsn(LLOAD, startTimeVar);
        aa.visitInsn(LSUB);
        aa.visitMethodInsn(INVOKESTATIC, METRIC_REPORTER_CLASSNAME, TIMER_METHOD, TIMER_SIGNATURE, false);
    }
}
