package com.github.forax.framework.injector;

import java.beans.PropertyDescriptor;
import java.lang.reflect.Constructor;
import java.util.*;
import java.util.function.Supplier;

public final class InjectorRegistry {

    private final HashMap<Class<?>, Supplier<?>> map = new HashMap<>();

    public <T> void registerInstance(Class<T> type, T object){
        Objects.requireNonNull(type, "type is null");
        Objects.requireNonNull(object, "object is null");
        registerProvider(type, () -> object);
    }

    public <T> T lookupInstance(Class<T> type) {
        Objects.requireNonNull(type, "type is null");
        var supplier = map.get(type);
        if(supplier == null) {
            throw new IllegalStateException("not injected " + type.getName());
        }
        return type.cast(supplier.get());
    }

    public <T> void registerProvider(Class<T> type, Supplier<T> supplier) {
        Objects.requireNonNull(type, "type is null");
        Objects.requireNonNull(supplier, "supplier is null");
        var result = map.putIfAbsent(type, supplier);
        if(result != null) {
            throw new IllegalStateException("already injected " + type.getName());
        }
    }

    static List<PropertyDescriptor> findInjectableProperties(Class<?> type) {
        Objects.requireNonNull(type, "type is null");
        var beanInfo = Utils.beanInfo(type);
        return Arrays.stream(beanInfo.getPropertyDescriptors())
                .filter(property -> {
                    var setter = property.getWriteMethod();
                    return setter != null && setter.isAnnotationPresent(Inject.class);
                })
                .toList();
    }

//    public <T> void registerProviderClass(Class<T> type, Class<? extends T> impl) {
//        Objects.requireNonNull(type, "type is null");
//        Objects.requireNonNull(impl, "impl is null");
//
//        var constructor = Utils.defaultConstructor(impl);
//        var properties = findInjectableProperties(type);
//        registerProvider(type, () -> {
//            var object = Utils.newInstance(constructor);
//            for(var property : properties) {
//                var  setter = property.getWriteMethod();
//                var propertyType = property.getPropertyType();
//                var value = lookupInstance(propertyType);
//                Utils.invokeMethod(object, setter, value);
//            }
//            return object;
//        });
//    }

    private Constructor<?> getInjectableConstructor(Class<?> type) {
        var constructors = Arrays.stream(type.getConstructors())
                .filter(c -> c.isAnnotationPresent(Inject.class))
                .toList();

        return switch(constructors.size()) {
            case 0 -> Utils.defaultConstructor(type);
            case 1 -> constructors.getFirst();
            default -> throw new IllegalStateException("more than one constructor annotated with @Inject : " + constructors.size());
        };

    }

    public <T> void registerProviderClass(Class<T> type, Class<? extends T> impl) {
        Objects.requireNonNull(type, "type is null");
        Objects.requireNonNull(impl, "impl is null");

        var constructor = getInjectableConstructor(impl);
        var parameterTypes = constructor.getParameterTypes();
        var properties = findInjectableProperties(type);
        registerProvider(type, () -> {
            var args = Arrays.stream(parameterTypes)
                    .map(this::lookupInstance)
                    .toArray();
            var object = Utils.newInstance(constructor, args);
            for(var property : properties) {
                var  setter = property.getWriteMethod();
                var propertyType = property.getPropertyType();
                var value = lookupInstance(propertyType);
                Utils.invokeMethod(object, setter, value);
            }
            return type.cast(object);
        });

    }

    public void registerProviderClass(Class<?> serviceClass){
        registerProviderClass2(serviceClass);
    }

    private <T> void registerProviderClass2(Class<T> serviceClass){
        Objects.requireNonNull(serviceClass, "serviceClass is null");
        registerProviderClass(serviceClass, serviceClass);
    }


}