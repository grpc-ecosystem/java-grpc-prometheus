#!/usr/bin/env bash

set -e

if [ ! -f WORKSPACE ]; then
    echo "This must be invoked from the WORKSPACE root"
    exit 1
fi

if [ ! $# -eq 1 ]; then
    echo "Expected exacly one argument <version>, e.g., <0.1.0>"
    exit 1
fi

JARNAME=java-grpc-prometheus-$1.jar
SRCJARNAME=java-grpc-prometheus-$1-src.jar

rm -rf output && mkdir output && chmod +x output

echo "Running all tests..."
bazel test src/...

echo "Building jar..."
bazel build //src/main/java/me/dinowernli/grpc/prometheus
cp ./bazel-bin/src/main/java/me/dinowernli/grpc/prometheus/libprometheus.jar ./output/$JARNAME

echo "Building srcjar..."
bazel build src/main/java/me/dinowernli/grpc/prometheus:libprometheus-src.jar
cp ./bazel-bin/src/main/java/me/dinowernli/grpc/prometheus/libprometheus-src.jar ./output/$SRCJARNAME

echo "Generating checksums..."
(cd output && sha1sum $JARNAME > $JARNAME.sha1 && sha1sum -c $JARNAME.sha1)
(cd output && md5sum $JARNAME > $JARNAME.md5 && md5sum -c $JARNAME.md5)

echo "Done"

