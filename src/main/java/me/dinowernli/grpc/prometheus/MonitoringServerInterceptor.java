// Copyright 2016 Dino Wernli. All Rights Reserved. See LICENSE for licensing terms.

package me.dinowernli.grpc.prometheus;

import java.time.Clock;
import java.util.Optional;

import io.grpc.Metadata;
import io.grpc.MethodDescriptor;
import io.grpc.ServerCall;
import io.grpc.ServerCallHandler;
import io.grpc.ServerInterceptor;
import io.prometheus.client.CollectorRegistry;

/** An interceptor which sends stats about incoming grpc calls to Prometheus. */
public class MonitoringServerInterceptor implements ServerInterceptor {
  private final Clock clock;
  private final Configuration configuration;

  public static MonitoringServerInterceptor create(Configuration configuration) {
    return new MonitoringServerInterceptor(Clock.systemDefaultZone(), configuration);
  }

  private MonitoringServerInterceptor(Clock clock, Configuration configuration) {
    this.clock = clock;
    this.configuration = configuration;
  }

  @Override
  public <R, S> ServerCall.Listener<R> interceptCall(
      MethodDescriptor<R, S> method,
      ServerCall<S> call,
      Metadata requestHeaders,
      ServerCallHandler<R, S> next) {
    // TODO(dino): If we cache the ServerMetrics instance, we can achieve an initial 0 value on
    // registration and save some cycles here where we always create a new one per-request.
    ServerMetrics metrics = ServerMetrics.create(method, configuration.getCollectorRegistry());
    ServerCall<S> monitoringCall = new MonitoringServerCall<S>(
        call, clock, method.getType(), metrics, configuration);
    return new MonitoringServerCallListener<R>(
        next.startCall(method, monitoringCall, requestHeaders), metrics);
  }

  /**
   * Holds information about which metrics should be kept track of during rpc calls. Can be used to
   * turn on more elaborate and expensive metrics, such as latency histograms.
   */
  public static class Configuration {
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
          false /* isIncludeLatencyHistograms */, Optional.empty() /* collectorRegistry */);
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
    public Optional<CollectorRegistry> getCollectorRegistry() {
      return collectorRegistry;
    }

    private Configuration(
        boolean isIncludeLatencyHistograms, Optional<CollectorRegistry> collectorRegistry) {
      this.isIncludeLatencyHistograms = isIncludeLatencyHistograms;
      this.collectorRegistry = collectorRegistry;
    }
  }
}
