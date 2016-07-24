// Copyright 2016 Dino Wernli. All Rights Reserved. See LICENSE for licensing terms.

package me.dinowernli.grpc.prometheus;

import java.util.Optional;

import io.prometheus.client.CollectorRegistry;

/**
 * Holds information about which metrics should be kept track of during rpc calls. Can be used to
 * turn on more elaborate and expensive metrics, such as latency histograms.
 */
public class Configuration {
  private final boolean isIncludeLatencyHistograms;
  private final Optional<CollectorRegistry> collectorRegistry;

  /** Returns a {@link Configuration} for recording all cheap metrics about the rpcs. */
  public static Configuration cheapMetricsOnly() {
    return new Configuration(
        false /* isIncludeLatencyHistograms */, Optional.empty() /* collectorRegistry */);
  }

  /**
   * Returns a {@link Configuration} for recording all metrics about the rpcs. This includes
   * metrics which might produce a lot of data, such as latency histograms.
   */
  public static Configuration allMetrics() {
    return new Configuration(
        true /* isIncludeLatencyHistograms */, Optional.empty() /* collectorRegistry */);
  }

  /**
   * Returns a copy {@link Configuration} with the difference that Prometheus metrics are
   * recorded using the supplied {@link CollectorRegistry}.
   */
  public Configuration withCollectorRegistry(CollectorRegistry collectorRegistry) {
    return new Configuration(isIncludeLatencyHistograms, Optional.of(collectorRegistry));
  }

  /** Returns whether or not latency histograms for calls should be included. */
  public boolean isIncludeLatencyHistograms() {
    return isIncludeLatencyHistograms;
  }

  /** Returns the {@link CollectorRegistry} used to record stats. */
  public CollectorRegistry getCollectorRegistry() {
    return collectorRegistry.orElse(CollectorRegistry.defaultRegistry);
  }

  private Configuration(
      boolean isIncludeLatencyHistograms, Optional<CollectorRegistry> collectorRegistry) {
    this.isIncludeLatencyHistograms = isIncludeLatencyHistograms;
    this.collectorRegistry = collectorRegistry;
  }
}
