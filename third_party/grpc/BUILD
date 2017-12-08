package(default_visibility = ["//visibility:public"])

java_library(
    name = "grpc",
    licenses = ["permissive"],
    exports = [
        "@com_google_instrumentation_instrumentation_api//jar",
        "@io_grpc_grpc_benchmarks//jar",
        "@io_grpc_grpc_context//jar",
        "@io_grpc_grpc_core//jar",
        "@io_grpc_grpc_netty//jar",
        "@io_grpc_grpc_protobuf//jar",
        "@io_grpc_grpc_protobuf_lite//jar",
        "@io_grpc_grpc_stub//jar",
        "@io_grpc_grpc_okhttp//jar",
    ],
)

java_library(
    name = "grpc_testing",
    testonly = 1,
    licenses = ["permissive"],
    exports = [
        "@grpc_testing_artifact//jar",
    ],
)
