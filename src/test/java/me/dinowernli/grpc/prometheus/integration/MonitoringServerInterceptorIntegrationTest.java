// Copyright 2016 Dino Wernli. All Rights Reserved. See LICENSE for licensing terms.

package me.dinowernli.grpc.prometheus.integration;

import com.github.dinowernli.proto.grpc.prometheus.HelloProto.HelloRequest;
import com.github.dinowernli.proto.grpc.prometheus.HelloProto.HelloResponse;
import com.github.dinowernli.proto.grpc.prometheus.HelloServiceGrpc;
import com.github.dinowernli.proto.grpc.prometheus.HelloServiceGrpc.HelloServiceBlockingStub;
import com.github.dinowernli.proto.grpc.prometheus.HelloServiceGrpc.HelloServiceStub;
import com.google.common.collect.ImmutableList;
import io.grpc.Channel;
import io.grpc.Server;
import io.grpc.ServerInterceptors;
import io.grpc.inprocess.InProcessChannelBuilder;
import io.grpc.inprocess.InProcessServerBuilder;
import io.grpc.stub.StreamObserver;
import io.grpc.testing.StreamRecorder;
import io.prometheus.client.Collector.MetricFamilySamples;
import io.prometheus.client.CollectorRegistry;
import me.dinowernli.grpc.prometheus.Configuration;
import me.dinowernli.grpc.prometheus.MonitoringServerInterceptor;
import me.dinowernli.grpc.prometheus.testing.HelloServiceImpl;
import me.dinowernli.grpc.prometheus.testing.RegistryHelper;
import org.junit.After;
import org.junit.Before;
import org.junit.Test;

import java.io.IOException;
import java.util.List;
import java.util.stream.Collectors;

import static com.google.common.truth.Truth.assertThat;

/**
 * Integrations tests which make sure that if a service is started with a
 * {@link MonitoringServerInterceptor}, then all Prometheus metrics get recorded correctly.
 */
public class MonitoringServerInterceptorIntegrationTest {
  private static final String grpcServerName = "grpc-server";
  private static final String RECIPIENT = "Dave";
  private static final HelloRequest REQUEST = HelloRequest.newBuilder()
      .setRecipient(RECIPIENT)
      .build();

  private static final Configuration CHEAP_METRICS = Configuration.cheapMetricsOnly();
  private static final Configuration ALL_METRICS = Configuration.allMetrics();

  private CollectorRegistry collectorRegistry;
  private Server grpcServer;

  @Before
  public void setUp() {
    collectorRegistry = new CollectorRegistry();
  }

  @After
  public void tearDown() throws Exception {
    grpcServer.shutdown().awaitTermination();
  }

  @Test
  public void unaryRpcMetrics() throws Throwable {
    startGrpcServer(CHEAP_METRICS);
    createGrpcBlockingStub().sayHello(REQUEST);

    assertThat(findRecordedMetricOrThrow("grpc_server_started").samples).hasSize(2);
    assertThat(findRecordedMetricNamesOrThrow("grpc_server_started")).contains("grpc_server_started_total");
    assertThat(findRecordedMetricNamesOrThrow("grpc_server_started")).contains("grpc_server_started_created");

    assertThat(findRecordedMetricOrThrow("grpc_server_msg_received").samples).isEmpty();
    assertThat(findRecordedMetricOrThrow("grpc_server_msg_sent").samples).isEmpty();

    MetricFamilySamples handled = findRecordedMetricOrThrow("grpc_server_handled");
    assertThat(handled.samples).hasSize(2);
    MetricFamilySamples.Sample totalSample =
            handled.samples.stream().filter(s -> s.name.equals("grpc_server_handled_total")).findFirst().get();
    assertThat(totalSample.labelValues).containsExactly(
            "UNARY", HelloServiceImpl.SERVICE_NAME, HelloServiceImpl.UNARY_METHOD_NAME,
            "OK", "OK"); // TODO: These are the "code" and "grpc_code" labels which are currently duplicated. "code" should be deprecated in a future release.
    assertThat(totalSample.value).isWithin(0).of(1);
  }

  @Test
  public void clientStreamRpcMetrics() throws Throwable {
    startGrpcServer(CHEAP_METRICS);
    StreamRecorder<HelloResponse> streamRecorder = StreamRecorder.create();
    StreamObserver<HelloRequest> requestStream =
            createGrpcStub().sayHelloClientStream(streamRecorder);
    requestStream.onNext(REQUEST);
    requestStream.onNext(REQUEST);
    requestStream.onNext(REQUEST);

    // Not a blocking stub, so we need to wait.
    streamRecorder.awaitCompletion();

    assertThat(findRecordedMetricOrThrow("grpc_server_started").samples).hasSize(2);
    assertThat(findRecordedMetricNamesOrThrow("grpc_server_started")).contains("grpc_server_started_total");
    assertThat(findRecordedMetricNamesOrThrow("grpc_server_started")).contains("grpc_server_started_created");

    assertThat(findRecordedMetricOrThrow("grpc_server_msg_received").samples).hasSize(2);
    assertThat(findRecordedMetricNamesOrThrow("grpc_server_msg_received")).contains("grpc_server_msg_received_total");
    assertThat(findRecordedMetricNamesOrThrow("grpc_server_msg_received")).contains("grpc_server_msg_received_created");

    assertThat(findRecordedMetricOrThrow("grpc_server_msg_sent").samples).isEmpty();

    MetricFamilySamples handled = findRecordedMetricOrThrow("grpc_server_handled");
    assertThat(handled.samples).hasSize(2);
    MetricFamilySamples.Sample totalSample =
            handled.samples.stream().filter(s -> s.name.equals("grpc_server_handled_total")).findFirst().get();
    assertThat(totalSample.labelValues).containsExactly(
            "CLIENT_STREAMING",
            HelloServiceImpl.SERVICE_NAME,
            HelloServiceImpl.CLIENT_STREAM_METHOD_NAME,
            "OK", // TODO: These are the "code" and "grpc_code" labels which are currently duplicated. "code" should be deprecated in a future release.
            "OK");
    assertThat(totalSample.value).isWithin(0).of(1);
  }

  @Test
  public void serverStreamRpcMetrics() throws Throwable {
    startGrpcServer(CHEAP_METRICS);
    ImmutableList<HelloResponse> responses =
            ImmutableList.copyOf(createGrpcBlockingStub().sayHelloServerStream(REQUEST));

    assertThat(findRecordedMetricOrThrow("grpc_server_started").samples).hasSize(2);
    assertThat(findRecordedMetricNamesOrThrow("grpc_server_started")).contains("grpc_server_started_total");
    assertThat(findRecordedMetricNamesOrThrow("grpc_server_started")).contains("grpc_server_started_created");

    assertThat(findRecordedMetricOrThrow("grpc_server_msg_received").samples).isEmpty();

    assertThat(findRecordedMetricOrThrow("grpc_server_msg_sent").samples).hasSize(2);
    assertThat(findRecordedMetricNamesOrThrow("grpc_server_msg_sent")).contains("grpc_server_msg_sent_total");
    assertThat(findRecordedMetricNamesOrThrow("grpc_server_msg_sent")).contains("grpc_server_msg_sent_created");

    MetricFamilySamples handled = findRecordedMetricOrThrow("grpc_server_handled");
    assertThat(handled.samples).hasSize(2);
    MetricFamilySamples.Sample totalSample =
            handled.samples.stream().filter(s -> s.name.equals("grpc_server_handled_total")).findFirst().get();
    assertThat(totalSample.labelValues).containsExactly(
            "SERVER_STREAMING",
            HelloServiceImpl.SERVICE_NAME,
            HelloServiceImpl.SERVER_STREAM_METHOD_NAME,
            "OK", // TODO: These are the "code" and "grpc_code" labels which are currently duplicated. "code" should be deprecated in a future release.
            "OK");
    assertThat(totalSample.value).isWithin(0).of(1);

    MetricFamilySamples messagesSent = findRecordedMetricOrThrow("grpc_server_msg_sent");
    totalSample = messagesSent.samples.stream().filter(s -> s.name.equals("grpc_server_msg_sent_total")).findFirst().get();
    assertThat(totalSample.labelValues).containsExactly(
            "SERVER_STREAMING",
            HelloServiceImpl.SERVICE_NAME,
            HelloServiceImpl.SERVER_STREAM_METHOD_NAME);
    assertThat(totalSample.value).isWithin(0).of(responses.size());
  }

  @Test
  public void bidiStreamRpcMetrics() throws Throwable {
    startGrpcServer(CHEAP_METRICS);
    StreamRecorder<HelloResponse> streamRecorder = StreamRecorder.create();
    StreamObserver<HelloRequest> requestStream =
            createGrpcStub().sayHelloBidiStream(streamRecorder);
    requestStream.onNext(REQUEST);
    requestStream.onNext(REQUEST);
    requestStream.onCompleted();

    // Not a blocking stub, so we need to wait.
    streamRecorder.awaitCompletion();

    assertThat(findRecordedMetricOrThrow("grpc_server_started").samples).hasSize(2);
    assertThat(findRecordedMetricNamesOrThrow("grpc_server_started")).contains("grpc_server_started_total");
    assertThat(findRecordedMetricNamesOrThrow("grpc_server_started")).contains("grpc_server_started_created");

    assertThat(findRecordedMetricOrThrow("grpc_server_msg_received").samples).hasSize(2);
    assertThat(findRecordedMetricNamesOrThrow("grpc_server_msg_received")).contains("grpc_server_msg_received_total");
    assertThat(findRecordedMetricNamesOrThrow("grpc_server_msg_received")).contains("grpc_server_msg_received_created");

    assertThat(findRecordedMetricOrThrow("grpc_server_msg_sent").samples).hasSize(2);
    assertThat(findRecordedMetricNamesOrThrow("grpc_server_msg_sent")).contains("grpc_server_msg_sent_total");
    assertThat(findRecordedMetricNamesOrThrow("grpc_server_msg_sent")).contains("grpc_server_msg_sent_created");

    MetricFamilySamples handled = findRecordedMetricOrThrow("grpc_server_handled");
    assertThat(handled.samples).hasSize(2);
    MetricFamilySamples.Sample totalSample =
            handled.samples.stream().filter(s -> s.name.equals("grpc_server_handled_total")).findFirst().get();
    assertThat(totalSample.labelValues).containsExactly(
            "BIDI_STREAMING",
            HelloServiceImpl.SERVICE_NAME,
            HelloServiceImpl.BIDI_STREAM_METHOD_NAME,
            "OK", // TODO: These are the "code" and "grpc_code" labels which are currently duplicated. "code" should be deprecated in a future release.
            "OK");
    assertThat(totalSample.value).isWithin(0).of(1);
  }

  @Test
  public void noHistogramIfDisabled() throws Throwable {
    startGrpcServer(CHEAP_METRICS);
    createGrpcBlockingStub().sayHello(REQUEST);
    assertThat(RegistryHelper.findRecordedMetric(
            "grpc_server_handled_latency_seconds", collectorRegistry).isPresent()).isFalse();
  }

  @Test
  public void addsHistogramIfEnabled() throws Throwable {
    startGrpcServer(ALL_METRICS);
    createGrpcBlockingStub().sayHello(REQUEST);

    MetricFamilySamples latency = findRecordedMetricOrThrow("grpc_server_handled_latency_seconds");
    assertThat(latency.samples.size()).isGreaterThan(0);
  }

  @Test
  public void overridesHistogramBuckets() throws Throwable {
    double[] buckets = new double[] {0.1, 0.2, 0.8};
    startGrpcServer(ALL_METRICS.withLatencyBuckets(buckets));
    createGrpcBlockingStub().sayHello(REQUEST);

    long expectedNum = buckets.length + 1;  // Our two buckets and the Inf buckets.
    assertThat(countSamples(
            "grpc_server_handled_latency_seconds",
            "grpc_server_handled_latency_seconds_bucket")).isEqualTo(expectedNum);
  }

  @Test
  public void recordsMultipleCalls() throws Throwable {
    startGrpcServer(CHEAP_METRICS);

    createGrpcBlockingStub().sayHello(REQUEST);
    createGrpcBlockingStub().sayHello(REQUEST);
    createGrpcBlockingStub().sayHello(REQUEST);

    StreamRecorder<HelloResponse> streamRecorder = StreamRecorder.create();
    StreamObserver<HelloRequest> requestStream =
            createGrpcStub().sayHelloBidiStream(streamRecorder);
    requestStream.onNext(REQUEST);
    requestStream.onNext(REQUEST);
    requestStream.onCompleted();
    streamRecorder.awaitCompletion();

    assertThat(findRecordedMetricOrThrow("grpc_server_started").samples).hasSize(4);
    assertThat(findRecordedMetricOrThrow("grpc_server_handled").samples).hasSize(4);
  }

  private void startGrpcServer(Configuration monitoringConfig) {
    MonitoringServerInterceptor interceptor = MonitoringServerInterceptor.create(
        monitoringConfig.withCollectorRegistry(collectorRegistry));
    grpcServer = InProcessServerBuilder.forName(grpcServerName)
        .addService(ServerInterceptors.intercept(new HelloServiceImpl().bindService(), interceptor))
        .build();
    try {
      grpcServer.start();
    } catch (IOException e) {
      throw new RuntimeException("Exception while running grpc server", e);
    }
  }

  private MetricFamilySamples findRecordedMetricOrThrow(String name) {
    return RegistryHelper.findRecordedMetricOrThrow(name, collectorRegistry);
  }

  private List<String> findRecordedMetricNamesOrThrow(String name) {
    return RegistryHelper.findRecordedMetricNamesOrThrow(name, collectorRegistry);
  }

  private HelloServiceBlockingStub createGrpcBlockingStub() {
    return HelloServiceGrpc.newBlockingStub(createGrpcChannel());
  }

  private int countSamples(String metricName, String sampleName) {
    return RegistryHelper.countSamples(metricName, sampleName, collectorRegistry);
  }

  private HelloServiceStub createGrpcStub() {
    return HelloServiceGrpc.newStub(createGrpcChannel());
  }

  private Channel createGrpcChannel() {
    return InProcessChannelBuilder.forName(grpcServerName)
        .usePlaintext()
        .build();
  }
}
