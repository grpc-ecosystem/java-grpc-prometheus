// Copyright 2016 Dino Wernli. All Rights Reserved. See LICENSE for licensing terms.

package com.github.dinowernli.grpc.prometheus.testing;

import io.grpc.stub.StreamObserver;
import polyglot.HelloProto.HelloRequest;
import polyglot.HelloProto.HelloResponse;
import polyglot.HelloServiceGrpc.HelloService;

public class HelloServiceImpl implements HelloService {
  @Override
  public void sayHello(HelloRequest request, StreamObserver<HelloResponse> responseObserver) {
    responseObserver.onNext(HelloResponse.newBuilder()
       .setMessage("Hello, " + request.getRecipient())
       .build());
    responseObserver.onCompleted();
  }

  @Override
  public void sayHelloStream(HelloRequest request, StreamObserver<HelloResponse> responseObserver) {
    responseObserver.onNext(HelloResponse.newBuilder()
        .setMessage("Hello, " + request.getRecipient())
        .build());
    responseObserver.onCompleted();
  }
}
