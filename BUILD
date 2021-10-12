load("@rules_jvm_external//:defs.bzl", "java_export")

java_export(
    name = "maven_export_lib",
    maven_coordinates = "me.dinowernli:java-grpc-prometheus:0.5.0",
    runtime_deps = [
      "//src/main/java/me/dinowernli/grpc/prometheus",
      "//third_party/grpc",
      "//third_party/prometheus",
    ],
    deploy_env = [
      "//third_party/grpc",
      "//third_party/prometheus",
    ],
)
