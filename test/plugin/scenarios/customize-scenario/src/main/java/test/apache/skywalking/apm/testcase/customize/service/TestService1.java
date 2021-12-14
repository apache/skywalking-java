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

package test.apache.skywalking.apm.testcase.customize.service;

import java.util.List;
import java.util.Map;

import test.apache.skywalking.apm.testcase.customize.model.Model0;

public class TestService1 {

    public static void staticMethod() {

    }

    public static void staticMethod(String str0, int count, Map m, List l, Object[] os) {

    }

    public void method() {

    }

    public void method(String str0, int count) {

    }

    public void method(Model0 m0, String str0, int count) {

    }

    public String retString(String str0) {
        return str0;
    }

    public Model0 retModel0(Model0 m0) {
        return m0;
    }
}
