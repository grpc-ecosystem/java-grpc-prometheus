// Copyright 2016 Dino Wernli. All Rights Reserved. See LICENSE for licensing terms.

package com.github.dinowernli.grpc.prometheus;

import static com.google.common.truth.Truth.assertThat;

import java.io.IOException;
import java.util.Enumeration;

import org.junit.After;
import org.junit.Before;
import org.junit.Test;

import com.github.dinowernli.grpc.prometheus.MonitoringInterceptor.Configuration;
import com.github.dinowernli.grpc.prometheus.testing.HelloServiceImpl;

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
import polyglot.HelloProto.HelloRequest;
import polyglot.HelloProto.HelloResponse;
import polyglot.HelloServiceGrpc;
import polyglot.HelloServiceGrpc.HelloServiceBlockingStub;
import polyglot.HelloServiceGrpc.HelloServiceStub;

/**
 * Integrations tests which make sure that if a service is started with a
 * {@link MonitoringInterceptor}, then all Prometheus metrics get recorded correctly.
 */
public class MonitoringInterceptorIntegrationTest {
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

    // TODO(dino): Rename the service from Polyglot to something else.

    MetricFamilySamples handled = findRecordedMetric("grpc_server_handled_total");
    assertThat(handled.samples).hasSize(1);
    assertThat(handled.samples.get(0).labelValues).containsExactly(
        "UNARY", "polyglot.HelloService", "polyglot.HelloService/SayHello", "OK");
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
        "CLIENT_STREAMING",
        "polyglot.HelloService",
        "polyglot.HelloService/SayHelloClientStream",
        "OK");
    assertThat(handled.samples.get(0).value).isWithin(0).of(1);
  }

  @Test
  public void doesNotAddHistogramsIfDisabled() throws Throwable {
    createGrpcBlockingStub().sayHello(REQUEST);

    // TODO(dino): Add plumbing for disabling histograms in the test.
  }

  private void startGrpcServer() {
    MonitoringInterceptor interceptor = MonitoringInterceptor.create(
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
