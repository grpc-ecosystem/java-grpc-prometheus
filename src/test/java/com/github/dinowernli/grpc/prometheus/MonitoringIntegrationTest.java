// Copyright 2016 Dino Wernli. All Rights Reserved. See LICENSE for licensing terms.

package com.github.dinowernli.grpc.prometheus;

import static com.google.common.truth.Truth.assertThat;

import java.io.IOException;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.net.HttpURLConnection;
import java.net.MalformedURLException;
import java.net.URL;

import org.eclipse.jetty.servlet.ServletContextHandler;
import org.eclipse.jetty.servlet.ServletHolder;
import org.junit.After;
import org.junit.Before;
import org.junit.Test;

import com.github.dinowernli.grpc.prometheus.testing.HelloServiceImpl;
import com.google.common.collect.ImmutableList;
import com.google.common.io.CharStreams;

import io.grpc.Channel;
import io.grpc.Server;
import io.grpc.ServerBuilder;
import io.grpc.ServerInterceptors;
import io.grpc.netty.NegotiationType;
import io.grpc.netty.NettyChannelBuilder;
import io.prometheus.client.exporter.MetricsServlet;
import polyglot.HelloProto.HelloRequest;
import polyglot.HelloProto.HelloResponse;
import polyglot.HelloServiceGrpc;

public class MonitoringIntegrationTest {
  private static final String METRICS_SERVLET_ROOT = "/";
  private static final String METRICS_PATH = "/metrics";
  private static final int METRICS_SERVLET_PORT = 12346;
  private static final int GRPC_PORT = 12345;

  private static final String RECIPIENT = "Dave";
  private static final HelloRequest REQUEST = HelloRequest.newBuilder()
      .setRecipient(RECIPIENT)
      .build();

  private org.eclipse.jetty.server.Server metricsServer;
  private Server grpcServer;

  @Before
  public void setUp() {
    startMetricServer();
    startGrpcServer();
  }

  @After
  public void tearDown() throws Exception {
    grpcServer.shutdown().awaitTermination();
    metricsServer.stop();
  }

  @Test
  @Deprecated
  public void testSomething() throws Throwable {
    HelloResponse response =
        HelloServiceGrpc.newFutureStub(newGrpcChannel()).sayHello(REQUEST).get();
    assertThat(response.getMessage()).isEqualTo("Hello, " + RECIPIENT);
  }

  @Test
  public void testUnaryHappyCase() throws Throwable {
    HelloServiceGrpc.newFutureStub(newGrpcChannel()).sayHello(REQUEST).get();

    ImmutableList<String> metricsLines = fetchMetricsPageLines();
    assertThat(metricsLines.isEmpty()).isFalse();
  }

  private Channel newGrpcChannel() {
    return NettyChannelBuilder.forAddress("localhost", GRPC_PORT)
        .negotiationType(NegotiationType.PLAINTEXT)
        .build();
  }

  private void startGrpcServer() {
    MonitoringInterceptor monitoringInterceptor = MonitoringInterceptor.create();
    grpcServer = ServerBuilder.forPort(GRPC_PORT)
        .addService(ServerInterceptors.intercept(
            HelloServiceGrpc.bindService(new HelloServiceImpl()), monitoringInterceptor))
        .build();
    try {
      grpcServer.start();
    } catch (IOException e) {
      throw new RuntimeException("Exception while running grpc server", e);
    }
  }

  private void startMetricServer() {
    metricsServer = new org.eclipse.jetty.server.Server(METRICS_SERVLET_PORT);
    ServletContextHandler context = new ServletContextHandler();
    context.setContextPath(METRICS_SERVLET_ROOT);
    metricsServer.setHandler(context);
    context.addServlet(new ServletHolder(new MetricsServlet()), METRICS_PATH);
    try {
      metricsServer.start();
    } catch (Exception e) {
      throw new RuntimeException("Exception while running metrics server", e);
    }
  }

  private static ImmutableList<String> fetchMetricsPageLines() {
    try {
      URL metricsUrl = createMetricsPageUrl();
      HttpURLConnection connection = (HttpURLConnection) metricsUrl.openConnection();
      connection.setRequestMethod("GET");
      connection.setUseCaches(false);
      connection.setDoOutput(true);
      connection.getOutputStream().close();

      InputStream inputStream = connection.getInputStream();
      return ImmutableList.copyOf(CharStreams.readLines(new InputStreamReader(inputStream)));
    } catch (IOException e) {
      throw new RuntimeException("Unable to create url", e);
    }
  }

  private static URL createMetricsPageUrl() {
    try {
      return new URL("http://localhost:" + METRICS_SERVLET_PORT + METRICS_PATH);
    } catch (MalformedURLException e) {
      throw new RuntimeException("Could not create URL to metrics servlet", e);
    }
  }
}
