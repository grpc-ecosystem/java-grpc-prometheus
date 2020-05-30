load("@rules_java//java:defs.bzl", "java_library")

package(default_visibility = ["//visibility:public"])

java_library(
    name = "prometheus",
    srcs = glob(["*.java"]),
    deps = [
        "//third_party/grpc",
        "//third_party/prometheus",
    ],
)
