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
import org.apache.skywalking.apm.agent.bytebuddy.biz.ChildBar;
import org.apache.skywalking.apm.agent.bytebuddy.biz.ParentBar;
import org.apache.skywalking.apm.agent.core.plugin.interceptor.enhance.EnhancedInstance;
import org.junit.Test;

import java.lang.instrument.Instrumentation;

public class ReTransform4Test extends AbstractReTransformTest {

    @Test
    public void testInterceptConstructor() throws Exception {
        Instrumentation instrumentation = ByteBuddyAgent.install();

        // install transformer
        installMethodInterceptor(PARENT_BAR_CLASS_NAME, "sayHelloParent", 1);
        installMethodInterceptor(CHILD_BAR_CLASS_NAME, "sayHelloChild", 1);
        // implement EnhancedInstance
        installInterface(PARENT_BAR_CLASS_NAME);
        installInterface(CHILD_BAR_CLASS_NAME);

        // call target class
        callBar(1);

        // check interceptors
        checkInterface(ParentBar.class, EnhancedInstance.class);
        checkInterface(ChildBar.class, EnhancedInstance.class);
        checkErrors();

         installTraceClassTransformer("Trace class: ");

        // do retransform
        reTransform(instrumentation, ChildBar.class);

        // check interceptors
        checkInterface(ParentBar.class, EnhancedInstance.class);
        checkInterface(ChildBar.class, EnhancedInstance.class);
        checkErrors();
    }

}

