package(default_visibility = ["//visibility:public"])

java_library(
  name = "testing",
  srcs = glob(["*.java"]),
  testonly = 1,
  deps = [
    "//src/main/proto:hello_proto_grpc",
    "//third_party/grpc",
    "//third_party/prometheus",
  ],
)
