package com.github.forax.framework.mapper;

import jdk.jshell.execution.Util;

import java.beans.Introspector;
import java.beans.PropertyDescriptor;
import java.lang.reflect.Method;
import java.util.Arrays;
import java.util.List;
import java.util.stream.Collectors;

public final class JSONWriter {

    private interface Generator {
        String generate(JSONWriter writer, Object bean);
    }

    private record Property(String prefix, Method getter) {}

    private static final class PropertyDescriptorClassValue
            extends ClassValue<List<Property>> {
        @Override
        protected List<Property> computeValue(Class<?> type) {
            var beanInfo = Utils.beanInfo(type);
            return Arrays.stream(beanInfo.getPropertyDescriptors())
                    .map(propertyDescriptor -> {
                        var name = propertyDescriptor.getName();
                        var getter = propertyDescriptor.getReadMethod();
                        var prefix = "\"" + name + "\": ";
                        return new Property(prefix, getter);
                    }).toList();
        }
    }

    private static final PropertyDescriptorClassValue CLASS_VALUE = new PropertyDescriptorClassValue();

    private String toJSONObject(Object o) {
        var propertyDescriptors = CLASS_VALUE.get(o.getClass());
        return propertyDescriptors.stream()
                .map(property -> {
                    var value = Utils.invokeMethod(o, property.getter());
                    return property.prefix() + toJSON(value);
                }).collect(Collectors.joining(", ", "{", "}"));
    }

    public String toJSON(Object o) {
        return switch (o){
            case Boolean b -> "" + b;
            case Integer i -> "" + i;
            case String s -> '"' + s + "\"";
            case Double d -> "" + d;
            case null -> "null";
            case Object _ -> toJSONObject(o);
        };
    }
}
