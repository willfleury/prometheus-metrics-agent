package com.fleury.metrics.agent.reporter;

import io.prometheus.client.CollectorRegistry;

/**
 *
 * @author Will Fleury
 */
public class TestMetricReader {

    private final CollectorRegistry registry;

    public TestMetricReader(CollectorRegistry registry) {
        this.registry = registry;
    }

    public long getCount(String name) {
        return getRegistryValue(name);
    }

    public long getCount(String name, String[] labelNames, String[] labelValues) {
        return getRegistryValue(name, labelNames, labelValues);
    }

    public TimerResult getTimes(String name) {
        return new TimerResult(
                getRegistryValue(name + "_sum"),
                getRegistryValue(name + "_count"));
    }

    public TimerResult getTimes(String name, String[] labelNames, String[] labelValues) {
        return new TimerResult(
                getRegistryValue(name + "_sum", labelNames, labelValues),
                getRegistryValue(name + "_count", labelNames, labelValues));
    }

    private long getRegistryValue(String name) {
        Double value = registry.getSampleValue(name);
        return value == null ? 0 : value.longValue();
    }

    private long getRegistryValue(String name, String[] labelNames, String[] labelValues) {
        Double value = registry.getSampleValue(name, labelNames, labelValues);
        return value == null ? 0 : value.longValue();
    }

    public static class TimerResult {
        public final long sum;
        public final long count;

        public TimerResult(long sum, long count) {
            this.sum = sum;
            this.count = count;
        }
    }

    public void reset() {
        CollectorRegistry.defaultRegistry.clear();
    }

}
