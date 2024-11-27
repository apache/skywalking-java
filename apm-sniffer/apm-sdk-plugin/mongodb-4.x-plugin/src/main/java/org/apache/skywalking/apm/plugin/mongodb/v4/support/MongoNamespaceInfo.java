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

package org.apache.skywalking.apm.plugin.mongodb.v4.support;

import com.mongodb.MongoNamespace;
import com.mongodb.annotations.Immutable;
import org.apache.skywalking.apm.util.StringUtil;

@Immutable
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

    public boolean equals(Object o) {
        if (this == o) {
            return true;
        } else if (o != null && this.getClass() == o.getClass()) {
            MongoNamespaceInfo that = (MongoNamespaceInfo) o;
            if (!equals(this.databaseName, that.databaseName)) {
                return false;
            } else {
                return equals(this.collectionName, that.collectionName);
            }
        } else {
            return false;
        }
    }

    private boolean equals(String src, String tgt) {
        if (src == null && tgt == null) {
            return true;
        }
        if (src == null && tgt != null) {
            return false;
        }
        if (src != null && tgt == null) {
            return false;
        }
        return src.equals(tgt);
    }

    public String toString() {
        if (StringUtil.isNotBlank(collectionName)) {
            return databaseName + '.' + collectionName;
        } else {
            return databaseName;
        }
    }

    public int hashCode() {
        int result = this.databaseName.hashCode();
        result = 31 * result + this.collectionName.hashCode();
        return result;
    }

}
