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

package org.apache.skywalking.apm.agent.bytebuddy.cases;

import org.apache.skywalking.apm.agent.bytebuddy.Log;

import java.lang.instrument.ClassFileTransformer;
import java.lang.instrument.IllegalClassFormatException;
import java.lang.instrument.Instrumentation;
import java.security.ProtectionDomain;

public class AbstractReTransformTest extends AbstractInterceptTest {

    protected static void reTransform(Instrumentation instrumentation, Class clazz) throws Exception {
        Log.info("-------------");
        Log.info("Begin to re-transform class: " + clazz.getName() + " ..");
        ClassFileTransformer transformer = new ClassFileTransformer() {
            public byte[] transform(ClassLoader loader, String className, Class<?> classBeingRedefined,
                                    ProtectionDomain protectionDomain, byte[] classfileBuffer) throws IllegalClassFormatException {
                Log.info(String.format("transform: className=%s, classBeingRedefined=%s, classloader=%s, protectionDomain=%s, classfileBuffer=%d",
                        className, classBeingRedefined, loader, protectionDomain.getCodeSource(), classfileBuffer.length));
                return null;
            }
        };
        try {
            instrumentation.addTransformer(transformer, true);
            instrumentation.retransformClasses(clazz);
            Log.info("ReTransform class " + clazz.getName() + " successful.");
            Log.info("-------------");
        } catch (Throwable e) {
            Log.info("ReTransform class " + clazz.getName() + " failure: " + e);
            Log.info("-------------");
            throw e;
        } finally {
            instrumentation.removeTransformer(transformer);
        }
    }

}
