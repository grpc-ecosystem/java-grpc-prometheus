// Copyright 2016 Dino Wernli. All Rights Reserved. See LICENSE for licensing terms.

package com.github.dinowernli.grpc.prometheus;

import java.time.Clock;
import java.time.Instant;
import java.util.Optional;

import io.grpc.ForwardingServerCall;
import io.grpc.Metadata;
import io.grpc.MethodDescriptor.MethodType;
import io.grpc.ServerCall;
import io.grpc.Status;

/**
 * A {@link ForwardingServerCall} which update Prometheus metrics based on the server-side actions
 * taken for a single rpc, e.g., messages sent, latency, etc.
 */
class MonitoringForwardingServerCall<S> extends ForwardingServerCall.SimpleForwardingServerCall<S> {
  private static final long MILLIS_PER_SECOND = 1000L;

  private final Clock clock;
  private final MethodType methodType;
  private final MetricHelper metricHelper;

  private final Optional<Instant> startInstant;

  MonitoringForwardingServerCall(
      ServerCall<S> delegate,
      Clock clock,
      MethodType methodType,
      MetricHelper metricHelper) {
    super(delegate);
    this.clock = clock;
    this.methodType = methodType;
    this.metricHelper = metricHelper;
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
      metricHelper.addLabels(ServerMetrics.serverStreamMessagesSent).inc();
    }
    super.sendMessage(message);
  }

  private void reportStartMetrics() {
    metricHelper.addLabels(ServerMetrics.serverStartedCounter).inc();
  }

  private void reportEndMetrics(Status status) {
    String codeString = status.getCode().toString();
    double latencySec =
        (clock.millis() - startInstant.get().toEpochMilli()) / (double) MILLIS_PER_SECOND;
    metricHelper.addLabels(ServerMetrics.serverHandledCounter, codeString).inc();
    metricHelper.addLabels(ServerMetrics.serverHandledLatencySecondsHistogram).observe(latencySec);
  }
}