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

package test.apache.skywalking.apm.testcase.restapi;

import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;
import org.springframework.http.HttpHeaders;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.DeleteMapping;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.PutMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.ResponseBody;
import org.springframework.web.bind.annotation.RestController;
import org.springframework.web.util.UriComponentsBuilder;
import test.apache.skywalking.apm.testcase.entity.User;

@RestController
public class RestCaseController {

    private static final Map<Integer, User> USERS = new ConcurrentHashMap<Integer, User>();

    @GetMapping(value = "/get/{id}")
    @ResponseBody
    private ResponseEntity<User> getUser(@PathVariable("id") int id) throws InterruptedException {
        User currentUser = new User(id, "a");
        return ResponseEntity.ok(currentUser);
    }

    @PostMapping(value = "/create/")
    @ResponseBody
    public ResponseEntity<Void> createUser(@RequestBody User user,
        UriComponentsBuilder ucBuilder) throws InterruptedException {
        USERS.put(user.getId(), user);
        HttpHeaders headers = new HttpHeaders();
        headers.setLocation(ucBuilder.path("/get/{id}").buildAndExpand(user.getId()).toUri());
        return new ResponseEntity<Void>(headers, HttpStatus.CREATED);
    }

    @PutMapping(value = "/update/{id}")
    @ResponseBody
    public ResponseEntity<User> updateUser(@PathVariable("id") int id,
        @RequestBody User user) throws InterruptedException {
        User currentUser = new User(id, user.getUserName());
        return ResponseEntity.ok(currentUser);
    }

    @DeleteMapping(value = "/delete/{id}")
    @ResponseBody
    public ResponseEntity<Void> deleteUser(@PathVariable("id") int id) throws InterruptedException {
        User currentUser = USERS.get(id);
        if (currentUser == null) {
            return ResponseEntity.noContent().build();
        }
        USERS.remove(id);
        return ResponseEntity.noContent().build();
    }
}
