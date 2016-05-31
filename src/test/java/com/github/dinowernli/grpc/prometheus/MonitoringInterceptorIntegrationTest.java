// Copyright 2016 Dino Wernli. All Rights Reserved. See LICENSE for licensing terms.

package com.github.dinowernli.grpc.prometheus;

import static org.junit.Assert.assertTrue;

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

import com.github.dinowernli.grpc.prometheus.MonitoringInterceptor.Configuration;
import com.github.dinowernli.grpc.prometheus.testing.HelloServiceImpl;
import com.google.common.base.Joiner;
import com.google.common.collect.ImmutableList;
import com.google.common.collect.Iterables;
import com.google.common.io.CharStreams;

import io.grpc.Server;
import io.grpc.ServerBuilder;
import io.grpc.ServerInterceptors;
import io.grpc.netty.NegotiationType;
import io.grpc.netty.NettyChannelBuilder;
import io.grpc.stub.StreamObserver;
import io.prometheus.client.exporter.MetricsServlet;
import polyglot.HelloProto;
import polyglot.HelloProto.HelloRequest;
import polyglot.HelloProto.HelloResponse;
import polyglot.HelloServiceGrpc;
import polyglot.HelloServiceGrpc.HelloServiceBlockingStub;
import polyglot.HelloServiceGrpc.HelloServiceStub;

/**
 * Integrations tests which make sure that if a service is started with a
 * {@link MonitoringInterceptor}, then all Prometheus metrics get recorded correctly.
 *
 * The basic structure of each test is:
 *   1) Start a Prometheus metrics servlet.
 *   2) Start a grpc server which implements an instrumented grpc service.
 *   3) Make some grpc requests to the server.
 *   4) Fetch the metrics served by the servlet and check that the right metrics were incremented.
 *
 * Note that each single test gets a completely fresh set of servers and metrics.
 */
public class MonitoringInterceptorIntegrationTest {
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
    // TODO(dino): Make sure that each test gets a fresh counter state.

    startMetricServer();
    startGrpcServer();
  }

  @After
  public void tearDown() throws Exception {
    grpcServer.shutdown().awaitTermination();
    metricsServer.stop();
  }

  @Test
  public void unaryRpcMetrics() throws Throwable {
    createGrpcBlockingStub().sayHello(REQUEST);

    ImmutableList<String> metricsLines = fetchMetricsPageLines();
    assertTrue(Iterables.any(metricsLines, (String line) -> {
      return containsAll(line, "grpc_server_msg_received_total", "UNARY", "HelloService", "1.0");
    }));
    assertTrue(Iterables.any(metricsLines, (String line) -> {
      return containsAll(line, "grpc_server_handled_total", "UNARY", "HelloService", "1.0");
    }));
  }

  @Test
  public void clientStreamRpcMetrics() throws Throwable {
    StreamObserver<HelloRequest> requestStream =
        createGrpcStub().sayHelloClientStream(ignoreResponse());
    requestStream.onNext(REQUEST);
    requestStream.onNext(REQUEST);
    requestStream.onNext(REQUEST);

    ImmutableList<String> metricsLines = fetchMetricsPageLines();
    assertTrue(formatLines(metricsLines), Iterables.any(metricsLines, (String line) -> {
      return containsAll(line, "grpc_server_msg_received_total", "CLIENT_STREAMING");
    }));
  }

  @Test
  public void doesNotAddHistogramsIfDisabled() throws Throwable {
    createGrpcBlockingStub().sayHello(REQUEST);

    // TODO(dino): Add plumbing for disabling histograms in the test.
  }

  private void startGrpcServer() {
    MonitoringInterceptor interceptor =
        MonitoringInterceptor.create(Configuration.cheapMetricsOnly());
    grpcServer = ServerBuilder.forPort(GRPC_PORT)
        .addService(ServerInterceptors.intercept(
            HelloServiceGrpc.bindService(new HelloServiceImpl()), interceptor))
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

  // TOOD(dino): Write a Truth matcher for an object which wraps metric lines.
  private static boolean containsAll(String subject, String... contents) {
    return ImmutableList.copyOf(contents).stream()
        .allMatch((String content) -> subject.contains(content));
  }


  private static HelloServiceBlockingStub createGrpcBlockingStub() {
    return HelloServiceGrpc.newBlockingStub(NettyChannelBuilder.forAddress("localhost", GRPC_PORT)
        .negotiationType(NegotiationType.PLAINTEXT)
        .build());
  }

  private static HelloServiceStub createGrpcStub() {
    return HelloServiceGrpc.newStub(NettyChannelBuilder.forAddress("localhost", GRPC_PORT)
        .negotiationType(NegotiationType.PLAINTEXT)
        .build());
  }

  private static String formatLines(Iterable<String> lines) {
    return ">>>" + Joiner.on("\n>>>").join(lines);
  }

  private static StreamObserver<HelloResponse> ignoreResponse() {
    return new StreamObserver<HelloProto.HelloResponse>() {
      @Override
      public void onCompleted() {
      }

      @Override
      public void onError(Throwable arg0) {
      }

      @Override
      public void onNext(HelloProto.HelloResponse arg0) {
      }
    };
  }
}
