package me.dinowernli.grpc.prometheus;

import static com.google.common.truth.Truth.assertThat;

import io.grpc.Metadata;
import io.grpc.Metadata.Key;
import java.util.Arrays;
import java.util.List;
import org.junit.Test;

public class LabelsTest {

  @Test
  public void asArray() {
    List<String> list1 = Arrays.asList("a", "b");
    List<String> list2 = Arrays.asList("1", "2");
    String[] merged = Labels.asArray(list1, list2);
    assertThat(Arrays.asList(merged)).containsExactly("a", "b", "1", "2");
  }

  @Test
  public void metadataKeys() {
    List<Key<String>> keys = Labels.metadataKeys(Arrays.asList("HeaderName"));
    assertThat(keys).containsExactly(Key.of("HeaderName", Metadata.ASCII_STRING_MARSHALLER));
  }

  @Test
  public void customLabels() {
    Metadata metadata = new Metadata();
    metadata.put(Key.of("custom1", Metadata.ASCII_STRING_MARSHALLER), "customvalue1");
    metadata.put(Key.of("standard", Metadata.ASCII_STRING_MARSHALLER), "standardvalue");
    metadata.put(Key.of("custom2", Metadata.ASCII_STRING_MARSHALLER), "customvalue2");

    List<Key<String>> customKeys =
        Arrays.asList(
            Key.of("custom1", Metadata.ASCII_STRING_MARSHALLER),
            Key.of("custom2", Metadata.ASCII_STRING_MARSHALLER),
            Key.of("custom3", Metadata.ASCII_STRING_MARSHALLER));

    List<String> actual = Labels.customLabels(metadata, customKeys);

    // Should return values for custom1 and custom2 from the metadata,
    // and custom3 value is an empty string because it does not exist in metadata.
    assertThat(actual).containsExactly("customvalue1", "customvalue2", "");
  }
}
