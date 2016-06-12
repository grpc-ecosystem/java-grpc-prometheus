package(default_visibility = ["//visibility:public"])

java_library(
  name = "prometheus",
  srcs = glob(["*.java"]),
  deps = [
    "//src/main/proto:hello_proto_grpc",
    "//third_party/grpc",
    "//third_party/prometheus",
  ],
)
