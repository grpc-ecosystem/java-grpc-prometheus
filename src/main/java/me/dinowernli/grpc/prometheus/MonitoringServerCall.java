// Copyright 2016 Dino Wernli. All Rights Reserved. See LICENSE for licensing terms.

package me.dinowernli.grpc.prometheus;

import java.time.Clock;
import java.time.Instant;
import java.util.Optional;

import io.grpc.ForwardingServerCall;
import io.grpc.Metadata;
import io.grpc.MethodDescriptor.MethodType;
import io.grpc.ServerCall;
import io.grpc.Status;
import me.dinowernli.grpc.prometheus.MonitoringServerInterceptor.Configuration;

/**
 * A {@link ForwardingServerCall} which update Prometheus metrics based on the server-side actions
 * taken for a single rpc, e.g., messages sent, latency, etc.
 */
class MonitoringServerCall<S> extends ForwardingServerCall.SimpleForwardingServerCall<S> {
  private static final long MILLIS_PER_SECOND = 1000L;

  private final Clock clock;
  private final MethodType methodType;
  private final ServerMetrics serverMetrics;
  private final Configuration configuration;

  private final Optional<Instant> startInstant;

  MonitoringServerCall(
      ServerCall<S> delegate,
      Clock clock,
      MethodType methodType,
      ServerMetrics serverMetrics,
      Configuration configuration) {
    super(delegate);
    this.clock = clock;
    this.methodType = methodType;
    this.serverMetrics = serverMetrics;
    this.configuration = configuration;
    this.startInstant = Optional.of(clock.instant());

    // TODO(dino): Consider doing this in the onReady() method of the listener instead.
    reportStartMetrics();
  }

  @Override
  public void close(Status status, Metadata responseHeaders) {
    reportEndMetrics(status);
    super.close(status, responseHeaders);
  }

  @Override
  public void sendMessage(S message) {
    if (methodType == MethodType.SERVER_STREAMING || methodType == MethodType.BIDI_STREAMING) {
      serverMetrics.recordStreamMessageSent();
    }
    super.sendMessage(message);
  }

  private void reportStartMetrics() {
    serverMetrics.recordCallStarted();
  }

  private void reportEndMetrics(Status status) {
    serverMetrics.recordServerHandled(status.getCode());
    if (configuration.isIncludeLatencyHistograms()) {
      double latencySec =
          (clock.millis() - startInstant.get().toEpochMilli()) / (double) MILLIS_PER_SECOND;
      serverMetrics.recordLatency(latencySec);
    }
  }
}