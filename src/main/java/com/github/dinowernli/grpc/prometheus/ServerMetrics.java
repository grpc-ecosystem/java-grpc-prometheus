package com.github.dinowernli.grpc.prometheus;

import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import java.util.Optional;

import io.grpc.MethodDescriptor;
import io.grpc.Status.Code;
import io.prometheus.client.CollectorRegistry;
import io.prometheus.client.Counter;
import io.prometheus.client.Histogram;
import io.prometheus.client.SimpleCollector;

/**
 * Prometheus metric definitions used for server-side monitoring of grpc services.
 *
 * Instances of this class hold the counters we increment for a specific pair of grpc service
 * definition and collection registry.
 */
class ServerMetrics {
  private static final Counter.Builder serverStartedBuilder = Counter.build()
      .namespace("grpc")
      .subsystem("server")
      .name("started_total")
      .labelNames("grpc_type", "grpc_service", "grpc_method")
      .help("Total number of RPCs started on the server.");

  private static final Counter.Builder serverHandledBuilder = Counter.build()
      .namespace("grpc")
      .subsystem("server")
      .name("handled_total")
      .labelNames("grpc_type", "grpc_service", "grpc_method", "code")
      .help("Total number of RPCs completed on the server, regardless of success or failure.");

  private static final Histogram.Builder serverHandledLatencySecondsBuilder =
      Histogram.build()
          .namespace("grpc")
          .subsystem("server")
          .name("handled_total")
          .labelNames("grpc_type", "grpc_service", "grpc_method")
          .help("Histogram of response latency (seconds) of gRPC that had been application-level " +
              "handled by the server.");

  private static final Counter.Builder serverStreamMessagesReceivedBuilder = Counter.build()
      .namespace("grpc")
      .subsystem("server")
      .name("msg_received_total")
      .labelNames("grpc_type", "grpc_service", "grpc_method")
      .help("Total number of messages received from the client.");

  private static final Counter.Builder serverStreamMessagesSentBuilder = Counter.build()
      .namespace("grpc")
      .subsystem("server")
      .name("msg_received_total")
      .labelNames("grpc_type", "grpc_service", "grpc_method")
      .help("Total number of gRPC stream messages sent by the server.");

  private final Counter serverStarted;
  private final Counter serverHandled;
  private final Histogram serverHandledLatencySeconds;
  private final Counter serverStreamMessagesReceived;
  private final Counter serverStreamMessagesSent;

  private final String methodTypeLabel;
  private final String serviceNameLabel;
  private final String methodNameLabel;

  /**
   * Creates an instance of {@link ServerMetrics} for the supplied method. If the
   * {@link CollectorRegistry} is empty, the default global registry is used.
   */
  static <R, S> ServerMetrics create(
      MethodDescriptor<R, S> method, Optional<CollectorRegistry> collectorRegistry) {
    CollectorRegistry registry = collectorRegistry.orElse(CollectorRegistry.defaultRegistry);
    String serviceName = MethodDescriptor.extractFullServiceName(method.getFullMethodName());
    String methodName = method.getFullMethodName();
    return new ServerMetrics(method.getType().toString(), serviceName, methodName, registry);
  }

  private ServerMetrics(
      String methodTypeLabel,
      String serviceNameLabel,
      String methodNameLabel,
      CollectorRegistry registry) {
    this.methodNameLabel = methodNameLabel;
    this.methodTypeLabel = methodTypeLabel;
    this.serviceNameLabel = serviceNameLabel;

    this.serverStarted = serverStartedBuilder.register(registry);
    this.serverHandled = serverHandledBuilder.register(registry);
    this.serverHandledLatencySeconds = serverHandledLatencySecondsBuilder.register(registry);
    this.serverStreamMessagesReceived = serverStreamMessagesReceivedBuilder.register(registry);
    this.serverStreamMessagesSent = serverStreamMessagesSentBuilder.register(registry);
  }

  public void recordCallStarted() {
    addLabels(serverStarted).inc();
  }

  public void recordStreamMessageSent() {
    addLabels(serverStreamMessagesSent).inc();
  }

  public void recordServerHandled(Code code) {
    addLabels(serverHandled, code.toString()).inc();
  }

  public void recordLatency(double latencySec) {
    addLabels(serverHandledLatencySeconds).observe(latencySec);
  }

  public void recordMessageReceived() {
    addLabels(serverStreamMessagesReceived).inc();
  }

  private <T> T addLabels(SimpleCollector<T> collector, String... labels) {
    List<String> allLabels = new ArrayList<>();
    Collections.addAll(allLabels, methodTypeLabel, serviceNameLabel, methodNameLabel);
    Collections.addAll(allLabels, labels);
    return collector.labels(allLabels.toArray(new String[0]));
  }
}
