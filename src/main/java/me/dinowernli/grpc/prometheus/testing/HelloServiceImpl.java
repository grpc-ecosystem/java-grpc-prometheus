// Copyright 2016 Dino Wernli. All Rights Reserved. See LICENSE for licensing terms.

package me.dinowernli.grpc.prometheus.testing;

import com.github.dinowernli.proto.grpc.prometheus.HelloProto.HelloRequest;
import com.github.dinowernli.proto.grpc.prometheus.HelloProto.HelloResponse;
import com.github.dinowernli.proto.grpc.prometheus.HelloServiceGrpc.HelloServiceImplBase;
import io.grpc.stub.StreamObserver;

public class HelloServiceImpl extends HelloServiceImplBase {
  public static final String SERVICE_NAME =
      "com.github.dinowernli.proto.grpc.prometheus.HelloService";
  public static final String UNARY_METHOD_NAME = "SayHello";
  public static final String CLIENT_STREAM_METHOD_NAME = "SayHelloClientStream";
  public static final String SERVER_STREAM_METHOD_NAME = "SayHelloServerStream";
  public static final String BIDI_STREAM_METHOD_NAME = "SayHelloBidiStream";

  /**
   * Indicates how many request messages we wait for before responding to the client in the case of
   * the client streamed method.
   */
  private static final int CLIENT_STREAM_NUM_REQUESTS = 3;

  @Override
  public void sayHello(HelloRequest request, StreamObserver<HelloResponse> responseObserver) {
    responseObserver.onNext(
        HelloResponse.newBuilder().setMessage("Hello, " + request.getRecipient()).build());
    responseObserver.onCompleted();
  }

  @Override
  public void sayHelloServerStream(
      HelloRequest request, StreamObserver<HelloResponse> responseObserver) {
    responseObserver.onNext(responseForRequest(request));
    responseObserver.onCompleted();
  }

  @Override
  public StreamObserver<HelloRequest> sayHelloBidiStream(
      StreamObserver<HelloResponse> responseStream) {
    return new StreamObserver<HelloRequest>() {
      @Override
      public void onCompleted() {
        // When the client stops sending requests, we stop sending responses.
        responseStream.onCompleted();
      }

      @Override
      public void onError(Throwable t) {
        // Client error, propagate.
        responseStream.onError(t);
      }

      @Override
      public void onNext(HelloRequest request) {
        // Response to each request with exactly one response.
        responseStream.onNext(responseForRequest(request));
      }
    };
  }

  @Override
  public StreamObserver<HelloRequest> sayHelloClientStream(
      StreamObserver<HelloResponse> responseStream) {
    return new StreamObserver<HelloRequest>() {
      int numReceivedRequests = 0;

      @Override
      public void onCompleted() {
        // When the client stops sending requests, we stop sending responses.
        responseStream.onCompleted();
      }

      @Override
      public void onError(Throwable t) {
        // Client error, propagate.
        responseStream.onError(t);
      }

      @Override
      public void onNext(HelloRequest request) {
        ++numReceivedRequests;
        if (numReceivedRequests >= CLIENT_STREAM_NUM_REQUESTS) {
          responseStream.onNext(responseForRequest(request));
          responseStream.onCompleted();
        }
      }
    };
  }

  private HelloResponse responseForRequest(HelloRequest request) {
    if (request.getThrowException()) {
      throw new RuntimeException("I was told to throw!");
    }
    return HelloResponse.newBuilder().setMessage("Hello, " + request.getRecipient()).build();
  }
}
