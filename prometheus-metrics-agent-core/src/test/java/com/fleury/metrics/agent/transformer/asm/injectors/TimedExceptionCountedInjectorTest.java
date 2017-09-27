package com.fleury.metrics.agent.transformer.asm.injectors;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertTrue;
import static com.fleury.metrics.agent.reporter.TestMetricReader.TimerResult;

import com.fleury.metrics.agent.annotation.ExceptionCounted;
import com.fleury.metrics.agent.annotation.Timed;
import java.lang.reflect.InvocationTargetException;
import java.util.concurrent.TimeUnit;
import org.junit.Test;

/**
 *
 * @author Will Fleury
 */
public class TimedExceptionCountedInjectorTest extends BaseMetricTest {

    @Test
    public void shouldRecordConstructorInvocationStatistics() throws Exception {
        Class<TimedExceptionCountedConstructorClass> clazz = execute(TimedExceptionCountedConstructorClass.class);

        Object obj = clazz.newInstance();

        TimerResult value = metrics.getTimes("constructor_timer", new String[] {"type"}, new String[]{"timed"});
        assertEquals(1, value.count);
        assertTrue(value.sum >= TimeUnit.NANOSECONDS.toMillis(10L));

        assertEquals(0, metrics.getCount("constructor_exceptions", new String[] {"type"}, new String[]{"exception"}));
    }

    @Test
    public void shouldRecordMethodInvocationWhenExceptionThrownStatistics() throws Exception {
        Class<TimedExceptionCountedMethodClassWithException> clazz = execute(TimedExceptionCountedMethodClassWithException.class);

        Object obj = clazz.newInstance();

        boolean exceptionOccured = false;
        try {
            obj.getClass().getMethod("timed").invoke(obj);
        }
        catch (InvocationTargetException e) {
            exceptionOccured = true;
        }

        assertTrue(exceptionOccured);

        TimerResult value = metrics.getTimes("method_timer", new String[] {"type"}, new String[]{"timed"});
        assertEquals(1, value.count);
        assertTrue(value.sum >= TimeUnit.NANOSECONDS.toMillis(10L));

        assertEquals(1, metrics.getCount("method_exceptions", new String[] {"type"}, new String[]{"exception"}));
    }

    public static class TimedExceptionCountedConstructorClass {

        @Timed(name = "constructor_timer", labels = {"type:timed"})
        @ExceptionCounted(name = "constructor_exceptions", labels = {"type:exception"})
        public TimedExceptionCountedConstructorClass() {
            try {
                Thread.sleep(10L);
            }
            catch (InterruptedException e) {
            }
        }
    }

    public static class TimedExceptionCountedMethodClassWithException {

        @Timed(name = "method_timer", labels = {"type:timed"})
        @ExceptionCounted(name = "method_exceptions", labels = {"type:exception"})
        public void timed() {
            try {
                Thread.sleep(10L);
                callService();
            }
            catch (InterruptedException e) {
            } 
        }
        
        public final void callService() {
            BaseMetricTest.performBasicTask();
            throw new RuntimeException();
        }
    }
}
