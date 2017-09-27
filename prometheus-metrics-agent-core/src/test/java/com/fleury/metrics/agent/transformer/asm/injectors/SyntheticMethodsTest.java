package com.fleury.metrics.agent.transformer.asm.injectors;

import static org.junit.Assert.assertEquals;

import com.fleury.metrics.agent.annotation.Timed;
import org.junit.Test;

/**
 *
 * @author Will Fleury
 */
public class SyntheticMethodsTest extends BaseMetricTest {

    /**
     *
     * The following generics results in two methods with the same name in the CountTimerInvocationsClass bytecode.. This
     * confused the annotation scanning as annotations are placed on both the real and synthetic method.. Therefore need
     * to check the method access code to ensure its not synthetic.
     *
     * public counted(Lcom/fleury/metrics/agent/transformer/asm/injectors/OverrideMethodAnnotationTest$B;)V
     * @Lcom/fleury/metrics/agent/annotation/Counted;(name="counted")
     *   ...
     *
     *
     * public synthetic bridge counted(Lcom/fleury/metrics/agent/transformer/asm/injectors/OverrideMethodAnnotationTest$A;)V
     * @Lcom/fleury/metrics/agent/annotation/Counted;(name="counted")
     *   ...
     *
     *  See https://docs.oracle.com/javase/tutorial/java/generics/bridgeMethods.html
     */


    @Test
    public void shouldCountMethodInvocation() throws Exception {
        Class<CountTimerInvocationsClass> clazz = execute(CountTimerInvocationsClass.class);

        Object obj = clazz.newInstance();

        obj.getClass().getMethod("timed", B.class).invoke(obj, new Object[] {new B()});

        assertEquals(1, metrics.getTimes("timed").count);
    }

    public static class A { }

    public static class B extends  A { }

    public static class BaseClass<T extends A> {
        public void timed(T value) { }
    }

    public static class CountTimerInvocationsClass extends BaseClass<B> {

        @Override
        @Timed(name = "timed")
        public void timed(B value) {
            BaseMetricTest.performBasicTask();
        }
    }

}
