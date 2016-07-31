// Copyright 2016 Dino Wernli. All Rights Reserved. See LICENSE for licensing terms.

package me.dinowernli.grpc.prometheus;

import java.time.Clock;

import io.grpc.ClientCall;
import io.grpc.ForwardingClientCall;
import io.grpc.Metadata;

/**
 * A {@link SimpleForwardingClientCall} which increments prometheus counters for the rpc call.
 */
class MonitoringClientCall<R, S> extends ForwardingClientCall.SimpleForwardingClientCall<R, S> {
  private final ClientMetrics clientMetrics;
  private final GrpcMethod grpcMethod;
  private final Configuration configuration;
  private final Clock clock;

  MonitoringClientCall(
      ClientCall<R, S> delegate,
      ClientMetrics clientMetrics,
      GrpcMethod grpcMethod,
      Configuration configuration,
      Clock clock) {
    super(delegate);
    this.clientMetrics = clientMetrics;
    this.grpcMethod = grpcMethod;
    this.configuration = configuration;
    this.clock = clock;
  }

  @Override
  public void start(ClientCall.Listener<S> delegate, Metadata metadata) {
    clientMetrics.recordCallStarted();
    super.start(new MonitoringClientCallListener<>(
        delegate, clientMetrics, grpcMethod, configuration, clock), metadata);
  }

  @Override
  public void sendMessage(R requestMessage) {
    if (grpcMethod.streamsRequests()) {
      clientMetrics.recordStreamMessageSent();
    }
    super.sendMessage(requestMessage);
  }
}
