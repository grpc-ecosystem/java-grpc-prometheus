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

  public static MonitoringInterceptor create() {
    return new MonitoringInterceptor(Clock.systemDefaultZone());
  }

  public MonitoringInterceptor(Clock clock) {
    this.clock = clock;
  }

  @Override
  public <R, S> ServerCall.Listener<R> interceptCall(
      MethodDescriptor<R, S> method,
      ServerCall<S> call,
      Metadata requestHeaders,
      ServerCallHandler<R, S> next) {
    MetricHelper metricHelper = MetricHelper.create(method);
    ServerCall<S> monitoringCall =
        new MonitoringForwardingServerCall<S>(call, clock, method.getType(), metricHelper);
    return new MonitoringForwardingServerCallListener<R>(
        next.startCall(method, monitoringCall, requestHeaders), metricHelper);
  }
}
