// Copyright 2016 Dino Wernli. All Rights Reserved. See LICENSE for licensing terms.

package com.github.dinowernli.grpc.prometheus;

import static com.google.common.truth.Truth.assertThat;

import java.io.IOException;
import java.util.Enumeration;

import org.junit.After;
import org.junit.Before;
import org.junit.Test;

import com.github.dinowernli.grpc.prometheus.MonitoringServerInterceptor.Configuration;
import com.github.dinowernli.grpc.prometheus.testing.HelloServiceImpl;
import com.github.dinowernli.proto.grpc.prometheus.HelloProto.HelloRequest;
import com.github.dinowernli.proto.grpc.prometheus.HelloProto.HelloResponse;
import com.github.dinowernli.proto.grpc.prometheus.HelloServiceGrpc;
import com.github.dinowernli.proto.grpc.prometheus.HelloServiceGrpc.HelloServiceBlockingStub;
import com.github.dinowernli.proto.grpc.prometheus.HelloServiceGrpc.HelloServiceStub;
import com.google.common.collect.ImmutableList;

import io.grpc.Channel;
import io.grpc.Server;
import io.grpc.ServerBuilder;
import io.grpc.ServerInterceptors;
import io.grpc.netty.NegotiationType;
import io.grpc.netty.NettyChannelBuilder;
import io.grpc.stub.StreamObserver;
import io.grpc.testing.StreamRecorder;
import io.grpc.testing.TestUtils;
import io.prometheus.client.Collector.MetricFamilySamples;
import io.prometheus.client.CollectorRegistry;

/**
 * Integrations tests which make sure that if a service is started with a
 * {@link MonitoringServerInterceptor}, then all Prometheus metrics get recorded correctly.
 */
public class MonitoringInterceptorIntegrationTest {
  private static final String SERVICE_NAME =
      "com.github.dinowernli.proto.grpc.prometheus.HelloService";
  private static final String UNARY_METHOD_NAME = "SayHello";
  private static final String CLIENT_STREAM_METHOD_NAME = "SayHelloClientStream";
  private static final String SERVER_STREAM_METHOD_NAME = "SayHelloServerStream";
  private static final String BIDI_STREAM_METHOD_NAME = "SayHelloBidiStream";

  private static final String RECIPIENT = "Dave";
  private static final HelloRequest REQUEST = HelloRequest.newBuilder()
      .setRecipient(RECIPIENT)
      .build();

  private CollectorRegistry collectorRegistry;
  private Server grpcServer;
  private int grpcPort;

  @Before
  public void setUp() {
    collectorRegistry = new CollectorRegistry();
    startGrpcServer();
  }

  @After
  public void tearDown() throws Exception {
    grpcServer.shutdown().awaitTermination();
  }

  @Test
  public void unaryRpcMetrics() throws Throwable {
    createGrpcBlockingStub().sayHello(REQUEST);

    MetricFamilySamples handled = findRecordedMetric("grpc_server_handled_total");
    assertThat(handled.samples).hasSize(1);
    assertThat(handled.samples.get(0).labelValues).containsExactly(
        "UNARY", SERVICE_NAME, UNARY_METHOD_NAME, "OK");
    assertThat(handled.samples.get(0).value).isWithin(0).of(1);
  }

  @Test
  public void clientStreamRpcMetrics() throws Throwable {
    StreamRecorder<HelloResponse> streamRecorder = StreamRecorder.create();
    StreamObserver<HelloRequest> requestStream =
        createGrpcStub().sayHelloClientStream(streamRecorder);
    requestStream.onNext(REQUEST);
    requestStream.onNext(REQUEST);
    requestStream.onNext(REQUEST);

    // Not a blocking stub, so we need to wait.
    streamRecorder.awaitCompletion();

    MetricFamilySamples handled = findRecordedMetric("grpc_server_handled_total");
    assertThat(handled.samples).hasSize(1);
    assertThat(handled.samples.get(0).labelValues).containsExactly(
        "CLIENT_STREAMING", SERVICE_NAME, CLIENT_STREAM_METHOD_NAME, "OK");
    assertThat(handled.samples.get(0).value).isWithin(0).of(1);
  }

  @Test
  public void serverStreamRpcMetrics() throws Throwable {
    ImmutableList<HelloResponse> responses =
        ImmutableList.copyOf(createGrpcBlockingStub().sayHelloServerStream(REQUEST));

    MetricFamilySamples handled = findRecordedMetric("grpc_server_handled_total");
    assertThat(handled.samples).hasSize(1);
    assertThat(handled.samples.get(0).labelValues).containsExactly(
        "SERVER_STREAMING", SERVICE_NAME, SERVER_STREAM_METHOD_NAME, "OK");
    assertThat(handled.samples.get(0).value).isWithin(0).of(1);

    MetricFamilySamples messagesSent = findRecordedMetric("grpc_server_msg_sent_total");
    assertThat(messagesSent.samples.get(0).labelValues).containsExactly(
        "SERVER_STREAMING", SERVICE_NAME, SERVER_STREAM_METHOD_NAME);
    assertThat(messagesSent.samples.get(0).value).isWithin(0).of(responses.size());
  }

  @Test
  public void bidiStreamRpcMetrics() throws Throwable {
    StreamRecorder<HelloResponse> streamRecorder = StreamRecorder.create();
    StreamObserver<HelloRequest> requestStream =
        createGrpcStub().sayHelloBidiStream(streamRecorder);
    requestStream.onNext(REQUEST);
    requestStream.onNext(REQUEST);
    requestStream.onCompleted();

    // Not a blocking stub, so we need to wait.
    streamRecorder.awaitCompletion();

    MetricFamilySamples handled = findRecordedMetric("grpc_server_handled_total");
    assertThat(handled.samples).hasSize(1);
    assertThat(handled.samples.get(0).labelValues).containsExactly(
        "BIDI_STREAMING", SERVICE_NAME, BIDI_STREAM_METHOD_NAME, "OK");
    assertThat(handled.samples.get(0).value).isWithin(0).of(1);
  }

  @Test
  public void doesNotAddHistogramsIfDisabled() throws Throwable {
    createGrpcBlockingStub().sayHello(REQUEST);

    // TODO(dino): Add plumbing for disabling histograms in the test.
  }

  private void startGrpcServer() {
    MonitoringServerInterceptor interceptor = MonitoringServerInterceptor.create(
        Configuration.cheapMetricsOnly().withCollectorRegistry(collectorRegistry));
    grpcPort = TestUtils.pickUnusedPort();
    grpcServer = ServerBuilder.forPort(grpcPort)
        .addService(ServerInterceptors.intercept(
            HelloServiceGrpc.bindService(new HelloServiceImpl()), interceptor))
        .build();
    try {
      grpcServer.start();
    } catch (IOException e) {
      throw new RuntimeException("Exception while running grpc server", e);
    }
  }

  private MetricFamilySamples findRecordedMetric(String name) {
    Enumeration<MetricFamilySamples> samples = collectorRegistry.metricFamilySamples();
    while (samples.hasMoreElements()) {
      MetricFamilySamples sample = samples.nextElement();
      if (sample.name.equals(name)) {
        return sample;
      }
    }
    throw new IllegalArgumentException("Could not find metric with name: " + name);
  }

  private HelloServiceBlockingStub createGrpcBlockingStub() {
    return HelloServiceGrpc.newBlockingStub(createGrpcChannel());
  }

  private HelloServiceStub createGrpcStub() {
    return HelloServiceGrpc.newStub(createGrpcChannel());
  }

  private Channel createGrpcChannel() {
    return NettyChannelBuilder.forAddress("localhost", grpcPort)
        .negotiationType(NegotiationType.PLAINTEXT)
        .build();
  }
}
