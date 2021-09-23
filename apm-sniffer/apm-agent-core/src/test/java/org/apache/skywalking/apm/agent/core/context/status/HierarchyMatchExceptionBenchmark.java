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

package org.apache.skywalking.apm.agent.core.context.status;

import org.apache.skywalking.apm.agent.core.boot.ServiceManager;
import org.apache.skywalking.apm.agent.core.conf.Config;
import org.openjdk.jmh.annotations.Benchmark;
import org.openjdk.jmh.annotations.BenchmarkMode;
import org.openjdk.jmh.annotations.Mode;
import org.openjdk.jmh.annotations.OutputTimeUnit;
import org.openjdk.jmh.annotations.Scope;
import org.openjdk.jmh.annotations.State;
import org.openjdk.jmh.infra.Blackhole;
import org.openjdk.jmh.runner.Runner;
import org.openjdk.jmh.runner.options.Options;
import org.openjdk.jmh.runner.options.OptionsBuilder;

import java.util.concurrent.TimeUnit;

/**
 * # JMH version: 1.33
 * # VM version: JDK 1.8.0_292, OpenJDK 64-Bit Server VM, 25.292-b10
 * # VM invoker: /Library/Java/JavaVirtualMachines/adoptopenjdk-8.jdk/Contents/Home/jre/bin/java
 * # VM options: -javaagent:/Applications/IntelliJ IDEA.app/Contents/lib/idea_rt.jar=54972:/Applications/IntelliJ IDEA.app/Contents/bin -Dfile.encoding=UTF-8
 * # Blackhole mode: full + dont-inline hint (default, use -Djmh.blackhole.autoDetect=true to auto-detect)
 * # Warmup: 5 iterations, 10 s each
 * # Measurement: 5 iterations, 10 s each
 * # Timeout: 10 min per iteration
 * # Threads: 1 thread, will synchronize iterations
 * # Benchmark mode: Average time, time/op
 * <p/>
 * Benchmark                                             Mode  Cnt   Score   Error  Units
 * HierarchyMatchExceptionBenchmark.depthOneBenchmark    avgt   25  31.050 ± 0.731  ns/op
 * HierarchyMatchExceptionBenchmark.depthTwoBenchmark    avgt   25  64.918 ± 2.537  ns/op
 * HierarchyMatchExceptionBenchmark.depthThreeBenchmark  avgt   25  89.645 ± 2.556  ns/op
 */
@BenchmarkMode(Mode.AverageTime)
@OutputTimeUnit(TimeUnit.NANOSECONDS)
public class HierarchyMatchExceptionBenchmark {

    @State(Scope.Benchmark)
    public static class ThrowableState {
        static {
            Config.StatusCheck.IGNORED_EXCEPTIONS = "java.lang.NullPointerException";
            Config.StatusCheck.MAX_RECURSIVE_DEPTH = 2;
            ServiceManager.INSTANCE.boot();
        }

        private final Throwable singleT = new NullPointerException();
        private final Throwable doubleT = new RuntimeException(new NullPointerException());
        private final Throwable tripleT = new RuntimeException(new RuntimeException(new NullPointerException()));
    }

    @Benchmark
    public void depthOneBenchmark(Blackhole bh, ThrowableState state) {
        bh.consume(ServiceManager.INSTANCE.findService(StatusCheckService.class).isError(state.singleT));
    }

    @Benchmark
    public void depthTwoBenchmark(Blackhole bh, ThrowableState state) {
        bh.consume(ServiceManager.INSTANCE.findService(StatusCheckService.class).isError(state.doubleT));
    }

    @Benchmark
    public void depthThreeBenchmark(Blackhole bh, ThrowableState state) {
        bh.consume(ServiceManager.INSTANCE.findService(StatusCheckService.class).isError(state.tripleT));
    }

    public static void main(String[] args) throws Exception {
        Options options = new OptionsBuilder()
                .include(HierarchyMatchExceptionBenchmark.class.getSimpleName()).build();
        new Runner(options).run();
    }
}
