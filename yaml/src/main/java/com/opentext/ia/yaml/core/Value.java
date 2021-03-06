/*
 * Copyright (c) 2016-2017 by OpenText Corporation. All Rights Reserved.
 */
package com.opentext.ia.yaml.core;

import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.Objects;


/**
 * Value in a {@linkplain YamlMap}. It can be empty, a scalar, a list, or a nested map.
 */
public class Value {

  private final Object data;

  public Value() {
    this(null);
  }

  public Value(Object data) {
    this.data = data instanceof Value ? ((Value)data).data : data;
  }

  public boolean isEmpty() {
    return data == null;
  }

  public boolean isMap() {
    return data instanceof Map;
  }

  public YamlMap toMap() {
    if (!isMap()) {
      return new YamlMap();
    }
    return new YamlMap(data);
  }

  public boolean isList() {
    return data instanceof List;
  }

  @SuppressWarnings("unchecked")
  public YamlSequence toList() {
    if (!isList()) {
      return new YamlSequence(new ArrayList<>());
    }
    return new YamlSequence((List<Object>)data);
  }

  public boolean isScalar() {
    return !isEmpty() && !isList() && !isMap();
  }

  public boolean isBoolean() {
    return toString().equals(Boolean.toString(toBoolean()));
  }

  public boolean toBoolean() {
    return Boolean.parseBoolean(toString());
  }

  public boolean isInt() {
    return toString().equals(Integer.toString(toInt()));
  }

  public int toInt() {
    try {
      return Integer.parseInt(toString());
    } catch (NumberFormatException e) {
      return 0;
    }
  }

  public boolean isDouble() {
    return toString().equals(Double.toString(toDouble()));
  }

  public double toDouble() {
    try {
      return Double.parseDouble(toString());
    } catch (NumberFormatException e) {
      return 0.0;
    }
  }

  public boolean isString() {
    return isScalar() && !isBoolean() && !isInt() && !isDouble();
  }

  @Override
  public String toString() {
    return data == null ? "" : data.toString();
  }

  Object getRawData() {
    return data;
  }

  @Override
  public int hashCode() {
    return Objects.hash(data);
  }

  @Override
  public boolean equals(Object obj) {
    if (obj instanceof Value) {
      Value other = (Value)obj;
      return Objects.equals(data, other.data);
    }
    return Objects.equals(data, obj);
  }

}
