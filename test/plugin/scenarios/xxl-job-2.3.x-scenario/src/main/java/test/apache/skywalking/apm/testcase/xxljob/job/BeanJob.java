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

package test.apache.skywalking.apm.testcase.xxljob.job;

import com.xxl.job.core.context.XxlJobHelper;
import com.xxl.job.core.handler.IJobHandler;
import lombok.extern.slf4j.Slf4j;
import okhttp3.Request;
import okhttp3.Response;
import org.apache.skywalking.apm.testcase.xxljob.Utils;

@Slf4j
public class BeanJob extends IJobHandler {

    @Override
    public void execute() throws Exception {

        log.info("BeanJobHandler execute. param: {}", XxlJobHelper.getJobParam());
        
        Request request = new Request.Builder().url("http://localhost:18080/xxl-job-2.3.x-scenario/case/simpleJob").build();
        Response response = Utils.OK_CLIENT.newCall(request).execute();
        response.body().close();

    }
}
