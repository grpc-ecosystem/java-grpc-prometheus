// Copyright 2016 Dino Wernli. All Rights Reserved. See LICENSE for licensing terms.

package com.github.dinowernli.grpc.prometheus;

import java.time.Clock;

import io.grpc.Metadata;
import io.grpc.MethodDescriptor;
import io.grpc.ServerCall;
import io.grpc.ServerCallHandler;
import io.grpc.ServerInterceptor;

/** An interceptor which sends stats about incoming grpc calls to Prometheus. */
public class MonitoringInterceptor implements ServerInterceptor {
  private final Clock clock;
  private final Configuration configuration;

  public static MonitoringInterceptor create(Configuration configuration) {
    return new MonitoringInterceptor(Clock.systemDefaultZone(), configuration);
  }

  private MonitoringInterceptor(Clock clock, Configuration configuration) {
    this.clock = clock;
    this.configuration = configuration;
  }

  @Override
  public <R, S> ServerCall.Listener<R> interceptCall(
      MethodDescriptor<R, S> method,
      ServerCall<S> call,
      Metadata requestHeaders,
      ServerCallHandler<R, S> next) {
    MetricHelper metricHelper = MetricHelper.create(method);
    ServerCall<S> monitoringCall = new MonitoringForwardingServerCall<S>(
        call, clock, method.getType(), metricHelper, configuration);
    return new MonitoringForwardingServerCallListener<R>(
        next.startCall(method, monitoringCall, requestHeaders), metricHelper);
  }

  /**
   * Holds information about which metrics should be kept track of during rpc calls. Can be used to
   * turn on more elaborate and expensive metrics, such as latency histograms.
   */
  public static class Configuration {
    private final boolean isIncludeLatencyHistograms;

    /**
     * Returns a default {@link Configuration}. By default, we keep track of a few reasonable and
     * cheap metrics.
     */
    public static Configuration defaultConfig() {
      return new Configuration(false /* isIncludeLatencyHistograms */);
    }

    /** Returns a copy of this {@link Configuration} which includes latency histograms. */
    public Configuration withLatencyHistograms() {
      return new Configuration(true /* isIncludeLatencyHistograms */);
    }

    /** Returns whether or not latency histograms for calls should be included. */
    public boolean isIncludeLatencyHistograms() {
      return isIncludeLatencyHistograms;
    }

    private Configuration(boolean isIncludeLatencyHistograms) {
      this.isIncludeLatencyHistograms = isIncludeLatencyHistograms;
    }
  }
}
