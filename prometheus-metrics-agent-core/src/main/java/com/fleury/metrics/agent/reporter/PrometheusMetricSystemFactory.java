package com.fleury.metrics.agent.reporter;

import java.util.Map;

/**
 *
 * @author Will Fleury
 */
public class PrometheusMetricSystemFactory {

    public static final PrometheusMetricSystemFactory INSTANCE = new PrometheusMetricSystemFactory();

    public PrometheusMetricSystem metrics;

    public void init(Map<String, Object> configuration) {
        metrics = new PrometheusMetricSystem(configuration);
    }

    public PrometheusMetricSystem get() {
        return metrics;
    }

}
