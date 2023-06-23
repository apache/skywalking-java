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

import net.bytebuddy.agent.ByteBuddyAgent;
import org.junit.Test;

public class Intercept3Test extends AbstractInterceptTest {

    @Test
    public void test3() {
        ByteBuddyAgent.install();

        // install transformer
        installMethodInterceptor(BIZ_FOO_CLASS_NAME, SAY_HELLO_METHOD, 1);
        installConstructorInterceptor(BIZ_FOO_CLASS_NAME, 1);
        installMethodInterceptor(BIZ_FOO_CLASS_NAME, SAY_HELLO_METHOD, 2);

        // load target class
        callBizFoo(2);

        // check interceptors
        checkMethodInterceptor(SAY_HELLO_METHOD, 1);
        checkConstructorInterceptor(BIZ_FOO_CLASS_NAME, 1);
        checkMethodInterceptor(SAY_HELLO_METHOD, 2);
        checkErrors();
    }

}

