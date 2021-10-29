package org.apache.skywalking.apm.toolkit.trace;

import java.util.function.Function;

@TraceCrossThread
public class FunctionWrapper<T, R> implements Function<T, R> {

    final Function<T, R> function;

    public FunctionWrapper(Function<T, R> function) {
        this.function = function;
    }

    public static <T, R> FunctionWrapper<T, R> of(Function<T, R> function) {
        return new FunctionWrapper(function);
    }

    @Override
    public R apply(T t) {
        return this.function.apply(t);
    }

}