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

package org.apache.skywalking.apm.agent.bytebuddy.case1;

import org.apache.skywalking.apm.agent.bytebuddy.Log;
import org.apache.skywalking.apm.agent.bytebuddy.biz.BizFoo;
import net.bytebuddy.agent.ByteBuddyAgent;
import org.apache.skywalking.apm.agent.bytebuddy.biz.ProjectDO;
import org.apache.skywalking.apm.agent.bytebuddy.biz.ProjectService;
import org.junit.Test;

import java.lang.instrument.Instrumentation;
import java.lang.instrument.UnmodifiableClassException;

public class RetransformTest1 extends AbstractRetransformTest {

    @Test
    public void testInterceptConstructor() throws UnmodifiableClassException {
        Instrumentation instrumentation = ByteBuddyAgent.install();

        // install transformer
        installMethodInterceptor(BIZ_FOO_CLASS_NAME, SAY_HELLO_METHOD, 1);
        installConstructorInterceptor(BIZ_FOO_CLASS_NAME, 1);
        // project service
        installMethodInterceptor(PROJECT_SERVICE_CLASS_NAME, "add", 1);
        installMethodInterceptor(PROJECT_SERVICE_CLASS_NAME, "get", 1);
        installMethodInterceptor(PROJECT_SERVICE_CLASS_NAME, "list", 1);
        installConstructorInterceptor(PROJECT_SERVICE_CLASS_NAME, 1);

        // call target class
        try {
            callBizFoo(1);
        } catch (Throwable e) {
            e.printStackTrace();
        } finally {
            // check interceptors
            checkMethodInterceptor(SAY_HELLO_METHOD, 1);
            checkConstructorInterceptor(1);
        }

        ProjectService projectService = new ProjectService();
        projectService.add(ProjectDO.builder()
                .name("test")
                .id(1)
                .build());
        ProjectDO projectDO = projectService.getById(1);
        Log.info(projectDO);
        projectService.list();

        // installTraceClassTransformer("Trace class: ");

        // do retransform
        reTransform(instrumentation, BizFoo.class);
        reTransform(instrumentation, ProjectService.class);

        // test again
        callBizFoo(1);

        projectDO = projectService.getById(1);
        Log.info(projectDO);
    }

}

