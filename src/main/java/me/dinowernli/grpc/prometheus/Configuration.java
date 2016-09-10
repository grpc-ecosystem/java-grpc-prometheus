// Copyright 2016 Dino Wernli. All Rights Reserved. See LICENSE for licensing terms.

package me.dinowernli.grpc.prometheus;

import io.prometheus.client.CollectorRegistry;

/**
 * Holds information about which metrics should be kept track of during rpc calls. Can be used to
 * turn on more elaborate and expensive metrics, such as latency histograms.
 */
public class Configuration {
  private static double[] DEFAULT_LATENCY_BUCKETS =
      new double[] {.001, .005, .01, .05, 0.075, .1, .25, .5, 1, 2, 5, 10};

  private final boolean isIncludeLatencyHistograms;
  private final CollectorRegistry collectorRegistry;
  private final double[] latencyBuckets;

  /** Returns a {@link Configuration} for recording all cheap metrics about the rpcs. */
  public static Configuration cheapMetricsOnly() {
    return new Configuration(
        false /* isIncludeLatencyHistograms */,
        CollectorRegistry.defaultRegistry,
        DEFAULT_LATENCY_BUCKETS);
  }

  /**
   * Returns a {@link Configuration} for recording all metrics about the rpcs. This includes
   * metrics which might produce a lot of data, such as latency histograms.
   */
  public static Configuration allMetrics() {
    return new Configuration(
        true /* isIncludeLatencyHistograms */,
        CollectorRegistry.defaultRegistry,
        DEFAULT_LATENCY_BUCKETS);
  }

  /**
   * Returns a copy {@link Configuration} with the difference that Prometheus metrics are
   * recorded using the supplied {@link CollectorRegistry}.
   */
  public Configuration withCollectorRegistry(CollectorRegistry collectorRegistry) {
    return new Configuration(isIncludeLatencyHistograms, collectorRegistry, latencyBuckets);
  }

  /**
   * Returns a copy {@link Configuration} with the difference that the latency histogram values are
   * recorded with the specified set of buckets.
   */
  public Configuration withLatencyBuckets(double[] buckets) {
    return new Configuration(isIncludeLatencyHistograms, collectorRegistry, buckets);
  }

  /** Returns whether or not latency histograms for calls should be included. */
  public boolean isIncludeLatencyHistograms() {
    return isIncludeLatencyHistograms;
  }

  /** Returns the {@link CollectorRegistry} used to record stats. */
  public CollectorRegistry getCollectorRegistry() {
    return collectorRegistry;
  }

  /** Returns the histogram buckets to use for latency metrics. */
  public double[] getLatencyBuckets() {
    return latencyBuckets;
  }

  private Configuration(
      boolean isIncludeLatencyHistograms,
      CollectorRegistry collectorRegistry,
      double[] latencyBuckets) {
    this.isIncludeLatencyHistograms = isIncludeLatencyHistograms;
    this.collectorRegistry = collectorRegistry;
    this.latencyBuckets = latencyBuckets;
  }
}
