load("@rules_jvm_external//:defs.bzl", "java_export")

# To deploy to the local maven repo:
# > bazel run --define "maven_repo=file://$HOME/.m2/repository" //:maven_export_lib.publish

java_export(
    name = "maven_export_lib",
    maven_coordinates = "me.dinowernli:java-grpc-prometheus:0.5.0",
    pom_template = "//:pom_template.xml",
    # Make sure these show up in the dependencies of the resulting POM.
    runtime_deps = [
      "//src/main/java/me/dinowernli/grpc/prometheus",
      "//third_party/grpc",
      "//third_party/prometheus",
    ],
    # Exclude classes from these targets from being packaged into the jar itself.
    deploy_env = [
      "//third_party/grpc",
      "//third_party/prometheus",
    ],
)
