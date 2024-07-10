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

package org.apache.skywalking.apm.agent.core.util;

import java.util.HashSet;
import java.util.Set;

/**
 * @author honganan
 */
public class ClassUtil {

    public static boolean isAssignableFrom(String superClassName, Class<?> subClass) {
        if (superClassName == null || subClass == null) {
            return false;
        }

        Set<String> allSuperClassesAndInterfaces = getAllSuperClassesAndInterfaces(subClass);
        return allSuperClassesAndInterfaces.contains(superClassName);
    }

    private static Set<String> getAllSuperClassesAndInterfaces(Class<?> clazz) {
        Set<String> result = new HashSet<>();
        while (clazz != null) {
            result.add(clazz.getName());

            for (Class<?> iface : clazz.getInterfaces()) {
                result.addAll(getAllSuperClassesAndInterfaces(iface));
            }

            clazz = clazz.getSuperclass();
        }
        return result;
    }
}
