load("@rules_java//java:defs.bzl", "java_library")

package(default_visibility = ["//visibility:public"])

java_library(
    name = "grpc",
    licenses = ["permissive"],
    exports = [
        "@maven//:io_grpc_grpc_api",
        "@maven//:io_grpc_grpc_core",
        "@maven//:io_grpc_grpc_netty",
        "@maven//:io_grpc_grpc_stub",
    ],
)

java_library(
    name = "grpc_testing",
    testonly = 1,
    licenses = ["permissive"],
    exports = [
        "@maven//:io_grpc_grpc_testing",
    ],
)
