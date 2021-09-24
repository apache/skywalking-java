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
import org.apache.skywalking.apm.agent.core.plugin.match.MultiClassNameMatch;

import static net.bytebuddy.matcher.ElementMatchers.named;

/**
 * Fastjson A fast JSON parser/generator for Java (It's the most popular
 * json parsing library in China from Alibaba inc).
 * <p>
 * JSON provides a "one stop" solution for json parser/generator solution
 * basic requirements.
 * <p>
 * json2x: JSON#parse()/JSON#parseArray()/JSON#parseObject()/JSON#toJavaObject()
 * x2json: JSON#toJSON()/JSON#toJSONBytes()/JSON#toJSONString()/JSON#writeJSONString()
 */
public class FastjsonInstrumentation extends ClassStaticMethodsEnhancePluginDefine {

    public static final String ENHANCE_CLASS = "com.alibaba.fastjson.JSON";

    public enum Enhance {

        PARSE_ARRAY("parseArray", "org.apache.skywalking.apm.plugin.fastjson.ParseArrayInterceptor"),
        PARSE("parse", "org.apache.skywalking.apm.plugin.fastjson.ParseInterceptor"),
        PARSE_OBJECT("parseObject", "org.apache.skywalking.apm.plugin.fastjson.ParseObjectInterceptor"),
        TO_JAVA_OBJECT("toJavaObject", "org.apache.skywalking.apm.plugin.fastjson.ToJavaObjectInterceptor"),
        TO_JSON_BYTES("toJSONBytes", "org.apache.skywalking.apm.plugin.fastjson.ToJsonBytesInterceptor"),
        TO_JSON("toJSON", "org.apache.skywalking.apm.plugin.fastjson.ToJsonInterceptor"),
        TO_JSON_STRING("toJSONString", "org.apache.skywalking.apm.plugin.fastjson.ToJsonStringInterceptor"),
        WRITE_JSON_STRING("writeJSONString", "org.apache.skywalking.apm.plugin.fastjson.WriteJsonStringInterceptor");

        private String enhanceMethod, interceptorClass;

        Enhance(String enhanceMethod, String interceptorClass) {
            this.enhanceMethod = enhanceMethod;
            this.interceptorClass = interceptorClass;
        }
    }

    @Override
    public StaticMethodsInterceptPoint[] getStaticMethodsInterceptPoints() {

        return new StaticMethodsInterceptPoint[]{
                new StaticMethodsInterceptPoint() {
                    @Override
                    public ElementMatcher<MethodDescription> getMethodsMatcher() {
                        return named(Enhance.PARSE_ARRAY.enhanceMethod);
                    }

                    @Override
                    public String getMethodsInterceptor() {
                        return Enhance.PARSE_ARRAY.interceptorClass;
                    }

                    @Override
                    public boolean isOverrideArgs() {
                        return false;
                    }
                },
                new StaticMethodsInterceptPoint() {
                    @Override
                    public ElementMatcher<MethodDescription> getMethodsMatcher() {
                        return named(Enhance.PARSE.enhanceMethod);
                    }

                    @Override
                    public String getMethodsInterceptor() {
                        return Enhance.PARSE.interceptorClass;
                    }

                    @Override
                    public boolean isOverrideArgs() {
                        return false;
                    }
                },
                new StaticMethodsInterceptPoint() {
                    @Override
                    public ElementMatcher<MethodDescription> getMethodsMatcher() {
                        return named(Enhance.PARSE_OBJECT.enhanceMethod);
                    }

                    @Override
                    public String getMethodsInterceptor() {
                        return Enhance.PARSE_OBJECT.interceptorClass;
                    }

                    @Override
                    public boolean isOverrideArgs() {
                        return false;
                    }
                },
                new StaticMethodsInterceptPoint() {
                    @Override
                    public ElementMatcher<MethodDescription> getMethodsMatcher() {
                        return named(Enhance.TO_JAVA_OBJECT.enhanceMethod);
                    }

                    @Override
                    public String getMethodsInterceptor() {
                        return Enhance.TO_JAVA_OBJECT.interceptorClass;
                    }

                    @Override
                    public boolean isOverrideArgs() {
                        return false;
                    }
                },
                new StaticMethodsInterceptPoint() {
                    @Override
                    public ElementMatcher<MethodDescription> getMethodsMatcher() {
                        return named(Enhance.TO_JSON_BYTES.enhanceMethod);
                    }

                    @Override
                    public String getMethodsInterceptor() {
                        return Enhance.TO_JSON_BYTES.interceptorClass;
                    }

                    @Override
                    public boolean isOverrideArgs() {
                        return false;
                    }
                },
                new StaticMethodsInterceptPoint() {
                    @Override
                    public ElementMatcher<MethodDescription> getMethodsMatcher() {
                        return named(Enhance.TO_JSON.enhanceMethod);
                    }

                    @Override
                    public String getMethodsInterceptor() {
                        return Enhance.TO_JSON.interceptorClass;
                    }

                    @Override
                    public boolean isOverrideArgs() {
                        return false;
                    }
                },
                new StaticMethodsInterceptPoint() {
                    @Override
                    public ElementMatcher<MethodDescription> getMethodsMatcher() {
                        return named(Enhance.TO_JSON_STRING.enhanceMethod);
                    }

                    @Override
                    public String getMethodsInterceptor() {
                        return Enhance.TO_JSON_STRING.interceptorClass;
                    }

                    @Override
                    public boolean isOverrideArgs() {
                        return false;
                    }
                },
                new StaticMethodsInterceptPoint() {
                    @Override
                    public ElementMatcher<MethodDescription> getMethodsMatcher() {
                        return named(Enhance.WRITE_JSON_STRING.enhanceMethod);
                    }

                    @Override
                    public String getMethodsInterceptor() {
                        return Enhance.WRITE_JSON_STRING.interceptorClass;
                    }

                    @Override
                    public boolean isOverrideArgs() {
                        return false;
                    }
                }
        };
    }

    @Override
    protected ClassMatch enhanceClass() {
        return NameMatch.byName(ENHANCE_CLASS);
    }
}
