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

package org.apache.skywalking.oap.server.core.query.type;

import org.apache.skywalking.oap.server.core.UnexpectedException;

public enum ContentType {
    NONE(0), TEXT(1), JSON(2), YAML(3);

    private int value;

    ContentType(int value) {
        this.value = value;
    }

    public int value() {
        return value;
    }

    public static ContentType instanceOf(int value) {
        switch (value) {
            case 0:
                return NONE;
            case 1:
                return TEXT;
            case 2:
                return JSON;
            case 3:
                return YAML;
            default:
                throw new UnexpectedException("unexpected value=" + value);
        }
    }
}
