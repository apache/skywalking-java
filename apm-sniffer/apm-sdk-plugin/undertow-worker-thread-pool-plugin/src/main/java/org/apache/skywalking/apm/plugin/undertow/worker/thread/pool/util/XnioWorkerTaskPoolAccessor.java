/*
 * Licensed to the Apache Software Foundation (ASF) under one or more
 * contributor license agreements.  See the NOTICE file distributed with
 * this work for additional information regarding copyright ownership.
 * The ASF licenses this file to You under the Apache License, Version 2.0
 * (the "License"); you may not use this file except in compliance with
 * the License.  You may obtain a copy of the License at
 *
 *     http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 *
 */

package org.apache.skywalking.apm.plugin.undertow.worker.thread.pool.util;

import java.lang.reflect.Field;
import java.lang.reflect.InvocationTargetException;
import java.lang.reflect.Method;
import lombok.Getter;
import org.xnio.XnioWorker;

public class XnioWorkerTaskPoolAccessor {

    private final Object taskPool;
    @Getter
    private boolean containsGetPoolSizeMethod;

    private Method getCorePoolSizeMethod;
    private Method getMaximumPoolSizeMethod;
    private Method getActiveCountMethod;
    private Method getPoolSizeMethod;
    private Method getQueueSizeMethod;

    public XnioWorkerTaskPoolAccessor(final XnioWorker worker) throws NoSuchFieldException, IllegalAccessException {
        Field field = worker.getClass().getSuperclass().getDeclaredField("taskPool");
        field.setAccessible(true);
        this.taskPool = field.get(worker);

        try {
            getCorePoolSizeMethod = taskPool.getClass().getDeclaredMethod("getCorePoolSize");
            getCorePoolSizeMethod.setAccessible(true);
        } catch (NoSuchMethodException e) {
            // ignore
        }
        try {
            getMaximumPoolSizeMethod = taskPool.getClass().getDeclaredMethod("getMaximumPoolSize");
            getMaximumPoolSizeMethod.setAccessible(true);
        } catch (NoSuchMethodException e) {
            // ignore
        }
        try {
            getActiveCountMethod = taskPool.getClass().getDeclaredMethod("getActiveCount");
            getActiveCountMethod.setAccessible(true);
        } catch (NoSuchMethodException e) {
            // ignore
        }
        try {
            // getPoolSize add since 3.8.0
            getPoolSizeMethod = taskPool.getClass().getDeclaredMethod("getPoolSize");
            getPoolSizeMethod.setAccessible(true);
            containsGetPoolSizeMethod = true;
        } catch (NoSuchMethodException e) {
            containsGetPoolSizeMethod = false;
        }
        try {
            getQueueSizeMethod = taskPool.getClass().getDeclaredMethod("getQueueSize");
            getQueueSizeMethod.setAccessible(true);
        } catch (NoSuchMethodException e) {
            // ignore
        }
    }

    public int getCorePoolSize() {
        try {
            return (int) getCorePoolSizeMethod.invoke(taskPool);
        } catch (IllegalAccessException | InvocationTargetException e) {
            throw new RuntimeException(e);
        }
    }

    public int getMaximumPoolSize() {
        try {
            return (int) getMaximumPoolSizeMethod.invoke(taskPool);
        } catch (IllegalAccessException | InvocationTargetException e) {
            throw new RuntimeException(e);
        }
    }

    public int getActiveCount() {
        try {
            return (int) getActiveCountMethod.invoke(taskPool);
        } catch (IllegalAccessException | InvocationTargetException e) {
            throw new RuntimeException(e);
        }
    }

    public int getPoolSize() {
        try {
            return (int) getPoolSizeMethod.invoke(taskPool);
        } catch (IllegalAccessException | InvocationTargetException e) {
            throw new RuntimeException(e);
        }
    }

    public int getQueueSize() {
        try {
            return (int) getQueueSizeMethod.invoke(taskPool);
        } catch (IllegalAccessException | InvocationTargetException e) {
            throw new RuntimeException(e);
        }
    }
}
