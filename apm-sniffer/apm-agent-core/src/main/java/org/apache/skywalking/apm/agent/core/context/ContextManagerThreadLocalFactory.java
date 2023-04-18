package org.apache.skywalking.apm.agent.core.context;

/**
 * ContextManagerThreadLocalFactory is used to create a thread local for {@link ContextManager}.
 * @author darknesstm
 * 2023/4/18
 */
public interface ContextManagerThreadLocalFactory {

    /**
     * Create a thread local for {@link ContextManager}.
     * @return
     * @param <T>
     */
    <T> ThreadLocal<T> create();
}
