
package org.apache.skywalking.apm.toolkit.trace;

import java.util.function.Consumer;


@TraceCrossThread
public class ConsumerWrapper<V> implements Consumer<V> {

    final Consumer<V> consumer;

    public ConsumerWrapper(Consumer<V> consumer) {
        this.consumer = consumer;
    }

    public static <V> ConsumerWrapper<V> of(Consumer<V> consumer) {
        return new ConsumerWrapper(consumer);
    }

    @Override
    public void accept(V v) {
        this.consumer.accept(v);
    }

}
