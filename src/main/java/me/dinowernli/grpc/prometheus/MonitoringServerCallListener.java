package me.dinowernli.grpc.prometheus;

import io.grpc.ForwardingServerCallListener;
import io.grpc.MethodDescriptor.MethodType;
import io.grpc.ServerCall;

/**
 * A {@link ForwardingServerCallListener} which updates Prometheus metrics for a single rpc based
 * on updates received from grpc.
 */
class MonitoringServerCallListener<R>
    extends ForwardingServerCallListener<R> {
  private final ServerCall.Listener<R> delegate;
  private final MethodType methodType;
  private final ServerMetrics serverMetrics;

  MonitoringServerCallListener(
      ServerCall.Listener<R> delegate, ServerMetrics serverMetrics, MethodType methodType) {
    this.delegate = delegate;
    this.serverMetrics = serverMetrics;
    this.methodType = methodType;
  }

  @Override
  protected ServerCall.Listener<R> delegate() {
    return delegate;
  }

  @Override
  public void onMessage(R request) {
    if (methodType == MethodType.CLIENT_STREAMING || methodType == MethodType.BIDI_STREAMING) {
      serverMetrics.recordStreamMessageReceived();
    }
    super.onMessage(request);
  }
}