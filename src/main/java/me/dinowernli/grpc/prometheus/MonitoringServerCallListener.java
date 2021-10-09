// Copyright 2016 Dino Wernli. All Rights Reserved. See LICENSE for licensing terms.

package me.dinowernli.grpc.prometheus;

import io.grpc.Metadata;
import io.grpc.ForwardingServerCallListener;
import io.grpc.ServerCall;

/**
 * A {@link ForwardingServerCallListener} which updates Prometheus metrics for a single rpc based
 * on updates received from grpc.
 */
class MonitoringServerCallListener<R> extends ForwardingServerCallListener<R> {
  private final ServerCall.Listener<R> delegate;
  private final GrpcMethod grpcMethod;
  private final ServerMetrics serverMetrics;
  private final Metadata requestMetadata;

  MonitoringServerCallListener(
      ServerCall.Listener<R> delegate, ServerMetrics serverMetrics, GrpcMethod grpcMethod, Metadata requestMetadata) {
    this.delegate = delegate;
    this.serverMetrics = serverMetrics;
    this.grpcMethod = grpcMethod;
    this.requestMetadata = requestMetadata;
  }

  @Override
  protected ServerCall.Listener<R> delegate() {
    return delegate;
  }

  @Override
  public void onMessage(R request) {
    if (grpcMethod.streamsRequests()) {
      serverMetrics.recordStreamMessageReceived(requestMetadata);
    }
    super.onMessage(request);
  }
}