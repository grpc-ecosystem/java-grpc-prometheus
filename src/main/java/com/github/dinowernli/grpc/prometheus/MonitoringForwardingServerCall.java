package com.github.dinowernli.grpc.prometheus;

import java.time.Clock;
import java.time.Instant;
import java.util.Optional;

import io.grpc.ForwardingServerCall;
import io.grpc.Metadata;
import io.grpc.ServerCall;
import io.grpc.Status;

/**
 * A {@link ForwardingServerCall} which keeps tracks of relevant stats for a single call and
 * reports them to Prometheus.
 */
class MonitoringForwardingServerCall<S>
    extends ForwardingServerCall.SimpleForwardingServerCall<S> {
  private final Clock clock;

  private final Optional<Instant> startInstant;

  MonitoringForwardingServerCall(ServerCall<S> call, Clock clock) {
    super(call);
    this.clock = clock;
    this.startInstant = Optional.of(clock.instant());
  }

  @Override
  public void close(Status status, Metadata responseHeaders) {
    reportMetrics(status);
    super.close(status, responseHeaders);
  }

  private void reportMetrics(Status status) {
    long latencyMs = clock.millis() - startInstant.get().toEpochMilli();
    // TODO(dino): Report the difference.
  }
}