// Copyright 2016 Dino Wernli. All Rights Reserved. See LICENSE for licensing terms.

package me.dinowernli.grpc.prometheus;

import io.grpc.ForwardingServerCall;
import io.grpc.Metadata;
import io.grpc.ServerCall;
import io.grpc.Status;
import java.time.Clock;
import java.time.Instant;

/**
 * A {@link ForwardingServerCall} which updates Prometheus metrics based on the server-side actions
 * taken for a single rpc, e.g., messages sent, latency, etc.
 */
class MonitoringServerCall<R, S> extends ForwardingServerCall.SimpleForwardingServerCall<R, S> {
  private static final long MILLIS_PER_SECOND = 1000L;

  private final Clock clock;
  private final GrpcMethod grpcMethod;
  private final ServerMetrics serverMetrics;
  private final Configuration configuration;
  private final Instant startInstant;
  private final Metadata requestMetadata;

  MonitoringServerCall(
      ServerCall<R, S> delegate,
      Clock clock,
      GrpcMethod grpcMethod,
      ServerMetrics serverMetrics,
      Configuration configuration,
      Metadata requestMetadata) {
    super(delegate);
    this.clock = clock;
    this.grpcMethod = grpcMethod;
    this.serverMetrics = serverMetrics;
    this.configuration = configuration;
    this.startInstant = clock.instant();
    this.requestMetadata = requestMetadata;

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
    if (grpcMethod.streamsResponses()) {
      serverMetrics.recordStreamMessageSent(requestMetadata);
    }
    super.sendMessage(message);
  }

  private void reportStartMetrics() {
    serverMetrics.recordCallStarted(requestMetadata);
  }

  private void reportEndMetrics(Status status) {
    Status.Code code = status.getCode();
    serverMetrics.recordServerHandled(code, requestMetadata);
    if (configuration.isIncludeLatencyHistograms()) {
      double latencySec =
          (clock.millis() - startInstant.toEpochMilli()) / (double) MILLIS_PER_SECOND;
      serverMetrics.recordLatency(latencySec, requestMetadata, code);
    }
  }
}
