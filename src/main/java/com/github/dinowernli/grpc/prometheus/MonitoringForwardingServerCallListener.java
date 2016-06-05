package com.github.dinowernli.grpc.prometheus;

import io.grpc.ForwardingServerCallListener;
import io.grpc.ServerCall;

/**
 * A {@link ForwardingServerCallListener} which updates Prometheus metrics for a single rpc based
 * on updates received from grpc.
 */
class MonitoringForwardingServerCallListener<R>
    extends ForwardingServerCallListener<R> {
  private final ServerCall.Listener<R> delegate;
  private final ServerMetrics serverMetrics;

  MonitoringForwardingServerCallListener(
      ServerCall.Listener<R> delegate, ServerMetrics serverMetrics) {
    this.delegate = delegate;
    this.serverMetrics = serverMetrics;
  }

  @Override
  protected ServerCall.Listener<R> delegate() {
    return delegate;
  }

  @Override
  public void onMessage(R request) {
    serverMetrics.recordMessageReceived();
    super.onMessage(request);
  }
}