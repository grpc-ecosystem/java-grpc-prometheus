package com.github.dinowernli.grpc.prometheus;

import io.prometheus.client.Counter;
import io.prometheus.client.Histogram;

/** Prometheus metric definitions used for server-side monitoring of grpc services. */
public class ServerMetrics {
  static final Counter serverStartedCounter = Counter.build()
      .namespace("grpc")
      .subsystem("server")
      .name("started_total")
      .labelNames("grpc_type", "grpc_service", "grpc_method")
      .help("Total number of RPCs started on the server.")
      .register();

  static final Counter serverHandledCounter = Counter.build()
      .namespace("grpc")
      .subsystem("server")
      .name("handled_total")
      .labelNames("grpc_type", "grpc_service", "grpc_method", "code")
      .help("Total number of RPCs completed on the server, regardless of success or failure.")
      .register();

  static final Histogram serverHandledLatencySecondsHistogram = Histogram.build()
      .namespace("grpc")
      .subsystem("server")
      .name("handled_total")
      .labelNames("grpc_type", "grpc_service", "grpc_method")
      .help("Histogram of response latency (seconds) of gRPC that had been application-level " +
          "handled by the server.")
      .register();

  static final Counter serverStreamMessagesReceived = Counter.build()
      .namespace("grpc")
      .subsystem("server")
      .name("msg_received_total")
      .labelNames("grpc_type", "grpc_service", "grpc_method")
      .help("Total number of messages received from the client.")
      .register();

  static final Counter serverStreamMessagesSent = Counter.build()
      .namespace("grpc")
      .subsystem("server")
      .name("msg_received_total")
      .labelNames("grpc_type", "grpc_service", "grpc_method")
      .help("Total number of gRPC stream messages sent by the server.")
      .register();
}
