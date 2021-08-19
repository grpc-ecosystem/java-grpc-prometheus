package me.dinowernli.grpc.prometheus.integration;

import com.github.dinowernli.proto.grpc.prometheus.HelloProto;
import com.github.dinowernli.proto.grpc.prometheus.HelloProto.HelloResponse;
import com.github.dinowernli.proto.grpc.prometheus.HelloServiceGrpc;
import com.github.dinowernli.proto.grpc.prometheus.HelloServiceGrpc.HelloServiceStub;
import io.grpc.Server;
import io.grpc.inprocess.InProcessChannelBuilder;
import io.grpc.inprocess.InProcessServerBuilder;
import io.grpc.stub.StreamObserver;
import io.grpc.testing.StreamRecorder;
import io.prometheus.client.Collector;
import io.prometheus.client.CollectorRegistry;
import me.dinowernli.grpc.prometheus.Configuration;
import me.dinowernli.grpc.prometheus.MonitoringClientInterceptor;
import me.dinowernli.grpc.prometheus.testing.HelloServiceImpl;
import me.dinowernli.grpc.prometheus.testing.RegistryHelper;
import org.junit.After;
import org.junit.Before;
import org.junit.Test;

import java.io.IOException;

import static com.google.common.truth.Truth.assertThat;

/** Integration tests for the client-side monitoring pipeline. */
public class MonitoringClientInterceptorIntegrationTest {
  private static final String grpcServerName = "grpc-server";
  private static final Configuration CHEAP_METRICS = Configuration.cheapMetricsOnly();
  private static final Configuration ALL_METRICS = Configuration.allMetrics();

  private static final String RECIPIENT = "Jane";
  private static final HelloProto.HelloRequest REQUEST = HelloProto.HelloRequest.newBuilder()
      .setRecipient(RECIPIENT)
      .build();

  private Server grpcServer;
  private CollectorRegistry collectorRegistry;
  private StreamRecorder<HelloResponse> responseRecorder;

  @Before
  public void setUp() {
    responseRecorder = StreamRecorder.create();
    collectorRegistry = new CollectorRegistry();
    startServer();
  }

  @After
  public void tearDown() throws Throwable {
    grpcServer.shutdown().awaitTermination();
  }

  @Test
  public void unaryRpcMetrics() throws Throwable {
    createClientStub(CHEAP_METRICS).sayHello(REQUEST, responseRecorder);
    assertThat(
            findRecordedMetricOrThrow("grpc_client_started").samples.stream()
                    .filter(s -> s.name.equals("grpc_client_started_total"))
                    .findFirst().get().value)
            .isWithin(0).of(1);

    assertThat(findRecordedMetricOrThrow("grpc_client_msg_received").samples).isEmpty();
    assertThat(findRecordedMetricOrThrow("grpc_client_msg_sent").samples).isEmpty();

    responseRecorder.awaitCompletion();

    Collector.MetricFamilySamples handled = findRecordedMetricOrThrow("grpc_client_completed");
    assertThat(handled.samples).hasSize(2);
    Collector.MetricFamilySamples.Sample totalSample =
            handled.samples.stream().filter(s -> s.name.equals("grpc_client_completed_total")).findFirst().get();
    assertThat(totalSample.labelValues).containsExactly(
            "UNARY", HelloServiceImpl.SERVICE_NAME, HelloServiceImpl.UNARY_METHOD_NAME,
            "OK", "OK"); // TODO: These are the "code" and "grpc_code" labels which are currently duplicated. "code" should be deprecated in a future release.
    assertThat(totalSample.value).isWithin(0).of(1);
  }

  @Test
  public void clientStreamRpcMetrics() throws Throwable {
    StreamObserver<HelloProto.HelloRequest> requestStream =
            createClientStub(CHEAP_METRICS).sayHelloClientStream(responseRecorder);
    requestStream.onNext(REQUEST);
    requestStream.onNext(REQUEST);

    assertThat(
            findRecordedMetricOrThrow("grpc_client_started").samples.stream()
                    .filter(s -> s.name.equals("grpc_client_started_total"))
                    .findFirst().get().value)
            .isWithin(0).of(1);

    // The "sent" metric should get incremented even if the rpc hasn't terminated.
    assertThat(
            findRecordedMetricOrThrow("grpc_client_msg_sent").samples.stream()
                    .filter(s -> s.name.equals("grpc_client_msg_sent_total"))
                    .findFirst().get().value)
            .isWithin(0).of(2);

    // Last request, should trigger the response.
    requestStream.onNext(REQUEST);
    responseRecorder.awaitCompletion();

    // The received counter only considers stream messages.
    assertThat(findRecordedMetricOrThrow("grpc_client_msg_received").samples).isEmpty();

    assertThat(
            findRecordedMetricOrThrow("grpc_client_msg_sent").samples.stream()
                    .filter(s -> s.name.equals("grpc_client_msg_sent_total"))
                    .findFirst().get().value)
            .isWithin(0).of(3);

    Collector.MetricFamilySamples handled = findRecordedMetricOrThrow("grpc_client_completed");
    assertThat(handled.samples).hasSize(2);
    Collector.MetricFamilySamples.Sample totalSample =
            handled.samples.stream().filter(s -> s.name.equals("grpc_client_completed_total")).findFirst().get();
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
    createClientStub(CHEAP_METRICS).sayHelloServerStream(REQUEST, responseRecorder);
    responseRecorder.awaitCompletion();

    assertThat(
            findRecordedMetricOrThrow("grpc_client_started").samples.stream()
                    .filter(s -> s.name.equals("grpc_client_started_total"))
                    .findFirst().get().value)
            .isWithin(0).of(1);

    assertThat(
            findRecordedMetricOrThrow("grpc_client_msg_received").samples.stream()
                    .filter(s -> s.name.equals("grpc_client_msg_received_total"))
                    .findFirst().get().value)
            .isWithin(0).of(1);

    assertThat(findRecordedMetricOrThrow("grpc_client_msg_sent").samples).isEmpty();

    Collector.MetricFamilySamples handled = findRecordedMetricOrThrow("grpc_client_completed");
    assertThat(handled.samples).hasSize(2);
    Collector.MetricFamilySamples.Sample totalSample =
            handled.samples.stream().filter(s -> s.name.equals("grpc_client_completed_total")).findFirst().get();
    assertThat(totalSample.labelValues).containsExactly(
            "SERVER_STREAMING",
            HelloServiceImpl.SERVICE_NAME,
            HelloServiceImpl.SERVER_STREAM_METHOD_NAME,
            "OK", // TODO: These are the "code" and "grpc_code" labels which are currently duplicated. "code" should be deprecated in a future release.
            "OK");
    assertThat(totalSample.value).isWithin(0).of(1);
  }

  @Test
  public void bidiStreamRpcMetrics() throws Throwable {
    StreamObserver<HelloProto.HelloRequest> requestStream =
            createClientStub(CHEAP_METRICS).sayHelloBidiStream(responseRecorder);
    requestStream.onNext(REQUEST);
    requestStream.onNext(REQUEST);
    requestStream.onCompleted();

    responseRecorder.awaitCompletion();

    assertThat(
            findRecordedMetricOrThrow("grpc_client_started").samples.stream()
                    .filter(s -> s.name.equals("grpc_client_started_total"))
                    .findFirst().get().value)
            .isWithin(0).of(1);

    assertThat(
            findRecordedMetricOrThrow("grpc_client_msg_received").samples.stream()
                    .filter(s -> s.name.equals("grpc_client_msg_received_total"))
                    .findFirst().get().value)
            .isWithin(0).of(2);

    assertThat(
            findRecordedMetricOrThrow("grpc_client_msg_sent").samples.stream()
                    .filter(s -> s.name.equals("grpc_client_msg_sent_total"))
                    .findFirst().get().value)
            .isWithin(0).of(2);

    Collector.MetricFamilySamples handled = findRecordedMetricOrThrow("grpc_client_completed");
    assertThat(handled.samples).hasSize(2);
    Collector.MetricFamilySamples.Sample totalSample =
            handled.samples.stream().filter(s -> s.name.equals("grpc_client_completed_total")).findFirst().get();
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
    createClientStub(CHEAP_METRICS).sayHello(
            HelloProto.HelloRequest.getDefaultInstance(), responseRecorder);
    responseRecorder.awaitCompletion();
    assertThat(RegistryHelper.findRecordedMetric(
            "grpc_client completed_latency_seconds", collectorRegistry).isPresent()).isFalse();
  }

  @Test
  public void addsHistogramIfEnabled() throws Throwable {
    createClientStub(ALL_METRICS).sayHello(
            HelloProto.HelloRequest.getDefaultInstance(), responseRecorder);
    responseRecorder.awaitCompletion();
    Collector.MetricFamilySamples latency =
            findRecordedMetricOrThrow("grpc_client_completed_latency_seconds");
    assertThat(latency.samples.size()).isGreaterThan(0);
  }

  @Test
  public void overridesHistogramBuckets() throws Throwable {
    double[] buckets = new double[] {0.1, 0.2};
    createClientStub(ALL_METRICS.withLatencyBuckets(buckets)).sayHello(
            HelloProto.HelloRequest.getDefaultInstance(), responseRecorder);
    responseRecorder.awaitCompletion();

    long expectedNum = buckets.length + 1;  // Our two buckets and the Inf buckets.
    assertThat(countSamples(
            "grpc_client_completed_latency_seconds",
            "grpc_client_completed_latency_seconds_bucket")).isEqualTo(expectedNum);
  }


  private HelloServiceStub createClientStub(Configuration configuration) {
    return HelloServiceGrpc.newStub(InProcessChannelBuilder.forName(grpcServerName)
        .usePlaintext()
        .intercept(MonitoringClientInterceptor.create(
            configuration.withCollectorRegistry(collectorRegistry)))
        .build());
  }

  private void startServer() {
    grpcServer = InProcessServerBuilder.forName(grpcServerName)
        .addService(new HelloServiceImpl().bindService())
        .build();
    try {
      grpcServer.start();
    } catch (IOException e) {
      throw new RuntimeException("Exception while running grpc server", e);
    }
  }

  private double extractMetricValue(String name) {
    return RegistryHelper.extractMetricValue(name, collectorRegistry);
  }

  private Collector.MetricFamilySamples findRecordedMetricOrThrow(String name) {
    return RegistryHelper.findRecordedMetricOrThrow(name, collectorRegistry);
  }

  private int countSamples(String metricName, String sampleName) {
    return RegistryHelper.countSamples(metricName, sampleName, collectorRegistry);
  }
}
