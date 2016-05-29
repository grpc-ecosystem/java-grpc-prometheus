// Copyright 2016 Dino Wernli. All Rights Reserved. See LICENSE for licensing terms.

package com.github.dinowernli.grpc.prometheus;

import java.time.Clock;
import java.time.Instant;
import java.util.Optional;

import io.grpc.ForwardingServerCall;
import io.grpc.Metadata;
import io.grpc.MethodDescriptor;
import io.grpc.MethodDescriptor.MethodType;
import io.grpc.ServerCall;
import io.grpc.Status;
import io.prometheus.client.Counter;
import io.prometheus.client.Histogram;
import io.prometheus.client.SimpleCollector;

/**
 * A {@link ForwardingServerCall} which keeps track of relevant stats for a single call and
 * reports them to Prometheus.
 */
class MonitoringForwardingServerCall<S> extends ForwardingServerCall.SimpleForwardingServerCall<S> {
  private static final long MILLIS_PER_SECOND = 1000L;

  private static final Counter serverStartedCounter = Counter.build()
      .namespace("grpc")
      .subsystem("server")
      .name("started_total")
      .labelNames("grpc_type", "grpc_service", "grpc_method")
      .help("Total number of RPCs started on the server.")
      .register();

  private static final Counter serverHandledCounter = Counter.build()
      .namespace("grpc")
      .subsystem("server")
      .name("handled_total")
      .labelNames("grpc_type", "grpc_service", "grpc_method", "code")
      .help("Total number of RPCs completed on the server, regardless of success or failure.")
      .register();

  private static final Histogram serverHandledLatencySecondsHistogram = Histogram.build()
      .namespace("grpc")
      .subsystem("server")
      .name("handled_total")
      .labelNames("grpc_type", "grpc_service", "grpc_method")
      .help("Histogram of response latency (seconds) of gRPC that had been application-level " +
          "handled by the server.")
      .register();

  private static final Counter serverStreamMessagesReceived = Counter.build()
      .namespace("grpc")
      .subsystem("server")
      .name("msg_received_total")
      .labelNames("grpc_type", "grpc_service", "grpc_method")
      .help("Total number of RPCs started on the server.")
      .register();

  private static final Counter serverStreamMessagesSent = Counter.build()
      .namespace("grpc")
      .subsystem("server")
      .name("msg_received_total")
      .labelNames("grpc_type", "grpc_service", "grpc_method")
      .help("Total number of gRPC stream messages sent by the server.")
      .register();


  private final Clock clock;
  private final String methodName;
  private final MethodType methodType;
  private final String serviceName;

  private final Optional<Instant> startInstant;

  public static <R, S> ServerCall<S> create(
      ServerCall<S> call, Clock clock, MethodDescriptor<R, S> method) {
    String serviceName = MethodDescriptor.extractFullServiceName(method.getFullMethodName());
    String methodName = method.getFullMethodName();
    return new MonitoringForwardingServerCall<S>(
        call, clock, methodName, serviceName, method.getType());
  }

  MonitoringForwardingServerCall(
      ServerCall<S> call,
      Clock clock,
      String methodName,
      String serviceName,
      MethodType methodType) {
    super(call);
    this.clock = clock;
    this.methodName = methodName;
    this.serviceName = serviceName;
    this.methodType = methodType;
    this.startInstant = Optional.of(clock.instant());

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
      addLabels(serverStreamMessagesSent).inc();
    }
    super.sendMessage(message);

    // TODO(dino): Figure out how to count the number of messages received.
  }

  private void reportStartMetrics() {
    addLabels(serverStartedCounter).inc();
  }

  private void reportEndMetrics(Status status) {
    String codeString = status.getCode().toString();
    double latencySeconds =
        clock.millis() - startInstant.get().toEpochMilli() / (double) MILLIS_PER_SECOND;
    addLabels(serverHandledCounter, codeString).inc();
    addLabels(serverHandledLatencySecondsHistogram, codeString).observe(latencySeconds);
  }

  private <T> T addLabels(SimpleCollector<T> collector, String... labels) {
    String[] allLabels = new String[labels.length + 3];
    allLabels[0] = methodType.toString();
    allLabels[1] = serviceName;
    allLabels[2] = methodName;
    for (int i = 0; i < labels.length; ++i) {
      allLabels[i + 3] = labels[i];
    }
    return collector.labels(allLabels);
  }
}