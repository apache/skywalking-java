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

package org.apache.skywalking.apm.plugin.mongodb.v3.support;

import com.mongodb.MongoNamespace;
import lombok.EqualsAndHashCode;
import lombok.Getter;
import org.apache.skywalking.apm.util.StringUtil;

@EqualsAndHashCode
@Getter
public class MongoNamespaceInfo {

    private final String databaseName;
    private final String collectionName;

    public MongoNamespaceInfo(String databaseName) {
        this(databaseName, null);
    }

    public MongoNamespaceInfo(MongoNamespace mongoNamespace) {
        this(mongoNamespace.getDatabaseName(), mongoNamespace.getCollectionName());
    }

    public MongoNamespaceInfo(String databaseName, String collectionName) {
        this.databaseName = databaseName;
        this.collectionName = collectionName;
    }

    public String getDatabaseName() {
        return databaseName;
    }

    public String getCollectionName() {
        return collectionName;
    }

    public String toString() {
        if (StringUtil.isNotBlank(collectionName)) {
            return databaseName + '.' + collectionName;
        } else {
            return databaseName;
        }
    }

}
