/*
 * Copyright (c) 2016-2017 by OpenText Corporation. All Rights Reserved.
 */
package com.opentext.ia.yaml.core;

import java.io.ByteArrayInputStream;
import java.io.File;
import java.io.FileInputStream;
import java.io.FileNotFoundException;
import java.io.InputStream;
import java.nio.charset.StandardCharsets;
import java.util.ArrayList;
import java.util.Collection;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;
import java.util.stream.IntStream;
import java.util.stream.Stream;

import org.apache.commons.io.IOUtils;
import org.yaml.snakeyaml.DumperOptions;
import org.yaml.snakeyaml.DumperOptions.FlowStyle;
import org.yaml.snakeyaml.Yaml;
import org.yaml.snakeyaml.nodes.Node;
import org.yaml.snakeyaml.nodes.Tag;
import org.yaml.snakeyaml.representer.Represent;
import org.yaml.snakeyaml.representer.Representer;


/**
 * Type-safe access to a <a href="http://www.yaml.org/spec/1.2/spec.html">YAML</a> map.
 */
public class YamlMap {


  private final Map<String, Object> data;

  /**
   * Parses the given YAML.
   * @param yaml The YAML string to parse
   * @return A <code>YamlMap</code> corresponding to the provided YAML string
   */
  public static YamlMap from(String yaml) {
    return from(streamOf(yaml));
  }

  private static ByteArrayInputStream streamOf(String text) {
    return new ByteArrayInputStream(text.getBytes(StandardCharsets.UTF_8));
  }

  /**
   * Parses the given YAML.
   * @param yaml The YAML stream to parse
   * @return A <code>YamlMap</code> corresponding to the provided YAML input stream
   */
  public static YamlMap from(InputStream yaml) {
    try {
      YamlMap result = new YamlMap();
      for (Object data : new Yaml().loadAll(yaml)) {
        result.putAll(new YamlMap(data));
      }
      return result;
    } finally {
      IOUtils.closeQuietly(yaml);
    }
  }

  /**
   * Parses the given YAML.
   * @param yaml The YAML file to parse
   * @return A <code>YamlMap</code> corresponding to the provided YAML file, or an empty <code>YamlMap</code> if the
   * file does not exist
   */
  public static YamlMap from(File yaml) {
    try {
      return from(new FileInputStream(yaml));
    } catch (FileNotFoundException e) {
      return new YamlMap();
    }
  }

  public YamlMap() {
    this(null);
  }

  @SuppressWarnings("unchecked")
  public YamlMap(Object data) {
    this.data = data instanceof Map ? (Map<String, Object>)data : new HashMap<>();
  }

  public boolean isEmpty() {
    return data.isEmpty();
  }

  public int size() {
    return data.size();
  }

  public YamlMap put(String key, Object value) {
    data.put(key, unpack(value));
    return this;
  }

  @SuppressWarnings({ "unchecked", "rawtypes" })
  private Object unpack(Object value) {
    if (value instanceof YamlMap) {
      return ((YamlMap)value).getRawData();
    }
    if (value instanceof Iterable) {
      Collection<Object> result = new ArrayList<>();
      ((Iterable)value).forEach(v -> result.add(unpack(v)));
      return result;
    }
    if (value instanceof Value) {
      return ((Value)value).getRawData();
    }
    return value;
  }

  Map<String, Object> getRawData() {
    return data;
  }

  public void putAll(YamlMap other) {
    data.putAll(other.data);
  }

  public boolean containsKey(String key) {
    return data.containsKey(key);
  }

  public YamlMap remove(String key) {
    data.remove(key);
    return this;
  }

  public Value get(Object... keys) {
    YamlMap map = this;
    int i = 0;
    while (i < keys.length - 1) {
      Value value = map.get(keys[i++]);
      if (value.isList()) {
        int index = (int)keys[i++];
        value = value.toList().get(index);
        if (i < keys.length) {
          map = value.toMap();
        } else {
          return value;
        }
      } else {
        map = value.toMap();
      }
    }
    return new Value(map.data.get(keys[keys.length - 1]));
  }

  public Stream<Value> values() {
    return entries().map(Entry::getValue);
  }

  public Stream<Entry> entries() {
    return data.entrySet().stream()
        .map(entry -> new Entry(this, entry.getKey(), new Value(entry.getValue())))
        .sorted();
  }

  public void visit(Visitor visitor) {
    visit(visitor, new Visit(this));
  }

  private void visit(Visitor visitor, Visit visit) {
    if (visitor.test(visit)) {
      visitor.accept(visit);
    }
    if (visitor.maxNesting() > visit.getLevel()) {
      visit.getMap().entries().forEach(entry -> {
        String key = entry.getKey();
        Value value = entry.getValue();
        if (value.isMap()) {
          visit(visitor, visit.descend(key));
        } else if (isListOfMaps(value)) {
          IntStream.range(0, value.toList().size())
              .forEach(index -> visit(visitor, visit.descend(key, index)));
        }
      });
    }
  }

  private boolean isListOfMaps(Value value) {
    List<Value> values = value.toList();
    if (values.isEmpty()) {
      return false;
    }
    return values.size() == values.stream()
        .filter(Value::isMap)
        .count();
  }

  public InputStream toStream() {
    return streamOf(toString());
  }

  @Override
  public String toString() {
    DumperOptions options = new DumperOptions();
    options.setDefaultFlowStyle(FlowStyle.BLOCK);
    options.setPrettyFlow(true);
    Representer representer = new NullSkippingRepresenter();
    return new Yaml(representer, options).dump(data);
  }


  private static class NullSkippingRepresenter extends Representer {

    NullSkippingRepresenter() {
      this.multiRepresenters.put(Map.class, new Represent() {

        @Override
        @SuppressWarnings("unchecked")
        public Node representData(Object data) {
          return representMapping(getTag(data.getClass(), Tag.MAP), filterNullValues(data), null);
        }

      });
    }

    private Map<?, ?> filterNullValues(Object data) {
      Map<?, ?> result = (Map<?, ?>)data;
      if (result.containsValue(null)) {
        result = removePropertiesWithoutValue(result);
      }
      return result;
    }

    private Map<?, ?> removePropertiesWithoutValue(Map<?, ?> source) {
      Map<?, ?> result = new HashMap<>(source);
      Collection<?> propertiesWithoutValue = result.keySet().stream()
          .filter(key -> result.get(key) == null)
          .collect(Collectors.toList());
      propertiesWithoutValue.forEach(result::remove);
      return result;
    }
  }

}
