package(default_visibility = ["//visibility:public"])

java_library(
  name = "prometheus",
  exports = [
    "@prometheus_client_artifact//jar",
  ],
  licenses = ["permissive"],
)

java_library(
  name = "servlet",
  exports = [
    "@jetty_all_server_artifact//jar",
    "@javax_servlet_api_artifact//jar",
    "@prometheus_client_servlet_artifact//jar",
    "@prometheus_client_common_artifact//jar",
  ],
  licenses = ["permissive"],
)

