load("@rules_java//java:defs.bzl", "java_library")

package(default_visibility = ["//visibility:public"])

java_library(
    name = "testing",
    licenses = ["permissive"],
    exports = [
        "@io_grpc_grpc_java//core:inprocess",
        "@maven//:com_google_truth_truth",
        "@maven//:junit_junit",
        "@maven//:org_mockito_mockito_all",
    ],
)
