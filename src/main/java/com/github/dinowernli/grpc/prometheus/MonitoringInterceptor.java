package com.github.dinowernli.grpc.prometheus;

import java.time.Clock;

import io.grpc.Metadata;
import io.grpc.MethodDescriptor;
import io.grpc.ServerCall;
import io.grpc.ServerCallHandler;
import io.grpc.ServerInterceptor;

/** An interceptor which send stats about the rpcs to Prometheus. */
public class MonitoringInterceptor implements ServerInterceptor {
  private final Clock clock;

  public MonitoringInterceptor(Clock clock) {
    this.clock = clock;
  }

  @Override
  public <R, S> ServerCall.Listener<R> interceptCall(
      MethodDescriptor<R, S> method,
      ServerCall<S> call,
      Metadata requestHeaders,
      ServerCallHandler<R, S> next) {
    return next.startCall(
        method,
        new MonitoringForwardingServerCall<S>(call, clock, method.getFullMethodName()),
        requestHeaders);
  }
}
