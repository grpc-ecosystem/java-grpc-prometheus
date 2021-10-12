load("@rules_java//java:defs.bzl", "java_library")

package(default_visibility = ["//visibility:public"])

java_library(
    name = "grpc",
    licenses = ["permissive"],
    exports = [
        "@maven//:io_grpc_grpc_stub",
        "@maven//:io_grpc_grpc_api",
    ],
)

java_library(
    name = "grpc_testing",
    testonly = 1,
    licenses = ["permissive"],
    exports = [
        "@io_grpc_grpc_java//testing",
    ],
)
