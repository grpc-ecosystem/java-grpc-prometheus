package com.github.dinowernli.grpc.prometheus;

import java.util.ArrayList;
import java.util.Collections;
import java.util.List;

import io.grpc.MethodDescriptor;
import io.prometheus.client.SimpleCollector;

/** Helper class which encapsulates the necessary data to increment counter for a single call. */
class MetricHelper {
  private final String methodTypeLabel;
  private final String serviceNameLabel;
  private final String methodNameLabel;

  static <R, S> MetricHelper create(MethodDescriptor<R, S> method) {
    String serviceName = MethodDescriptor.extractFullServiceName(method.getFullMethodName());
    String methodName = method.getFullMethodName();
    return new MetricHelper(method.getType().toString(), serviceName, methodName);
  }

  private MetricHelper(String methodTypeLabel, String serviceNameLabel, String methodNameLabel) {
    this.methodTypeLabel = methodTypeLabel;
    this.serviceNameLabel = serviceNameLabel;
    this.methodNameLabel = methodNameLabel;
  }

  public <T> T addLabels(SimpleCollector<T> collector, String... labels) {
    List<String> allLabels = new ArrayList<>();
    Collections.addAll(allLabels, methodTypeLabel, serviceNameLabel, methodNameLabel);
    Collections.addAll(allLabels, labels);
    return collector.labels(allLabels.toArray(new String[0]));
  }
}
