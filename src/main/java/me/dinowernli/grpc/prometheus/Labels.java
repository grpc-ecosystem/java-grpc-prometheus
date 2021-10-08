package me.dinowernli.grpc.prometheus;

import io.grpc.Metadata;
import io.grpc.Metadata.Key;
import io.prometheus.client.SimpleCollector;

import java.util.ArrayList;
import java.util.Collections;
import java.util.List;

/**
 * Utility methods for manipulating metric labels.
 */
class Labels {
  /**
   * Merges two string lists into an array, maintaining order of first list then second list.
   */
  static String[] asArray(List<String> firstList, List<String> secondList) {
    List<String> list = new ArrayList<>(firstList);
    list.addAll(secondList);
    return list.toArray(new String[0]);
  }

  /**
   * Converts a list of strings to a list of grpc metadata keys.
   */
  static List<Key<String>> metadataKeys(List<String> headerNames) {
    List<Key<String>> keys = new ArrayList<>();
    for (String name : headerNames) {
      keys.add(Key.of(name, Metadata.ASCII_STRING_MARSHALLER));
    }
    return Collections.unmodifiableList(keys);
  }

  /**
   * Returns the ordered list of custom label values, by looking into metadata for values of selected custom headers.
   */
  static List<String> customLabels(Metadata metadata, List<Key<String>> labelHeaderKeys) {
    List<String> labels = new ArrayList<>();
    for (Key<String> key : labelHeaderKeys) {
      if (metadata.containsKey(key)) {
        labels.add(metadata.get(key));
      } else {
        labels.add("");
      }
    }
    return Collections.unmodifiableList(labels);
  }

  /**
   * Adds standard labels, as well as custom ones, in order, to a given collector.
   */
  static <T> T addLabels(SimpleCollector<T> collector, List<String> labels, GrpcMethod method) {
    List<String> allLabels = new ArrayList<>();
    allLabels.add(method.type());
    allLabels.add(method.serviceName());
    allLabels.add(method.methodName());
    allLabels.addAll(labels);
    return collector.labels(allLabels.toArray(new String[0]));
  }
}
