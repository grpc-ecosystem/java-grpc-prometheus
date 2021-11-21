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

/**
 * Prometheus metric definitions used for server-side monitoring of grpc services.
 *
 * <p>Instances of this class hold the counters we increment for a specific pair of grpc service
 * definition and collection registry.
 */
class ServerMetrics {
  private static final List<String> defaultRequestLabels =
      Arrays.asList("grpc_type", "grpc_service", "grpc_method");

  private static final String STATUS_CODE_LABEL = "grpc_code";

  private static final List<String> defaultResponseLabels =
      Arrays.asList("grpc_type", "grpc_service", "grpc_method", "code", STATUS_CODE_LABEL);

  private static final Counter.Builder serverStartedBuilder =
      Counter.build()
          .namespace("grpc")
          .subsystem("server")
          .name("started")
          .help("Total number of RPCs started on the server.");

  private static final Counter.Builder serverHandledBuilder =
      Counter.build()
          .namespace("grpc")
          .subsystem("server")
          .name("handled")
          // TODO: The "code" label should be deprecated in a future major release. (See also below
          // in recordServerHandled().)
          .help("Total number of RPCs completed on the server, regardless of success or failure.");

  private static final Histogram.Builder serverHandledLatencySecondsBuilder =
      Histogram.build()
          .namespace("grpc")
          .subsystem("server")
          .name("handled_latency_seconds")
          .help(
              "Histogram of response latency (seconds) of gRPC that had been application-level "
                  + "handled by the server.");

  private static final Counter.Builder serverStreamMessagesReceivedBuilder =
      Counter.build()
          .namespace("grpc")
          .subsystem("server")
          .name("msg_received")
          .help("Total number of stream messages received from the client.");

  private static final Counter.Builder serverStreamMessagesSentBuilder =
      Counter.build()
          .namespace("grpc")
          .subsystem("server")
          .name("msg_sent")
          .help("Total number of stream messages sent by the server.");

  private final List<Key<String>> labelHeaderKeys;
  private final Counter serverStarted;
  private final Counter serverHandled;
  private final Counter serverStreamMessagesReceived;
  private final Counter serverStreamMessagesSent;
  private final Optional<Histogram> serverHandledLatencySeconds;
  private final boolean isAddCodeLabelToHistograms;

  private final GrpcMethod method;

  private ServerMetrics(
      List<Key<String>> labelHeaderKeys,
      GrpcMethod method,
      Counter serverStarted,
      Counter serverHandled,
      Counter serverStreamMessagesReceived,
      Counter serverStreamMessagesSent,
      Optional<Histogram> serverHandledLatencySeconds,
      boolean isAddCodeLabelToHistograms) {
    this.labelHeaderKeys = labelHeaderKeys;
    this.method = method;
    this.serverStarted = serverStarted;
    this.serverHandled = serverHandled;
    this.serverStreamMessagesReceived = serverStreamMessagesReceived;
    this.serverStreamMessagesSent = serverStreamMessagesSent;
    this.serverHandledLatencySeconds = serverHandledLatencySeconds;
    this.isAddCodeLabelToHistograms = isAddCodeLabelToHistograms;
  }

  public void recordCallStarted(Metadata metadata) {
    addLabels(serverStarted, customLabels(metadata, labelHeaderKeys), method).inc();
  }

  public void recordServerHandled(Code code, Metadata metadata) {
    // TODO: The "code" label should be deprecated in a future major release.
    List<String> allLabels = new ArrayList<>();
    allLabels.add(code.toString());
    allLabels.add(code.toString());
    allLabels.addAll(customLabels(metadata, labelHeaderKeys));
    addLabels(serverHandled, allLabels, method).inc();
  }

  public void recordStreamMessageSent(Metadata metadata) {
    addLabels(serverStreamMessagesSent, customLabels(metadata, labelHeaderKeys), method).inc();
  }

  public void recordStreamMessageReceived(Metadata metadata) {
    addLabels(serverStreamMessagesReceived, customLabels(metadata, labelHeaderKeys), method).inc();
  }

  /**
   * Only has any effect if monitoring is configured to include latency histograms. Otherwise, this
   * does nothing.
   */
  public void recordLatency(double latencySec, Metadata metadata, Code code) {
    if (!this.serverHandledLatencySeconds.isPresent()) {
      return;
    }

    final List<String> allLabels = new ArrayList<String>();
    allLabels.addAll(customLabels(metadata, labelHeaderKeys));
    if (isAddCodeLabelToHistograms) {
      allLabels.add(code.toString());
    }

    addLabels(this.serverHandledLatencySeconds.get(), allLabels, method).observe(latencySec);
  }

  /** Knows how to produce {@link ServerMetrics} instances for individual methods. */
  static class Factory {
    private final List<Key<String>> labelHeaderKeys;
    private final Counter serverStarted;
    private final Counter serverHandled;
    private final Counter serverStreamMessagesReceived;
    private final Counter serverStreamMessagesSent;
    private final Optional<Histogram> serverHandledLatencySeconds;
    private final boolean isAddCodeLabelToHistograms;

    Factory(Configuration configuration) {
      CollectorRegistry registry = configuration.getCollectorRegistry();
      this.labelHeaderKeys = metadataKeys(configuration.getLabelHeaders());
      this.serverStarted =
          serverStartedBuilder
              .labelNames(asArray(defaultRequestLabels, configuration.getSanitizedLabelHeaders()))
              .register(registry);
      this.serverHandled =
          serverHandledBuilder
              .labelNames(asArray(defaultResponseLabels, configuration.getSanitizedLabelHeaders()))
              .register(registry);
      this.serverStreamMessagesReceived =
          serverStreamMessagesReceivedBuilder
              .labelNames(asArray(defaultRequestLabels, configuration.getSanitizedLabelHeaders()))
              .register(registry);
      this.serverStreamMessagesSent =
          serverStreamMessagesSentBuilder
              .labelNames(asArray(defaultRequestLabels, configuration.getSanitizedLabelHeaders()))
              .register(registry);

      if (configuration.isIncludeLatencyHistograms()) {

        List<String> labels = new ArrayList<String>();
        labels.addAll(defaultRequestLabels);
        labels.addAll(configuration.getSanitizedLabelHeaders());

        if (configuration.isAddCodeLabelToHistograms()) {
          labels.add(STATUS_CODE_LABEL);
        }
        this.isAddCodeLabelToHistograms = configuration.isAddCodeLabelToHistograms();

        this.serverHandledLatencySeconds =
            Optional.of(
                serverHandledLatencySecondsBuilder
                    .buckets(configuration.getLatencyBuckets())
                    .labelNames(labels.toArray(new String[0]))
                    .register(registry));

      } else {
        this.serverHandledLatencySeconds = Optional.empty();
        this.isAddCodeLabelToHistograms = false;
      }
    }

    /** Creates a {@link ServerMetrics} for the supplied gRPC method. */
    ServerMetrics createMetricsForMethod(GrpcMethod grpcMethod) {
      return new ServerMetrics(
          labelHeaderKeys,
          grpcMethod,
          serverStarted,
          serverHandled,
          serverStreamMessagesReceived,
          serverStreamMessagesSent,
          serverHandledLatencySeconds,
          isAddCodeLabelToHistograms);
    }
  }
}
