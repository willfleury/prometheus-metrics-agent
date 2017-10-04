package com.fleury.metrics.agent.model;

import com.fleury.metrics.agent.annotation.Counted;
import com.fleury.metrics.agent.annotation.ExceptionCounted;
import com.fleury.metrics.agent.annotation.Gauged;
import com.fleury.metrics.agent.annotation.Timed;
import io.prometheus.client.Counter;
import io.prometheus.client.Gauge;
import io.prometheus.client.Histogram;
import org.objectweb.asm.Type;

/**
 *
 * @author Will Fleury
 */
public enum MetricType {

    Counted(Counted.class, Counter.class),
    Gauged(Gauged.class, Gauge.class),
    Timed(Timed.class, Histogram.class),
    ExceptionCounted(ExceptionCounted.class, Counter.class);

    private final Class annotation;
    private final Class coreType;
    private final String desc;

    MetricType(Class annotation, Class coreType) {
        this.annotation = annotation;
        this.coreType = coreType;
        this.desc = Type.getDescriptor(annotation);
    }

    public Class getAnnotation() {
        return annotation;
    }

    public Class getCoreType() {
        return coreType;
    }
    
    public String getDesc() {
        return desc;
    }
}
