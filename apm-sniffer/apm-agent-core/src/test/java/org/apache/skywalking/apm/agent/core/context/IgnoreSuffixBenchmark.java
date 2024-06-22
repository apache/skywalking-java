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

package org.apache.skywalking.apm.agent.core.context;

import org.openjdk.jmh.annotations.Benchmark;
import org.openjdk.jmh.annotations.BenchmarkMode;
import org.openjdk.jmh.annotations.Mode;
import org.openjdk.jmh.annotations.OutputTimeUnit;
import org.openjdk.jmh.annotations.Scope;
import org.openjdk.jmh.annotations.Setup;
import org.openjdk.jmh.annotations.State;
import org.openjdk.jmh.runner.Runner;
import org.openjdk.jmh.runner.options.Options;
import org.openjdk.jmh.runner.options.OptionsBuilder;
import org.openjdk.jmh.runner.options.TimeValue;

import java.util.Arrays;
import java.util.Set;
import java.util.concurrent.TimeUnit;
import java.util.stream.Collectors;
import java.util.stream.Stream;

/**
 * ISSUE-12355
 */
@State(Scope.Benchmark)
@BenchmarkMode({Mode.Throughput, Mode.SampleTime})
@OutputTimeUnit(TimeUnit.NANOSECONDS)
public class IgnoreSuffixBenchmark {
    public static String IGNORE_SUFFIX = ".jpg,.jpeg,.js,.css,.png,.bmp,.gif,.ico,.mp3,.mp4,.html,.svg";
    private String[] ignoreSuffixArray;
    private Set ignoreSuffixSet;

    private String operationName = "test.api";

    @Setup
    public void setup() {
        ignoreSuffixArray = IGNORE_SUFFIX.split(",");
        ignoreSuffixSet = Stream.of(ignoreSuffixArray).collect(Collectors.toSet());
    }

    @Benchmark
    public boolean testArray() {
        int suffixIdx = operationName.lastIndexOf(".");
        return Arrays.stream(ignoreSuffixArray)
                .anyMatch(a -> a.equals(operationName.substring(suffixIdx)));
    }

    @Benchmark
    public boolean testHashSet() {
        int suffixIdx = operationName.lastIndexOf(".");
        return ignoreSuffixSet.contains(operationName.substring(suffixIdx));
    }

    public static void main(String[] args) throws Exception {
        Options opt = new OptionsBuilder()
                .include(IgnoreSuffixBenchmark.class.getName())
                .warmupIterations(3)
                .warmupTime(TimeValue.seconds(10))
                .measurementIterations(3)
                .measurementTime(TimeValue.seconds(10))
                .forks(1)
                .build();
        new Runner(opt).run();
    }
    /**
     * # JMH version: 1.33
     * # VM version: JDK 11.0.21, Java HotSpot(TM) 64-Bit Server VM, 11.0.21+9-LTS-193
     * # VM invoker: /Library/Java/JavaVirtualMachines/jdk-11.jdk/Contents/Home/bin/java
     * # VM options: -javaagent:/Applications/IntelliJ IDEA CE.app/Contents/lib/idea_rt.jar=60939:/Applications/IntelliJ IDEA CE.app/Contents/bin -Dfile.encoding=UTF-8
     * # Blackhole mode: full + dont-inline hint (default, use -Djmh.blackhole.autoDetect=true to auto-detect)
     * # Warmup: 3 iterations, 10 s each
     * # Measurement: 3 iterations, 10 s each
     * # Timeout: 10 min per iteration
     * # Threads: 1 thread, will synchronize iterations
     * # Benchmark mode: Sampling time
     * # Benchmark: org.apache.skywalking.apm.agent.core.context.IgnoreSuffixBenchmark.testHashSet
     *
     * Benchmark                                                Mode     Cnt        Score    Error   Units
     * IgnoreSuffixBenchmark.testArray                         thrpt       3        0.007 ±  0.003  ops/ns
     * IgnoreSuffixBenchmark.testHashSet                       thrpt       3        0.084 ±  0.035  ops/ns
     * IgnoreSuffixBenchmark.testArray                        sample  823984      183.234 ± 11.201   ns/op
     * IgnoreSuffixBenchmark.testArray:testArray·p0.00        sample               41.000            ns/op
     * IgnoreSuffixBenchmark.testArray:testArray·p0.50        sample              166.000            ns/op
     * IgnoreSuffixBenchmark.testArray:testArray·p0.90        sample              167.000            ns/op
     * IgnoreSuffixBenchmark.testArray:testArray·p0.95        sample              209.000            ns/op
     * IgnoreSuffixBenchmark.testArray:testArray·p0.99        sample              375.000            ns/op
     * IgnoreSuffixBenchmark.testArray:testArray·p0.999       sample             1124.630            ns/op
     * IgnoreSuffixBenchmark.testArray:testArray·p0.9999      sample            29971.248            ns/op
     * IgnoreSuffixBenchmark.testArray:testArray·p1.00        sample          1130496.000            ns/op
     * IgnoreSuffixBenchmark.testHashSet                      sample  972621       27.117 ±  1.788   ns/op
     * IgnoreSuffixBenchmark.testHashSet:testHashSet·p0.00    sample                  ≈ 0            ns/op
     * IgnoreSuffixBenchmark.testHashSet:testHashSet·p0.50    sample               41.000            ns/op
     * IgnoreSuffixBenchmark.testHashSet:testHashSet·p0.90    sample               42.000            ns/op
     * IgnoreSuffixBenchmark.testHashSet:testHashSet·p0.95    sample               42.000            ns/op
     * IgnoreSuffixBenchmark.testHashSet:testHashSet·p0.99    sample               83.000            ns/op
     * IgnoreSuffixBenchmark.testHashSet:testHashSet·p0.999   sample              167.000            ns/op
     * IgnoreSuffixBenchmark.testHashSet:testHashSet·p0.9999  sample             6827.950            ns/op
     * IgnoreSuffixBenchmark.testHashSet:testHashSet·p1.00    sample           478208.000            ns/op
     */
}
