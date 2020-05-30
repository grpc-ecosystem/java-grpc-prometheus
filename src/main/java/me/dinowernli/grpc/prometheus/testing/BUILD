load("@rules_java//java:defs.bzl", "java_library")

package(default_visibility = ["//visibility:public"])

java_library(
    name = "testing",
    testonly = 1,
    srcs = glob(["*.java"]),
    deps = [
        "//src/main/proto:hello_proto_grpc",
        "//third_party/grpc",
        "//third_party/prometheus",
    ],
)
