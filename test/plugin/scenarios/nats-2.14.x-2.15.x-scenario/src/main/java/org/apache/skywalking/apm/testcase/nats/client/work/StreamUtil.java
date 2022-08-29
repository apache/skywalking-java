/*
 *   Licensed to the Apache Software Foundation (ASF) under one or more
 *   contributor license agreements.  See the NOTICE file distributed with
 *   this work for additional information regarding copyright ownership.
 *   The ASF licenses this file to You under the Apache License, Version 2.0
 *   (the "License"); you may not use this file except in compliance with
 *   the License.  You may obtain a copy of the License at
 *
 *       http://www.apache.org/licenses/LICENSE-2.0
 *
 *   Unless required by applicable law or agreed to in writing, software
 *   distributed under the License is distributed on an "AS IS" BASIS,
 *   WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 *   See the License for the specific language governing permissions and
 *   limitations under the License.
 */

package org.apache.skywalking.apm.testcase.nats.client.work;

import io.nats.client.Connection;
import io.nats.client.JetStreamApiException;
import io.nats.client.JetStreamManagement;
import io.nats.client.api.StorageType;
import io.nats.client.api.StreamConfiguration;
import io.nats.client.api.StreamInfo;

import java.util.List;

public class StreamUtil {

    public static void initStream(Connection connection, String subject, String stream) throws Exception {

        JetStreamManagement jetStreamManagement = connection.jetStreamManagement();
        StreamInfo streamInfo;
        try {
            streamInfo = jetStreamManagement.getStreamInfo(stream);
        } catch (JetStreamApiException e) {
            streamInfo = null;
        }

        if (streamInfo == null) {
            StreamConfiguration sc = StreamConfiguration.builder()
                    .name(stream)
                    .storageType(StorageType.Memory)
                    .subjects(subject)
                    .build();
            jetStreamManagement.addStream(sc);
        } else {
            List<String> subjects = streamInfo.getConfiguration().getSubjects();
            if (!subjects.contains(subject)) {
                subjects.add(subject);
                jetStreamManagement.updateStream(streamInfo.getConfiguration());
            }
        }
    }

}
