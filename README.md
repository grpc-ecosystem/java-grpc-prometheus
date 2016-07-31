# java-grpc-prometheus

[![Build Status](https://travis-ci.org/dinowernli/java-grpc-prometheus.svg?branch=master)](https://travis-ci.org/dinowernli/java-grpc-prometheus)

Java interceptors which can be used to monitor Grpc services using Prometheus.

## Features

The features of this library include two monitoring grpc interceptors, `MonitoringServerInterceptor` and `MonitoringClientInterceptor`. These interceptors can be attached separately to grpc servers and client stubs respectively. For each RPC, the interceptors increment the following Prometheus metrics, broken down by method type, service name, method name, and response code:

* Server
    * `grpc_server_started_total`: Total number of RPCs started on the server.
    * `grpc_server_handled_total`: Total number of RPCs completed on the server, regardless of success or failure.
    * `grpc_server_handled_latency_seconds`: (Optional) Histogram of response latency of rpcs handled by the server, in seconds.
    * `grpc_server_msg_received_total`: Total number of stream messages received from the client.
    * `grpc_server_msg_sent_total`: Total number of stream messages sent by the server.
* Client
    * `grpc_client_started_total`: Total number of RPCs started on the client.
    * `grpc_client_completed`: Total number of RPCs completed on the client, regardless of success or failure.
    * `grpc_client_completed_latency_seconds`: (Optional) Histogram of rpc response latency for completed rpcs, in seconds.
    * `grpc_client_msg_received_total`: Total number of stream messages received from the server.
    * `grpc_client_msg_sent_total`: Total number of stream messages sent by the client.
    
Note that the presence or absence of the optional metrics can be configured by passing `Configuration` instance to the interceptors.

The server interceptors have an identical implementation in Golang, [go-grpc-prometheus](https://github.com/mwitkow/go-grpc-prometheus), brought to you by [@MWitkow](http://twitter.com/mwitkow).

## Usage

This library is made available on the [dinowernli GitHub Maven repository](https://github.com/dinowernli/maven-repos/tree/master).
Once the repository is set up, the library can be included using the following artifact id:

```
me.dinowernli:java-grpc-prometheus:0.1.0
```

In order to attach the monitoring server interceptor to your gRPC server, you can do the following:

```java
MonitoringServerInterceptor monitoringInterceptor = 
    MonitoringServerInterceptor.create(Configuration.cheapMetricsOnly());
grpcServer = ServerBuilder.forPort(GRPC_PORT)
    .addService(ServerInterceptors.intercept(
        HelloServiceGrpc.bindService(new HelloServiceImpl()), monitoringInterceptor))
    .build();
```

In order to attach the monitoring client interceptor to your gRPC client, you can do the following:

```java
MonitoringClientInterceptor monitoringInterceptor =
    MonitoringClientInterceptor.create(Configuration.cheapMetricsOnly());
grpcStub = HelloServiceGrpc.newStub(NettyChannelBuilder.forAddress(REMOTE_HOST, GRPC_PORT)
    .intercept(monitoringInterceptor)
    .build());
```

## Related reading

* [gRPC](http://grpc.io)
* [Prometheus](http://prometheus.io)
