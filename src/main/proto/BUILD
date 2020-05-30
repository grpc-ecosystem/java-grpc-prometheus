load("@rules_proto//proto:defs.bzl", "proto_library")
load("@rules_proto_grpc//java:defs.bzl", "java_grpc_library")

package(default_visibility = ["//visibility:public"])

proto_library(
    name = "hello_proto",
    srcs = ["hello.proto"],
)

java_grpc_library(
    name = "hello_proto_grpc",
    deps = [":hello_proto"],
)
