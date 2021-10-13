// Copyright 2016 Dino Wernli. All Rights Reserved. See LICENSE for licensing terms.

package me.dinowernli.grpc.prometheus;

import io.prometheus.client.CollectorRegistry;
import java.util.ArrayList;
import java.util.List;
import java.util.stream.Collectors;

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
  private final List<String> labelHeaders;

  /** Returns a {@link Configuration} for recording all cheap metrics about the rpcs. */
  public static Configuration cheapMetricsOnly() {
    return new Configuration(
        false /* isIncludeLatencyHistograms */,
        CollectorRegistry.defaultRegistry,
        DEFAULT_LATENCY_BUCKETS,
        new ArrayList<>());
  }

  /**
   * Returns a {@link Configuration} for recording all metrics about the rpcs. This includes metrics
   * which might produce a lot of data, such as latency histograms.
   */
  public static Configuration allMetrics() {
    return new Configuration(
        true /* isIncludeLatencyHistograms */,
        CollectorRegistry.defaultRegistry,
        DEFAULT_LATENCY_BUCKETS,
        new ArrayList<>());
  }

  /**
   * Returns a copy {@link Configuration} with the difference that Prometheus metrics are recorded
   * using the supplied {@link CollectorRegistry}.
   */
  public Configuration withCollectorRegistry(CollectorRegistry collectorRegistry) {
    return new Configuration(
        isIncludeLatencyHistograms, collectorRegistry, latencyBuckets, labelHeaders);
  }

  /**
   * Returns a copy {@link Configuration} with the difference that the latency histogram values are
   * recorded with the specified set of buckets.
   */
  public Configuration withLatencyBuckets(double[] buckets) {
    return new Configuration(isIncludeLatencyHistograms, collectorRegistry, buckets, labelHeaders);
  }

  /**
   * Returns a copy {@link Configuration} that recognizes the given list of header names and uses
   * their value from each request as prometheus labels.
   *
   * <p>Since hyphens is a common character in header names, and since Prometheus does not allow
   * hyphens in label names, All hyphens in the list of header names will be converted to
   * underscores before being added as metric label names.
   *
   * <p>If one of the headers added here is absent in one of the requests, its metric value for that
   * request will be an empty string.
   *
   * <p>Example: {@code withLabelHeaders(Arrays.asList("User-Agent"))} will make all metrics carry a
   * label "User_Agent", with label value filled in from the value of the "User-Agent" header of
   * each request.
   */
  public Configuration withLabelHeaders(List<String> headers) {
    List<String> newHeaders = new ArrayList<>(labelHeaders);
    newHeaders.addAll(headers);
    return new Configuration(
        isIncludeLatencyHistograms, collectorRegistry, latencyBuckets, newHeaders);
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

  /** Returns the configured list of headers to be used as labels. */
  public List<String> getLabelHeaders() {
    return labelHeaders;
  }

  /**
   * Returns the sanitized version of the label headers, after turning all hyphens to underscores.
   */
  public List<String> getSanitizedLabelHeaders() {
    return labelHeaders.stream().map(h -> h.replaceAll("-", "_")).collect(Collectors.toList());
  }

  private Configuration(
      boolean isIncludeLatencyHistograms,
      CollectorRegistry collectorRegistry,
      double[] latencyBuckets,
      List<String> labelHeaders) {
    this.isIncludeLatencyHistograms = isIncludeLatencyHistograms;
    this.collectorRegistry = collectorRegistry;
    this.latencyBuckets = latencyBuckets;
    this.labelHeaders = labelHeaders;
  }
}
