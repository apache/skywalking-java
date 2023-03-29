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

package org.apache.skywalking.apm.agent.core.profile;

import java.util.Objects;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Future;
import java.util.concurrent.atomic.AtomicInteger;
import java.util.concurrent.atomic.AtomicReferenceArray;
import org.apache.skywalking.apm.agent.core.conf.Config;
import org.apache.skywalking.apm.agent.core.context.TracingContext;

/**
 * profile task execution context, it will create on process this profile task
 */
public class ProfileTaskExecutionContext {

    // task data
    private final ProfileTask task;

    // record current first endpoint profiling count, use this to check has available profile slot
    private final AtomicInteger currentEndpointProfilingCount = new AtomicInteger(0);

    // profiling segment slot
    private volatile AtomicReferenceArray<ThreadProfiler> profilingSegmentSlots;

    // current profiling execution future
    private volatile Future profilingFuture;

    // total started profiling tracing context count
    private final AtomicInteger totalStartedProfilingCount = new AtomicInteger(0);

    public ProfileTaskExecutionContext(ProfileTask task) {
        this.task = task;
        profilingSegmentSlots = new AtomicReferenceArray<>(Config.Profile.MAX_PARALLEL * (Config.Profile.MAX_ACCEPT_SUB_PARALLEL + 1));
    }

    /**
     * start profiling this task
     */
    public void startProfiling(ExecutorService executorService) {
        profilingFuture = executorService.submit(new ProfileThread(this));
    }

    /**
     * stop profiling
     */
    public void stopProfiling() {
        if (profilingFuture != null) {
            profilingFuture.cancel(true);
        }
    }

    /**
     * check have available slot to profile and add it
     *
     * @return is add profile success
     */
    public ProfileStatusContext attemptProfiling(TracingContext tracingContext,
                                                 String traceSegmentId,
                                                 String firstSpanOPName) {
        // check has limited the max parallel profiling count
        final int profilingEndpointCount = currentEndpointProfilingCount.get();
        if (profilingEndpointCount >= Config.Profile.MAX_PARALLEL) {
            return ProfileStatusContext.createWithNone();
        }

        // check first operation name matches
        if (!Objects.equals(task.getFirstSpanOPName(), firstSpanOPName)) {
            return ProfileStatusContext.createWithNone();
        }

        // if out limit started profiling count then stop add profiling
        if (totalStartedProfilingCount.get() > task.getMaxSamplingCount()) {
            return ProfileStatusContext.createWithNone();
        }

        // try to occupy slot
        if (!currentEndpointProfilingCount.compareAndSet(profilingEndpointCount, profilingEndpointCount + 1)) {
            return ProfileStatusContext.createWithNone();
        }

        ThreadProfiler profiler;
        if ((profiler = addProfilingThread(tracingContext, traceSegmentId)) != null) {
            return profiler.profilingStatus();
        }
        return ProfileStatusContext.createWithNone();
    }

    public boolean continueProfiling(TracingContext tracingContext, String traceSegmentId) {
        return addProfilingThread(tracingContext, traceSegmentId) != null;
    }

    private ThreadProfiler addProfilingThread(TracingContext tracingContext, String traceSegmentId) {
        final ThreadProfiler threadProfiler = new ThreadProfiler(
            tracingContext, traceSegmentId, Thread.currentThread(), this);
        int slotLength = profilingSegmentSlots.length();
        for (int slot = 0; slot < slotLength; slot++) {
            if (profilingSegmentSlots.compareAndSet(slot, null, threadProfiler)) {
                return threadProfiler;
            }
        }
        // add profiling thread failure, so ignore it
        return null;
    }

    /**
     * profiling recheck
     */
    public void profilingRecheck(TracingContext tracingContext, String traceSegmentId, String firstSpanOPName) {
        // if started, keep profiling
        if (tracingContext.profileStatus().isBeingWatched()) {
            return;
        }

        // update profiling status
        tracingContext.profileStatus()
            .updateStatus(attemptProfiling(tracingContext, traceSegmentId, firstSpanOPName));
    }

    /**
     * find tracing context and clear on slot
     */
    public void stopTracingProfile(TracingContext tracingContext) {
        // find current tracingContext and clear it
        int slotLength = profilingSegmentSlots.length();
        for (int slot = 0; slot < slotLength; slot++) {
            ThreadProfiler currentProfiler = profilingSegmentSlots.get(slot);
            if (currentProfiler != null && currentProfiler.matches(tracingContext)) {
                profilingSegmentSlots.set(slot, null);

                // setting stop running
                currentProfiler.stopProfiling();
                if (currentProfiler.profilingStatus().isFromFirstSegment()) {
                    currentEndpointProfilingCount.addAndGet(-1);
                }
                break;
            }
        }
    }

    public ProfileTask getTask() {
        return task;
    }

    public AtomicReferenceArray<ThreadProfiler> threadProfilerSlots() {
        return profilingSegmentSlots;
    }

    public boolean isStartProfileable() {
        // check is out of max sampling count check
        return totalStartedProfilingCount.incrementAndGet() <= task.getMaxSamplingCount();
    }

    @Override
    public boolean equals(Object o) {
        if (this == o)
            return true;
        if (o == null || getClass() != o.getClass())
            return false;
        ProfileTaskExecutionContext that = (ProfileTaskExecutionContext) o;
        return Objects.equals(task, that.task);
    }

    @Override
    public int hashCode() {
        return Objects.hash(task);
    }
}
