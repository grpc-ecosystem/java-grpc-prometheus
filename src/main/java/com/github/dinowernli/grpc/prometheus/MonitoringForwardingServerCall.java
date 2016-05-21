package com.github.dinowernli.grpc.prometheus;

import java.time.Clock;
import java.time.Instant;
import java.util.Optional;

import io.grpc.ForwardingServerCall;
import io.grpc.Metadata;
import io.grpc.ServerCall;
import io.grpc.Status;
import io.prometheus.client.Counter;
import io.prometheus.client.Gauge;

/**
 * A {@link ForwardingServerCall} which keeps track of relevant stats for a single call and
 * reports them to Prometheus.
 */
class MonitoringForwardingServerCall<S> extends ForwardingServerCall.SimpleForwardingServerCall<S> {
  private static final Counter requests = Counter.build()
      .name("request_status")
      .labelNames("method", "status")
      .help("Counts the number of requests and the response status we served")
      .register();
  private static final Gauge requestLatencyMs = Gauge.build()
      .name("request_latency_ms")
      .labelNames("method")
      .help("Records the latency in milliseconds of a request")
      .register();

  private final Clock clock;
  private final String methodName;

  private final Optional<Instant> startInstant;

  MonitoringForwardingServerCall(ServerCall<S> call, Clock clock, String methodName) {
    super(call);
    this.clock = clock;
    this.methodName = methodName;
    this.startInstant = Optional.of(clock.instant());
  }

  @Override
  public void close(Status status, Metadata responseHeaders) {
    reportMetrics(status);
    super.close(status, responseHeaders);
  }

  private void reportMetrics(Status status) {
    long latencyMs = clock.millis() - startInstant.get().toEpochMilli();
    requests.labels(methodName, status.toString()).inc();
    requestLatencyMs.labels(methodName).set(latencyMs);
  }
}