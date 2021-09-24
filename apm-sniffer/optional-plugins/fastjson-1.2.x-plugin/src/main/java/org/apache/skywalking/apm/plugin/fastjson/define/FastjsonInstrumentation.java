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

package org.apache.skywalking.apm.plugin.fastjson.define;

import net.bytebuddy.description.method.MethodDescription;
import net.bytebuddy.matcher.ElementMatcher;
import org.apache.skywalking.apm.agent.core.plugin.interceptor.StaticMethodsInterceptPoint;
import org.apache.skywalking.apm.agent.core.plugin.interceptor.enhance.ClassStaticMethodsEnhancePluginDefine;
import org.apache.skywalking.apm.agent.core.plugin.match.ClassMatch;
import org.apache.skywalking.apm.agent.core.plugin.match.NameMatch;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import static net.bytebuddy.matcher.ElementMatchers.named;

/**
 * Fastjson A fast JSON parser/generator for Java from Alibaba inc.
 * <p>
 * JSON provides a "one stop" solution for json parser/generator solution
 * basic requirements.
 * <p>
 * json2x: JSON#parse()/JSON#parseArray()/JSON#parseObject()/JSON#toJavaObject()
 * x2json: JSON#toJSON()/JSON#toJSONBytes()/JSON#toJSONString()/JSON#writeJSONString()
 */
public class FastjsonInstrumentation extends ClassStaticMethodsEnhancePluginDefine {

    public static final String ENHANCE_CLASS = "com.alibaba.fastjson.JSON";

    private static final Map<String, String> ENHANCE_METHODS = new HashMap<String, String>() {
        {
            put("parseArray", "org.apache.skywalking.apm.plugin.fastjson.ParseArrayInterceptor");
            put("parse", "org.apache.skywalking.apm.plugin.fastjson.ParseInterceptor");
            put("parseObject", "org.apache.skywalking.apm.plugin.fastjson.ParseObjectInterceptor");
            put("toJavaObject", "org.apache.skywalking.apm.plugin.fastjson.ToJavaObjectInterceptor");
            put("toJSONBytes", "org.apache.skywalking.apm.plugin.fastjson.ToJsonBytesInterceptor");
            put("toJSON", "org.apache.skywalking.apm.plugin.fastjson.ToJsonInterceptor");
            put("toJSONString", "org.apache.skywalking.apm.plugin.fastjson.ToJsonStringInterceptor");
            put("writeJSONString", "org.apache.skywalking.apm.plugin.fastjson.WriteJsonStringInterceptor");
        }
    };

    @Override
    public StaticMethodsInterceptPoint[] getStaticMethodsInterceptPoints() {

        final List<StaticMethodsInterceptPoint> points = new ArrayList<StaticMethodsInterceptPoint>(ENHANCE_METHODS.size());

        for (Map.Entry<String, String> entry : ENHANCE_METHODS.entrySet()) {
            final StaticMethodsInterceptPoint point = new StaticMethodsInterceptPoint() {
                @Override
                public ElementMatcher<MethodDescription> getMethodsMatcher() {
                    return named(entry.getKey());
                }

                @Override
                public String getMethodsInterceptor() {
                    return entry.getValue();
                }

                @Override
                public boolean isOverrideArgs() {
                    return false;
                }
            };
            points.add(point);
        }

        return points.toArray(new StaticMethodsInterceptPoint[points.size()]);
    }

    @Override
    protected ClassMatch enhanceClass() {
        return NameMatch.byName(ENHANCE_CLASS);
    }
}
