package com.fleury.metrics.agent.transformer.visitors.injectors;

import com.fleury.metrics.agent.model.Metric;
import com.fleury.metrics.agent.model.MetricType;

import static com.fleury.metrics.agent.model.MetricType.ExceptionCounted;
import static com.fleury.metrics.agent.model.MetricType.Timed;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import org.objectweb.asm.Type;
import org.objectweb.asm.commons.AdviceAdapter;

/**
 *
 * @author Will Fleury
 */
public class InjectorFactory {
    
    public static List<Injector> createInjectors(Map<MetricType, Metric> metrics, AdviceAdapter adviceAdapter, String className, Type[] argTypes, int access) {
        List<Injector> injectors = new ArrayList<Injector>();
        
        //handle special case for both exception counter and timer (try catch finally)
        if (metrics.containsKey(ExceptionCounted) && metrics.containsKey(Timed)) {
            injectors.add(new TimedExceptionCountedInjector(
                    metrics.get(Timed), 
                    metrics.get(ExceptionCounted), 
                    adviceAdapter, className, argTypes, access));
            
            metrics.remove(Timed);
            metrics.remove(ExceptionCounted);
        }
        
        for (Metric metric : metrics.values()) {
            injectors.add(createInjector(metric, adviceAdapter, className, argTypes, access));
        }
        
        return injectors;
    }

    public static Injector createInjector(Metric metric, AdviceAdapter adviceAdapter, String className, Type[] argTypes, int access) {
        switch (metric.getType()) {
            case Counted:
                return new CounterInjector(metric, adviceAdapter, className, argTypes, access);

            case Gauged:
                return new GaugeInjector(metric, adviceAdapter, className, argTypes, access);

            case ExceptionCounted:
                return new ExceptionCounterInjector(metric, adviceAdapter, className, argTypes, access);

            case Timed:
                return new TimerInjector(metric, adviceAdapter, className, argTypes, access);

            default:
                throw new IllegalStateException("unknown metric type: " + metric.getType());
        }
    }

}
