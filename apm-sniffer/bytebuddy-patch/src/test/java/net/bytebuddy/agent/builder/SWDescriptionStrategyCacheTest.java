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

package net.bytebuddy.agent.builder;

import org.junit.Test;
import org.junit.Assert;

import java.lang.reflect.Field;
import java.lang.reflect.Method;
import java.net.URLClassLoader;
import java.net.URL;
import java.util.Map;

/**
 * Tests the behavior of the WeakHashMap caching mechanism in SWDescriptionStrategy.
 */
public class SWDescriptionStrategyCacheTest {

    @Test
    public void testWeakHashMapCacheCleanup() throws Exception {
        // 获取静态缓存字段
        Field cacheField = SWDescriptionStrategy.SWTypeDescriptionWrapper.class
            .getDeclaredField("CLASS_LOADER_TYPE_CACHE");
        cacheField.setAccessible(true);
        @SuppressWarnings("unchecked")
        Map<ClassLoader, Map<String, SWDescriptionStrategy.TypeCache>> cache = 
            (Map<ClassLoader, Map<String, SWDescriptionStrategy.TypeCache>>) cacheField.get(null);

        // 记录初始缓存大小
        int initialCacheSize = cache.size();

        // 创建测试用的ClassLoader
        URLClassLoader testClassLoader = new URLClassLoader(new URL[0], null);
        String testTypeName = "com.test.DynamicClass";

        // 创建SWTypeDescriptionWrapper实例
        SWDescriptionStrategy.SWTypeDescriptionWrapper wrapper = 
            new SWDescriptionStrategy.SWTypeDescriptionWrapper(
                null, "test", testClassLoader, testTypeName);

        // 通过反射调用getTypeCache方法触发缓存
        Method getTypeCacheMethod = wrapper.getClass()
            .getDeclaredMethod("getTypeCache");
        getTypeCacheMethod.setAccessible(true);
        SWDescriptionStrategy.TypeCache typeCache = 
            (SWDescriptionStrategy.TypeCache) getTypeCacheMethod.invoke(wrapper);

        // 验证缓存中存在该ClassLoader
        Assert.assertTrue("Cache should contain the test ClassLoader", 
            cache.containsKey(testClassLoader));
        Assert.assertNotNull("TypeCache should be created", typeCache);
        Assert.assertEquals("Cache size should increase by 1", 
            initialCacheSize + 1, cache.size());

        // 清除ClassLoader引用，准备GC测试
        testClassLoader = null;
        wrapper = null;
        typeCache = null;

        // 强制垃圾回收
        System.gc();
        Thread.sleep(100);
        System.gc();
        Thread.sleep(100);

        // WeakHashMap应该自动清理被回收的ClassLoader条目
        int attempts = 0;
        int currentCacheSize = cache.size();
        while (currentCacheSize > initialCacheSize && attempts < 20) {
            System.gc();
            Thread.sleep(50);
            currentCacheSize = cache.size();
            attempts++;
        }

        System.out.println("Cache size after GC: " + currentCacheSize + 
            " (initial: " + initialCacheSize + ", attempts: " + attempts + ")");
        
        // 验证WeakHashMap的清理机制工作正常
        Assert.assertTrue("WeakHashMap should clean up entries or attempts should be reasonable", 
            currentCacheSize <= initialCacheSize + 1 && attempts < 20);
    }

    @Test
    public void testBootstrapClassLoaderHandling() throws Exception {
        // 获取Bootstrap ClassLoader缓存字段
        Field bootstrapCacheField = SWDescriptionStrategy.SWTypeDescriptionWrapper.class
            .getDeclaredField("BOOTSTRAP_TYPE_CACHE");
        bootstrapCacheField.setAccessible(true);
        @SuppressWarnings("unchecked")
        Map<String, SWDescriptionStrategy.TypeCache> bootstrapCache = 
            (Map<String, SWDescriptionStrategy.TypeCache>) bootstrapCacheField.get(null);

        int initialBootstrapCacheSize = bootstrapCache.size();

        // 测试Bootstrap ClassLoader (null) 的处理
        String testTypeName = "test.BootstrapClass";
        SWDescriptionStrategy.SWTypeDescriptionWrapper wrapper = 
            new SWDescriptionStrategy.SWTypeDescriptionWrapper(
                null, "test", null, testTypeName);

        // 通过反射调用getTypeCache方法
        Method getTypeCacheMethod = wrapper.getClass()
            .getDeclaredMethod("getTypeCache");
        getTypeCacheMethod.setAccessible(true);
        SWDescriptionStrategy.TypeCache typeCache = 
            (SWDescriptionStrategy.TypeCache) getTypeCacheMethod.invoke(wrapper);

        // 验证Bootstrap ClassLoader的缓存处理
        Assert.assertNotNull("TypeCache should be created for bootstrap classloader", typeCache);
        Assert.assertTrue("Bootstrap cache should contain the type", 
            bootstrapCache.containsKey(testTypeName));
        Assert.assertEquals("Bootstrap cache size should increase by 1", 
            initialBootstrapCacheSize + 1, bootstrapCache.size());
    }

    @Test
    public void testMultipleClassLoadersIndependentCache() throws Exception {
        Field cacheField = SWDescriptionStrategy.SWTypeDescriptionWrapper.class
            .getDeclaredField("CLASS_LOADER_TYPE_CACHE");
        cacheField.setAccessible(true);
        @SuppressWarnings("unchecked")
        Map<ClassLoader, Map<String, SWDescriptionStrategy.TypeCache>> cache = 
            (Map<ClassLoader, Map<String, SWDescriptionStrategy.TypeCache>>) cacheField.get(null);

        int initialCacheSize = cache.size();

        // 创建两个不同的ClassLoader
        URLClassLoader classLoader1 = new URLClassLoader(new URL[0], null);
        URLClassLoader classLoader2 = new URLClassLoader(new URL[0], null);
        String testTypeName = "com.test.SameClassName";

        // 为两个ClassLoader创建相同类名的TypeCache
        SWDescriptionStrategy.SWTypeDescriptionWrapper wrapper1 = 
            new SWDescriptionStrategy.SWTypeDescriptionWrapper(
                null, "test", classLoader1, testTypeName);
        SWDescriptionStrategy.SWTypeDescriptionWrapper wrapper2 = 
            new SWDescriptionStrategy.SWTypeDescriptionWrapper(
                null, "test", classLoader2, testTypeName);

        // 通过反射调用getTypeCache方法
        Method getTypeCacheMethod = 
            SWDescriptionStrategy.SWTypeDescriptionWrapper.class.getDeclaredMethod("getTypeCache");
        getTypeCacheMethod.setAccessible(true);

        SWDescriptionStrategy.TypeCache typeCache1 = 
            (SWDescriptionStrategy.TypeCache) getTypeCacheMethod.invoke(wrapper1);
        SWDescriptionStrategy.TypeCache typeCache2 = 
            (SWDescriptionStrategy.TypeCache) getTypeCacheMethod.invoke(wrapper2);

        // 验证两个ClassLoader有独立的缓存项
        Assert.assertNotNull("TypeCache1 should be created", typeCache1);
        Assert.assertNotNull("TypeCache2 should be created", typeCache2);
        Assert.assertNotSame("TypeCaches should be different instances", typeCache1, typeCache2);

        // 验证缓存结构
        Assert.assertEquals("Cache should contain both classloaders", 
            initialCacheSize + 2, cache.size());
        Assert.assertTrue("Cache should contain classloader1", cache.containsKey(classLoader1));
        Assert.assertTrue("Cache should contain classloader2", cache.containsKey(classLoader2));

        // 验证每个ClassLoader有独立的类型缓存
        Map<String, SWDescriptionStrategy.TypeCache> typeCacheMap1 = cache.get(classLoader1);
        Map<String, SWDescriptionStrategy.TypeCache> typeCacheMap2 = cache.get(classLoader2);
        
        Assert.assertNotNull("ClassLoader1 should have type cache map", typeCacheMap1);
        Assert.assertNotNull("ClassLoader2 should have type cache map", typeCacheMap2);
        Assert.assertNotSame("Type cache maps should be different", typeCacheMap1, typeCacheMap2);
        
        Assert.assertTrue("ClassLoader1 cache should contain the type", 
            typeCacheMap1.containsKey(testTypeName));
        Assert.assertTrue("ClassLoader2 cache should contain the type", 
            typeCacheMap2.containsKey(testTypeName));
    }

    @Test
    public void testConcurrentAccess() throws Exception {
        // 测试并发访问场景
        final String testTypeName = "com.test.ConcurrentClass";
        final int threadCount = 10;
        final Thread[] threads = new Thread[threadCount];
        final URLClassLoader[] classLoaders = new URLClassLoader[threadCount];
        final SWDescriptionStrategy.TypeCache[] results = new SWDescriptionStrategy.TypeCache[threadCount];

        // 创建多个线程同时访问缓存
        for (int i = 0; i < threadCount; i++) {
            final int index = i;
            classLoaders[i] = new URLClassLoader(new URL[0], null);
            threads[i] = new Thread(() -> {
                try {
                    SWDescriptionStrategy.SWTypeDescriptionWrapper wrapper = 
                        new SWDescriptionStrategy.SWTypeDescriptionWrapper(
                            null, "test", classLoaders[index], testTypeName);
                    
                    Method getTypeCacheMethod = wrapper.getClass()
                        .getDeclaredMethod("getTypeCache");
                    getTypeCacheMethod.setAccessible(true);
                    results[index] = (SWDescriptionStrategy.TypeCache) 
                        getTypeCacheMethod.invoke(wrapper);
                } catch (Exception e) {
                    Assert.fail("Concurrent access should not throw exception: " + e.getMessage());
                }
            });
        }

        // 启动所有线程
        for (Thread thread : threads) {
            thread.start();
        }

        // 等待所有线程完成
        for (Thread thread : threads) {
            thread.join(1000); // 最多等待1秒
        }

        // 验证所有结果
        for (int i = 0; i < threadCount; i++) {
            Assert.assertNotNull("Result " + i + " should not be null", results[i]);
        }

        System.out.println("Concurrent access test completed successfully with " + threadCount + " threads");
    }
}