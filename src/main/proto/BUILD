load("@rules_proto//proto:defs.bzl", "proto_library")
load("@io_grpc_grpc_java//:java_grpc_library.bzl", "java_grpc_library")
load("@rules_java//java:defs.bzl", "java_proto_library")

package(default_visibility = ["//visibility:public"])

proto_library(
    name = "hello_proto",
    srcs = ["hello.proto"],
)

java_proto_library(
    name = "hello_java_proto",
    deps = [":hello_proto"],
)

java_grpc_library(
    name = "hello_proto_grpc",
    srcs = [":hello_proto"],
    deps = [":hello_java_proto"],
)
