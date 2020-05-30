load("@rules_java//java:defs.bzl", "java_test")

java_test(
    name = "server_test",
    size = "small",
    srcs = ["MonitoringServerInterceptorIntegrationTest.java"],
    test_class = "me.dinowernli.grpc.prometheus.integration.MonitoringServerInterceptorIntegrationTest",
    deps = [
        "//src/main/java/me/dinowernli/grpc/prometheus",
        "//src/main/java/me/dinowernli/grpc/prometheus/testing",
        "//src/main/proto:hello_proto_grpc",
        "//third_party/grpc",
        "//third_party/grpc:grpc_testing",
        "//third_party/guava",
        "//third_party/prometheus",
        "//third_party/testing",
    ],
)

java_test(
    name = "client_test",
    size = "small",
    srcs = ["MonitoringClientInterceptorIntegrationTest.java"],
    test_class = "me.dinowernli.grpc.prometheus.integration.MonitoringClientInterceptorIntegrationTest",
    deps = [
        "//src/main/java/me/dinowernli/grpc/prometheus",
        "//src/main/java/me/dinowernli/grpc/prometheus/testing",
        "//src/main/proto:hello_proto_grpc",
        "//third_party/grpc",
        "//third_party/grpc:grpc_testing",
        "//third_party/guava",
        "//third_party/prometheus",
        "//third_party/testing",
    ],
)
