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
  private final MetricHelper metricHelper;

  MonitoringForwardingServerCallListener(
      ServerCall.Listener<R> delegate, MetricHelper metricHelper) {
    this.delegate = delegate;
    this.metricHelper = metricHelper;
  }

  @Override
  protected ServerCall.Listener<R> delegate() {
    return delegate;
  }

  @Override
  public void onMessage(R request) {
    metricHelper.addLabels(ServerMetrics.serverStreamMessagesReceived).inc();
    super.onMessage(request);
  }
}