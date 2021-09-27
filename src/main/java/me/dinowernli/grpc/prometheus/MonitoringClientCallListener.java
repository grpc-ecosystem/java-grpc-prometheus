// Copyright 2016 Dino Wernli. All Rights Reserved. See LICENSE for licensing terms.

package me.dinowernli.grpc.prometheus;

import java.time.Clock;
import java.time.Instant;

import io.grpc.ClientCall;
import io.grpc.ForwardingClientCallListener;
import io.grpc.Metadata;
import io.grpc.Status;

class MonitoringClientCallListener<S> extends ForwardingClientCallListener<S> {
  private static final long MILLIS_PER_SECOND = 1000L;

  private final ClientCall.Listener<S> delegate;
  private final ClientMetrics clientMetrics;
  private final GrpcMethod grpcMethod;
  private final Configuration configuration;
  private final Clock clock;
  private final Instant startInstant;
  private final Metadata requestHeaders;

  MonitoringClientCallListener(
      ClientCall.Listener<S> delegate,
      ClientMetrics clientMetrics,
      GrpcMethod grpcMethod,
      Configuration configuration,
      Clock clock,
      Metadata requestHeaders) {
    this.delegate = delegate;
    this.clientMetrics = clientMetrics;
    this.grpcMethod = grpcMethod;
    this.configuration = configuration;
    this.clock = clock;
    this.startInstant = clock.instant();
    this.requestHeaders = requestHeaders;
  }

  @Override
  protected ClientCall.Listener<S> delegate() {
    return delegate;
  }

  @Override
  public void onClose(Status status, Metadata metadata) {
    clientMetrics.recordClientHandled(status.getCode(), requestHeaders);
    if (configuration.isIncludeLatencyHistograms()) {
      double latencySec =
          (clock.millis() - startInstant.toEpochMilli()) / (double) MILLIS_PER_SECOND;
      clientMetrics.recordLatency(latencySec, requestHeaders);
    }
    super.onClose(status, metadata);
  }

  @Override
  public void onMessage(S responseMessage) {
    if (grpcMethod.streamsResponses()) {
      clientMetrics.recordStreamMessageReceived(requestHeaders);
    }
    super.onMessage(responseMessage);
  }
}
