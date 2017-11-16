/*
 * Copyright 2017, OpenSkywalking Organization All rights reserved.
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *     http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 *
 * Project repository: https://github.com/OpenSkywalking/skywalking
 */

package org.skywalking.apm.collector.agent.jetty.handler;

import com.google.gson.JsonElement;
import java.io.IOException;

/**
 * @author peng-yongsheng
 */
public class InstanceRegisterPost {

    public void send(String jsonFile) throws IOException {
        JsonElement instance = JsonFileReader.INSTANCE.read(jsonFile);
        HttpClientTools.INSTANCE.post("http://localhost:12800/instance/register", instance.toString());
    }
}
