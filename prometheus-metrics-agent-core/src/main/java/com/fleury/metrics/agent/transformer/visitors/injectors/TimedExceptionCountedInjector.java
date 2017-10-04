package com.fleury.metrics.agent.transformer.visitors.injectors;

import static com.fleury.metrics.agent.config.Configuration.staticFinalFieldName;
import static com.fleury.metrics.agent.model.MetricType.Counted;
import static com.fleury.metrics.agent.model.MetricType.Timed;

import com.fleury.metrics.agent.model.Metric;
import org.objectweb.asm.Label;
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
 *     long startTimer = System.nanoTime();
 *     try {
 *
 *         //original method code
 *
 *     } catch (Throwable t) {
 *         PrometheusMetricSystem.recordCount(COUNTER, labels);
 *         throw t;
 *     } finally {
 *         PrometheusMetricSystem.recordTime(TIMER, labels);
 *     }
 * }
 * </pre>
 *
 * @author Will Fleury
 */
public class TimedExceptionCountedInjector extends AbstractInjector {
    
    private static final String EXCEPTION_COUNT_METHOD = "recordCount";
    private static final String EXCEPTION_COUNT_SIGNATURE = Type.getMethodDescriptor(
            Type.VOID_TYPE,
            Type.getType(Counted.getCoreType()), Type.getType(String[].class));
    
    private static final String TIMER_METHOD = "recordTime";
    private static final String TIMER_SIGNATURE = Type.getMethodDescriptor(
            Type.VOID_TYPE,
            Type.getType(Timed.getCoreType()), Type.getType(String[].class), Type.LONG_TYPE);
    
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

        aa.visitFieldInsn(GETSTATIC, className, staticFinalFieldName(exceptionMetric), Type.getDescriptor(Counted.getCoreType()));
        injectLabelsToStack(exceptionMetric);
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
        aa.visitFieldInsn(GETSTATIC, className, staticFinalFieldName(timerMetric), Type.getDescriptor(Timed.getCoreType()));
        injectLabelsToStack(timerMetric);

        aa.visitMethodInsn(INVOKESTATIC, "java/lang/System", "nanoTime", "()J", false);
        aa.visitVarInsn(LLOAD, startTimeVar);
        aa.visitInsn(LSUB);
        aa.visitMethodInsn(INVOKESTATIC, METRIC_REPORTER_CLASSNAME, TIMER_METHOD, TIMER_SIGNATURE, false);
    }
}
