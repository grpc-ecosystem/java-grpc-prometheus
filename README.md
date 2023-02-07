# java-grpc-prometheus

![Build Status](https://github.com/grpc-ecosystem/java-grpc-prometheus/actions/workflows/bazel_test.yaml/badge.svg)

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
    
Note that by passing a `Configuration` instance to the interceptors, it is possible to configure the following:
* Whether or not a latency histogram is recorded for RPCs.
* Which histogram buckets to use for the latency metrics.
* Which Prometheus `CollectorRegistry` the metrics get registered with.
* (Optional) Which headers you want to be applied to metrics as added labels.

The server interceptors have an identical implementation in Golang, [go-grpc-prometheus](https://github.com/mwitkow/go-grpc-prometheus), brought to you by [@MWitkow](http://twitter.com/mwitkow).

## Usage

This library is made available on the [dinowernli GitHub Maven repository](https://github.com/dinowernli/maven-repos/tree/master).
Once the repository is set up, the library can be included using the following artifact id:

```
me.dinowernli:java-grpc-prometheus:0.3.0
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

If you want to instruct the interceptor to use a specific header of interest, for example "header-1" as a label on all
produced metrics, you can do the following, which will cause the metrics to carry an extra label "header_1" whose
value is filled from the header value on each request: 

```java
MonitoringServerInterceptor monitoringInterceptor =
    MonitoringServerInterceptor.create(
        Configuration
            .cheapMetricsOnly()
            .withLabelHeaders(Arrays.asList("header-1"))
    );
grpcServer = ServerBuilder.forPort(GRPC_PORT)
    .addService(ServerInterceptors.intercept(
        HelloServiceGrpc.bindService(new HelloServiceImpl()), monitoringInterceptor))
    .build();
```

### Custom CollectorRegistry

In order to attach the monitoring server interceptor to your gRPC server with a custom collector registry, you can do the following:

```java
CollectorRegistry collectorRegistry = new CollectorRegistry();
MonitoringServerInterceptor monitoringInterceptor =
    MonitoringServerInterceptor.create(
        Configuration.cheapMetricsOnly().withCollectorRegistry(collectorRegistry));
grpcServer = ServerBuilder.forPort(GRPC_PORT)
    .addService(ServerInterceptors.intercept(
        HelloServiceGrpc.bindService(new HelloServiceImpl()), monitoringInterceptor))
    .build();
```

In order to attach the monitoring client interceptor to your gRPC client with a custom collector registry, you can do the following:

```java
CollectorRegistry collectorRegistry = new CollectorRegistry();
MonitoringClientInterceptor monitoringInterceptor =
    MonitoringClientInterceptor.create(
        Configuration.cheapMetricsOnly().withCollectorRegistry(collectorRegistry));
grpcStub = HelloServiceGrpc.newStub(NettyChannelBuilder.forAddress(REMOTE_HOST, GRPC_PORT)
    .intercept(monitoringInterceptor)
    .build());
```

If you're using Spring Boot 2 with micrometer-registry-prometheus you should inject the CollectorRegistry that is already provided in the application context:

```java
@Autowired
private CollectorRegistry collectorRegistry;

// use the provided registry 
MonitoringServerInterceptor monitoringInterceptor =  
    MonitoringServerInterceptor.create(Configuration.cheapMetricsOnly().withCollectorRegistry(collectorRegistry));
```


## Related reading

* [gRPC](http://grpc.io)
* [Prometheus](http://prometheus.io)
