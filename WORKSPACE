http_archive(
    name = "org_pubref_rules_protobuf",
    strip_prefix = "rules_protobuf-0.8.1",
    urls = ["https://github.com/pubref/rules_protobuf/archive/v0.8.1.zip"],
)

load("@org_pubref_rules_protobuf//java:rules.bzl", "java_proto_repositories")

java_proto_repositories()

maven_jar(
    name = "io_grpc_grpc_benchmarks",
    artifact = "io.grpc:grpc-benchmarks:1.6.1",
)

maven_jar(
    name = "io_grpc_grpc_okhttp",
    artifact = "io.grpc:grpc-okhttp:1.6.1",
)

maven_jar(
    name = "grpc_testing_artifact",
    artifact = "io.grpc:grpc-testing:1.4.0",
)

maven_jar(
  name = "guava_artifact",
  artifact = "com.google.guava:guava:21.0",
)

maven_jar(
  name = "junit_artifact",
  artifact = "junit:junit:4.10",
)

maven_jar(
  name = "netty_artifact",
  artifact = "io.netty:netty-all:4.1.13.Final",
)

maven_jar(
  name = "protobuf_java_artifact",
  artifact = "com.google.protobuf:protobuf-java:3.2.0"
)

maven_jar(
  name = "protobuf_java_util_artifact",
  artifact = "com.google.protobuf:protobuf-java-util:3.2.0",
)

maven_jar(
  name = "mockito_artifact",
  artifact = "org.mockito:mockito-all:1.10.19",
)

maven_jar(
  name = "prometheus_client_artifact",
  artifact = "io.prometheus:simpleclient:0.0.14",
)

maven_jar(
  name = "truth_artifact",
  artifact = "com.google.truth:truth:0.28",
)

