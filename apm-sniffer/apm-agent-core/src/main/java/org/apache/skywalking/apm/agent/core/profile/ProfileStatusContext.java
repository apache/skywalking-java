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

import org.apache.skywalking.apm.agent.core.conf.Config;
import org.apache.skywalking.apm.agent.core.context.ContextSnapshot;
import org.apache.skywalking.apm.agent.core.context.TracingContext;

import java.util.concurrent.atomic.AtomicInteger;

/**
 * Wrapper {@link ProfileStatus}, make sure {@link org.apache.skywalking.apm.agent.core.context.TracingContext} with {@link ThreadProfiler} have same reference with {@link ProfileStatus},
 * And only the profile module could change the status
 */
public class ProfileStatusContext {

    private volatile ProfileStatus status;
    private volatile boolean fromFirstSegment;
    private volatile long firstSegmentCreateTime;
    private volatile AtomicInteger subThreadProfilingCount;

    private ProfileStatusContext(ProfileStatus status, long firstSegmentCreateTime, AtomicInteger subThreadProfilingCount) {
        this.status = status;
        this.fromFirstSegment = true;
        this.firstSegmentCreateTime = firstSegmentCreateTime;
        this.subThreadProfilingCount = subThreadProfilingCount;
    }

    /**
     * Create with not watching
     */
    public static ProfileStatusContext createWithNone() {
        return new ProfileStatusContext(ProfileStatus.NONE, 0, null);
    }

    /**
     * Create with pending to profile
     */
    public static ProfileStatusContext createWithPending(long firstSegmentCreateTime) {
        return new ProfileStatusContext(ProfileStatus.PENDING, firstSegmentCreateTime, new AtomicInteger(0));
    }

    public ProfileStatus get() {
        return this.status;
    }

    public long firstSegmentCreateTime() {
        return this.firstSegmentCreateTime;
    }

    public boolean isFromFirstSegment() {
        return fromFirstSegment;
    }

    /**
     * The profile monitoring is watching, wait for some profile conditions.
     */
    public boolean isBeingWatched() {
        return this.status != ProfileStatus.NONE;
    }

    public boolean isProfiling() {
        return this.status == ProfileStatus.PROFILING;
    }

    public ProfileStatusContext clone() {
        return new ProfileStatusContext(this.status, this.firstSegmentCreateTime, this.subThreadProfilingCount);
    }

    /**
     * Continued profile status context
     * @return is needs to keep profile
     */
    public boolean continued(ContextSnapshot snapshot) {
        this.status = snapshot.getProfileStatusContext().get();
        this.fromFirstSegment = false;
        this.firstSegmentCreateTime = snapshot.getProfileStatusContext().firstSegmentCreateTime();
        this.subThreadProfilingCount = snapshot.getProfileStatusContext().subThreadProfilingCount;
        return this.isBeingWatched() &&
            // validate is reach the count of sub-thread
            this.subThreadProfilingCount != null &&
            this.subThreadProfilingCount.incrementAndGet() <= Config.Profile.MAX_ACCEPT_SUB_PARALLEL;
    }

    /**
     * Update status, only access with profile module
     */
    void updateStatus(ProfileStatus status, TracingContext tracingContext) {
        this.status = status;
        if (this.firstSegmentCreateTime == 0 && tracingContext != null) {
            this.firstSegmentCreateTime = tracingContext.createTime();
        }
    }

    void updateStatus(ProfileStatusContext statusContext) {
        this.status = statusContext.get();
        this.firstSegmentCreateTime = statusContext.firstSegmentCreateTime();
        this.subThreadProfilingCount = statusContext.subThreadProfilingCount;
    }

}
