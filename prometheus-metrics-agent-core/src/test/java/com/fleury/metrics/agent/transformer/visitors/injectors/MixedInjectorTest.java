package com.fleury.metrics.agent.transformer.visitors.injectors;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertTrue;
import static com.fleury.metrics.agent.reporter.TestMetricReader.TimerResult;

import com.fleury.metrics.agent.annotation.Counted;
import com.fleury.metrics.agent.annotation.ExceptionCounted;
import com.fleury.metrics.agent.annotation.Timed;
import java.lang.reflect.InvocationTargetException;
import java.util.concurrent.TimeUnit;
import org.junit.Test;

/**
 *
 * @author Will Fleury
 */
public class MixedInjectorTest extends BaseMetricTest {

    @Test
    public void shouldRecordConstructorInvocationStatistics() throws Exception {
        Class<MixedMetricConstructorClass> clazz = execute(MixedMetricConstructorClass.class);

        Object obj = clazz.newInstance();

        TimerResult value = metrics.getTimes("constructor_timed", new String[] {"type"}, new String[]{"timed"});
        assertEquals(1, value.count);
        assertTrue(value.sum >= TimeUnit.NANOSECONDS.toMillis(10L));

        assertEquals(1, metrics.getCount("constructor_count", new String[] {"type"}, new String[]{"counted"}));
        assertEquals(0, metrics.getCount("constructor_exceptions", new String[] {"type"}, new String[]{"exception"}));
    }

    @Test
    public void shouldRecordMethodInvocationStatistics() throws Exception {
        Class<MixedMetricMethodClass> clazz = execute(MixedMetricMethodClass.class);

        Object obj = clazz.newInstance();

        obj.getClass().getMethod("timed").invoke(obj);

        TimerResult value = metrics.getTimes("timed_timed", new String[] {"type"}, new String[]{"timed"});
        assertEquals(1, value.count);
        assertTrue(value.sum >= TimeUnit.NANOSECONDS.toMillis(10L));

        assertEquals(1, metrics.getCount("timed_count", new String[] {"type"}, new String[]{"counted"}));
        assertEquals(0, metrics.getCount("timed_exceptions", new String[] {"type"}, new String[]{"exception"}));
    }

    @Test
    public void shouldRecordMethodInvocationWhenExceptionThrownStatistics() throws Exception {
        Class<MixedMetricMethodClassWithException> clazz = execute(MixedMetricMethodClassWithException.class);

        Object obj = clazz.newInstance();

        boolean exceptionOccured = false;
        try {
            obj.getClass().getMethod("timed").invoke(obj);
        }
        catch (InvocationTargetException e) {
            exceptionOccured = true;
        }

        assertTrue(exceptionOccured);

        TimerResult value = metrics.getTimes("timed_timed", new String[] {"type"}, new String[]{"timed"});
        assertEquals(1, value.count);
        assertTrue(value.sum >= TimeUnit.NANOSECONDS.toMillis(10L));

        assertEquals(1, metrics.getCount("timed_exceptions", new String[] {"type"}, new String[]{"exception"}));
        assertEquals(1, metrics.getCount("timed_count", new String[] {"type"}, new String[]{"counted"}));
    }

    public static class MixedMetricConstructorClass {

        @Timed(name = "constructor_timed", labels = {"type:timed"})
        @ExceptionCounted(name = "constructor_exceptions", labels = {"type:exception"})
        @Counted(name = "constructor_count", labels = {"type:counted"})
        public MixedMetricConstructorClass() {
            try {
                Thread.sleep(10L);
            }
            catch (InterruptedException e) {
            }
        }
    }

    public static class MixedMetricMethodClass {

        @Timed(name = "timed_timed", labels = {"type:timed"})
        @ExceptionCounted(name = "timed_exceptions", labels = {"type:exception"})
        @Counted(name = "timed_count", labels = {"type:counted"})
        public void timed() {
            try {
                Thread.sleep(10L);
            }
            catch (InterruptedException e) {
            }
        }
    }

    public static class MixedMetricMethodClassWithException {

        @Timed(name = "timed_timed", labels = {"type:timed"})
        @ExceptionCounted(name = "timed_exceptions", labels = {"type:exception"})
        @Counted(name = "timed_count", labels = {"type:counted"})
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
