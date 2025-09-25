package org.github.forax.framework.interceptor;

import java.lang.annotation.Annotation;
import java.lang.reflect.Method;
import java.lang.reflect.Proxy;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.HashMap;
import java.util.List;
import java.util.Objects;
import java.util.stream.Stream;

public final class InterceptorRegistry {
  //private AroundAdvice advice;
  //private final HashMap<Class<? extends Annotation>, List<AroundAdvice>> aroundAdvices = new HashMap<>();

//  public void addAroundAdvice(Class<? extends Annotation> annotationClass, AroundAdvice aroundAdvice) {
//    Objects.requireNonNull(annotationClass);
//    Objects.requireNonNull(aroundAdvice);
//
//    aroundAdvices.computeIfAbsent(annotationClass, _ -> new ArrayList<>()).add(aroundAdvice);
//  }

  public <T> T createProxy(Class<T> type, T implementation) {
    Objects.requireNonNull(type);
    Objects.requireNonNull(implementation);
    if (!type.isInterface()) {
      throw new IllegalArgumentException("type is not an interface: " + type.getName());
    }

    return type.cast(Proxy.newProxyInstance(type.getClassLoader(),
        new Class<?>[]{type},
        (_, method, args) -> {
          var invocation = getInvocation(findInterceptors(method));

          return invocation.proceed(implementation, method, args);
//          for (var advice : advices){
//            advice.before(implementation, method, args);
//          }
//          Object result = null;
//          try {
//            result = Utils.invokeMethod(implementation, method, args);
//          } finally {
//            for (var advice : advices.reversed()){
//              advice.after(implementation, method, args, result);
//            }
//          }
//          return result;
        }));
  }


//  List<AroundAdvice> findAdvices(Method method) {
//    Objects.requireNonNull(method);
//    var annotations = method.getAnnotations();
//    return Arrays.stream(annotations)
//        .flatMap(ann -> {
//          var annotationType = ann.annotationType();
//          var advice = aroundAdvices.getOrDefault(annotationType, List.of());
//          return advice.stream();
//        })
//        .toList();
//  }


  List<Interceptor> findInterceptors(Method method){
    Objects.requireNonNull(method);
    var annotations = method.getAnnotations();
    return Arrays.stream(annotations)
        .flatMap(ann -> {
          var annotationType = ann.annotationType();
          var interceptorList = interceptors.getOrDefault(annotationType, List.of());
          return interceptorList.stream();
        })
        .toList();
  }

  Invocation getInvocation(List<Interceptor> interceptors) {
    Invocation invocation = Utils::invokeMethod;
    for(var interceptor : interceptors.reversed()){
      var copyOfInvocation = invocation;
      invocation = (o, m, a) ->
          interceptor.intercept(o, m, a, copyOfInvocation);

    }
    return invocation;
  }

  private final HashMap<Class<?>, List<Interceptor>> interceptors = new HashMap<>();

  public void addInterceptor(Class<? extends Annotation> annotationClass, Interceptor interceptor){
    Objects.requireNonNull(annotationClass);
    Objects.requireNonNull(interceptor);

    interceptors.computeIfAbsent(annotationClass, _ -> new ArrayList<>())
        .add(interceptor);

  }

    public void addAroundAdvice(Class<? extends Annotation> annotationClass, AroundAdvice aroundAdvice) {
    Objects.requireNonNull(annotationClass);
    Objects.requireNonNull(aroundAdvice);
      addInterceptor(annotationClass, (o, m, a, i) -> {
      aroundAdvice.before(o, m, a);
      Object result = null;
      try{
        result = i.proceed(o, m, a);
      } finally {
        aroundAdvice.after(o, m, a, result);
      }
      return result;
    });
  }

}
