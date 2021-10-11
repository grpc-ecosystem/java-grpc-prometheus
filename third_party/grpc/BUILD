load("@rules_java//java:defs.bzl", "java_library")

package(default_visibility = ["//visibility:public"])

java_library(
    name = "grpc",
    licenses = ["permissive"],
    exports = [
        "@io_grpc_grpc_java//api",
        "@io_grpc_grpc_java//stub",
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
