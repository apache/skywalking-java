/*
 * Licensed to the Apache Software Foundation (ASF) under one or more
 * contributor license agreements.  See the NOTICE file distributed with
 * this work for additional information regarding copyright ownership.
 * The ASF licenses this file to You under the Apache License, Version 2.0
 * (the "License"); you may not use this file except in compliance with
 * the License.  You may obtain a copy of the License at
 *
 *    http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 *
 */

package org.apache.skywalking.apm.testcase.mongodb.controller;

import com.mongodb.MongoClient;
import com.mongodb.client.AggregateIterable;
import com.mongodb.client.FindIterable;
import com.mongodb.client.MongoCollection;
import com.mongodb.client.MongoDatabase;
import com.mongodb.client.model.Aggregates;
import com.mongodb.client.model.Filters;
import org.bson.BsonDocument;
import org.bson.Document;
import org.bson.conversions.Bson;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import java.util.Arrays;
import java.util.List;

import static com.mongodb.client.model.Filters.eq;

@RestController
@RequestMapping("/case")
public class CaseController {

    @Value(value = "${mongodb.host}")
    private String host;

    @Value(value = "${mongodb.port}")
    private Integer port;

    @GetMapping("/healthCheck")
    public String health() {
        // check connect to mongodb server
        try (MongoClient mongoClient = new MongoClient(host, port)) {
            return "success";
        }
    }

    @RequestMapping("/mongodb")
    public String mongoDBCase() {
        try (MongoClient mongoClient = new MongoClient(host, port)) {
            MongoDatabase db = mongoClient.getDatabase("test-database");
            // CreateCollectionOperation
            db.createCollection("testCollection");

            MongoCollection<Document> collection = db.getCollection("testCollection");
            Document document = Document.parse("{id: 1, name: \"test\"}");
            // MixedBulkWriteOperation
            collection.insertOne(document);

            // FindOperation
            FindIterable<Document> findIterable = collection.find(eq("name", "org"));
            findIterable.first();

            // AggregateOperation
            List<Bson> pipeline = Arrays.asList(
                    Aggregates.match(Filters.eq("name", "test"))
            );
            AggregateIterable<Document> aggregateIterable = collection.aggregate(pipeline);
            aggregateIterable.first();

            // MixedBulkWriteOperation
            collection.updateOne(eq("name", "org"), BsonDocument.parse("{ $set : { \"name\": \"testA\"} }"));

            // FindOperation
            findIterable = collection.find(eq("name", "testA"));
            findIterable.first();

            // MixedBulkWriteOperation
            collection.deleteOne(eq("id", "1"));

            // DropDatabaseOperation
            mongoClient.dropDatabase("test-database");
        }
        return "success";
    }
}
