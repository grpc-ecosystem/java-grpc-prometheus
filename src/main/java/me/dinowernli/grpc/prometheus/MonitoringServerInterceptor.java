// Copyright 2016 Dino Wernli. All Rights Reserved. See LICENSE for licensing terms.

package me.dinowernli.grpc.prometheus;

import io.grpc.Metadata;
import io.grpc.MethodDescriptor;
import io.grpc.ServerCall;
import io.grpc.ServerCallHandler;
import io.grpc.ServerInterceptor;
import java.time.Clock;

/** A {@link ServerInterceptor} which sends stats about incoming grpc calls to Prometheus. */
public class MonitoringServerInterceptor implements ServerInterceptor {
  private final Clock clock;
  private final Configuration configuration;
  private final ServerMetrics.Factory serverMetricsFactory;

  public static MonitoringServerInterceptor create(Configuration configuration) {
    return new MonitoringServerInterceptor(
        Clock.systemDefaultZone(), configuration, new ServerMetrics.Factory(configuration));
  }

  private MonitoringServerInterceptor(
      Clock clock, Configuration configuration, ServerMetrics.Factory serverMetricsFactory) {
    this.clock = clock;
    this.configuration = configuration;
    this.serverMetricsFactory = serverMetricsFactory;
  }

  @Override
  public <R, S> ServerCall.Listener<R> interceptCall(
      ServerCall<R, S> call, Metadata requestMetadata, ServerCallHandler<R, S> next) {
    MethodDescriptor<R, S> methodDescriptor = call.getMethodDescriptor();
    GrpcMethod grpcMethod = GrpcMethod.of(methodDescriptor);
    ServerMetrics metrics = serverMetricsFactory.createMetricsForMethod(grpcMethod);
    ServerCall<R, S> monitoringCall =
        new MonitoringServerCall(call, clock, grpcMethod, metrics, configuration, requestMetadata);
    return new MonitoringServerCallListener<>(
        next.startCall(monitoringCall, requestMetadata), metrics, grpcMethod, requestMetadata);
  }
}
