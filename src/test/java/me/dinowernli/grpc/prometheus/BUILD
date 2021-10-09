load("@rules_java//java:defs.bzl", "java_test")

java_test(
    name = "unit_test",
    size = "small",
    srcs = ["LabelsTest.java"],
    test_class = "me.dinowernli.grpc.prometheus.LabelsTest",
    deps = [
        "//src/main/java/me/dinowernli/grpc/prometheus",
        "//third_party/grpc",
        "//third_party/guava",
        "//third_party/testing",
    ],
)