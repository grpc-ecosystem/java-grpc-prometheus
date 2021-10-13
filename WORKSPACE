load("@bazel_tools//tools/build_defs/repo:http.bzl", "http_archive")

GRPC_JAVA_VERSION = "1.29.0"
GRPC_JAVA_SHA = "446ad7a2e85bbd05406dbf95232c7c49ed90de83b3b60cb2048b0c4c9f254d29"

http_archive(
    name = "io_grpc_grpc_java",
    sha256 = GRPC_JAVA_SHA,
    strip_prefix = "grpc-java-%s" % GRPC_JAVA_VERSION,
    url = "https://github.com/grpc/grpc-java/archive/v%s.zip" % GRPC_JAVA_VERSION,
)

load("@io_grpc_grpc_java//:repositories.bzl", "IO_GRPC_GRPC_JAVA_ARTIFACTS")
load("@io_grpc_grpc_java//:repositories.bzl", "IO_GRPC_GRPC_JAVA_OVERRIDE_TARGETS")

RULES_JVM_EXTERNAL_TAG = "4.1"
RULES_JVM_EXTERNAL_SHA = "f36441aa876c4f6427bfb2d1f2d723b48e9d930b62662bf723ddfb8fc80f0140"

http_archive(
    name = "rules_jvm_external",
    sha256 = RULES_JVM_EXTERNAL_SHA,
    strip_prefix = "rules_jvm_external-%s" % RULES_JVM_EXTERNAL_TAG,
    urls = ["https://github.com/bazelbuild/rules_jvm_external/archive/%s.zip" % RULES_JVM_EXTERNAL_TAG],
)
load("@rules_jvm_external//:repositories.bzl", "rules_jvm_external_deps")
rules_jvm_external_deps()

load("@rules_jvm_external//:defs.bzl", "maven_install")

MAVEN_ARTIFACTS = [
    "com.google.cloud:google-cloud-core:1.93.10",
    "com.google.cloud:google-cloud-storage:1.113.4",
    "com.google.truth:truth:1.0.1",
    "io.grpc:grpc-api:%s" % GRPC_JAVA_VERSION,
    "io.grpc:grpc-stub:%s" % GRPC_JAVA_VERSION,
    "io.prometheus:simpleclient:0.11.0",
    "junit:junit:4.10",
    "org.mockito:mockito-all:1.10.19",
]

maven_install(
    artifacts = MAVEN_ARTIFACTS + IO_GRPC_GRPC_JAVA_ARTIFACTS,
    generate_compat_repositories = True,
    maven_install_json = "//:maven_install.json",
    override_targets = IO_GRPC_GRPC_JAVA_OVERRIDE_TARGETS,
    repositories = [
        "https://jcenter.bintray.com/",
        "https://maven.google.com",
        "https://repo1.maven.org/maven2",
    ],
)

load("@maven//:defs.bzl", "pinned_maven_install")

pinned_maven_install()

load("@maven//:compat.bzl", "compat_repositories")

compat_repositories()

load("@io_grpc_grpc_java//:repositories.bzl", "grpc_java_repositories")

# Run grpc_java_repositories after compat_repositories to ensure the
# maven_install-selected dependencies are used.
grpc_java_repositories()

load("@com_google_protobuf//:protobuf_deps.bzl", "protobuf_deps")

protobuf_deps()
