// Copyright 2016 Dino Wernli. All Rights Reserved. See LICENSE for licensing terms.

package me.dinowernli.grpc.prometheus;

import static me.dinowernli.grpc.prometheus.Labels.addLabels;
import static me.dinowernli.grpc.prometheus.Labels.asArray;
import static me.dinowernli.grpc.prometheus.Labels.customLabels;
import static me.dinowernli.grpc.prometheus.Labels.metadataKeys;

import io.grpc.Metadata;
import io.grpc.Metadata.Key;
import io.grpc.Status.Code;
import io.prometheus.client.CollectorRegistry;
import io.prometheus.client.Counter;
import io.prometheus.client.Histogram;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;
import java.util.Optional;

/** Prometheus metric definitions used for client-side monitoring of grpc services. */
class ClientMetrics {
  private static final List<String> defaultRequestLabels =
      Arrays.asList("grpc_type", "grpc_service", "grpc_method");

  private static final List<String> defaultResponseLabels =
      Arrays.asList("grpc_type", "grpc_service", "grpc_method", "code", "grpc_code");

  private static final Counter.Builder rpcStartedBuilder =
      Counter.build()
          .namespace("grpc")
          .subsystem("client")
          .name("started")
          .help("Total number of RPCs started on the client.");

  private static final Counter.Builder rpcCompletedBuilder =
      Counter.build()
          .namespace("grpc")
          .subsystem("client")
          .name("completed")
          // TODO: The "code" label should be deprecated in a future major release. (See also below
          // in recordClientHandled().)
          .help("Total number of RPCs completed on the client, regardless of success or failure.");

  private static final Histogram.Builder completedLatencySecondsBuilder =
      Histogram.build()
          .namespace("grpc")
          .subsystem("client")
          .name("completed_latency_seconds")
          .help("Histogram of rpc response latency (in seconds) for completed rpcs.");

  private static final Counter.Builder streamMessagesReceivedBuilder =
      Counter.build()
          .namespace("grpc")
          .subsystem("client")
          .name("msg_received")
          .help("Total number of stream messages received from the server.");

  private static final Counter.Builder streamMessagesSentBuilder =
      Counter.build()
          .namespace("grpc")
          .subsystem("client")
          .name("msg_sent")
          .help("Total number of stream messages sent by the client.");

  private final List<Key<String>> labelHeaderKeys;
  private final Counter rpcStarted;
  private final Counter rpcCompleted;
  private final Counter streamMessagesReceived;
  private final Counter streamMessagesSent;
  private final Optional<Histogram> completedLatencySeconds;

  private final GrpcMethod method;

  private ClientMetrics(
      List<Key<String>> labelHeaderKeys,
      GrpcMethod method,
      Counter rpcStarted,
      Counter rpcCompleted,
      Counter streamMessagesReceived,
      Counter streamMessagesSent,
      Optional<Histogram> completedLatencySeconds) {
    this.labelHeaderKeys = labelHeaderKeys;
    this.method = method;
    this.rpcStarted = rpcStarted;
    this.rpcCompleted = rpcCompleted;
    this.streamMessagesReceived = streamMessagesReceived;
    this.streamMessagesSent = streamMessagesSent;
    this.completedLatencySeconds = completedLatencySeconds;
  }

  public void recordCallStarted(Metadata metadata) {
    addLabels(rpcStarted, customLabels(metadata, labelHeaderKeys), method).inc();
  }

  public void recordClientHandled(Code code, Metadata metadata) {
    // TODO: The "code" label should be deprecated in a future major release.
    List<String> allLabels = new ArrayList<>();
    allLabels.add(code.toString());
    allLabels.add(code.toString());
    allLabels.addAll(customLabels(metadata, labelHeaderKeys));
    addLabels(rpcCompleted, allLabels, method).inc();
  }

  public void recordStreamMessageSent(Metadata metadata) {
    addLabels(streamMessagesSent, customLabels(metadata, labelHeaderKeys), method).inc();
  }

  public void recordStreamMessageReceived(Metadata metadata) {
    addLabels(streamMessagesReceived, customLabels(metadata, labelHeaderKeys), method).inc();
  }

  /**
   * Only has any effect if monitoring is configured to include latency histograms. Otherwise, this
   * does nothing.
   */
  public void recordLatency(double latencySec, Metadata metadata) {
    if (!completedLatencySeconds.isPresent()) {
      return;
    }
    addLabels(completedLatencySeconds.get(), customLabels(metadata, labelHeaderKeys), method)
        .observe(latencySec);
  }

  /** Knows how to produce {@link ClientMetrics} instances for individual methods. */
  static class Factory {
    private final List<Key<String>> labelHeaderKeys;
    private final Counter rpcStarted;
    private final Counter rpcCompleted;
    private final Counter streamMessagesReceived;
    private final Counter streamMessagesSent;
    private final Optional<Histogram> completedLatencySeconds;

    Factory(Configuration configuration) {
      CollectorRegistry registry = configuration.getCollectorRegistry();
      this.labelHeaderKeys = metadataKeys(configuration.getLabelHeaders());
      this.rpcStarted =
          rpcStartedBuilder
              .labelNames(asArray(defaultRequestLabels, configuration.getSanitizedLabelHeaders()))
              .register(registry);
      this.rpcCompleted =
          rpcCompletedBuilder
              .labelNames(asArray(defaultResponseLabels, configuration.getSanitizedLabelHeaders()))
              .register(registry);
      this.streamMessagesReceived =
          streamMessagesReceivedBuilder
              .labelNames(asArray(defaultRequestLabels, configuration.getSanitizedLabelHeaders()))
              .register(registry);
      this.streamMessagesSent =
          streamMessagesSentBuilder
              .labelNames(asArray(defaultRequestLabels, configuration.getSanitizedLabelHeaders()))
              .register(registry);

      if (configuration.isIncludeLatencyHistograms()) {
        this.completedLatencySeconds =
            Optional.of(
                ClientMetrics.completedLatencySecondsBuilder
                    .buckets(configuration.getLatencyBuckets())
                    .labelNames(
                        asArray(defaultRequestLabels, configuration.getSanitizedLabelHeaders()))
                    .register(registry));
      } else {
        this.completedLatencySeconds = Optional.empty();
      }
    }

    /** Creates a {@link ClientMetrics} for the supplied gRPC method. */
    ClientMetrics createMetricsForMethod(GrpcMethod grpcMethod) {
      return new ClientMetrics(
          labelHeaderKeys,
          grpcMethod,
          rpcStarted,
          rpcCompleted,
          streamMessagesReceived,
          streamMessagesSent,
          completedLatencySeconds);
    }
  }
}
