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

package test.apache.skywalking.apm.testcase;

import jakarta.ws.rs.GET;
import jakarta.ws.rs.Path;
import jakarta.ws.rs.client.Client;
import jakarta.ws.rs.client.ClientBuilder;
import jakarta.ws.rs.client.Entity;
import jakarta.ws.rs.client.WebTarget;
import jakarta.ws.rs.core.Response;
import test.apache.skywalking.apm.testcase.entity.User;

@Path("/")
public class RestTemplateController {

    private static final String SUCCESS = "Success";

    private static final String URL = "http://localhost:8080/resteasy-6.x-scenario";

    @GET
    @Path("case/resttemplate")
    public Response restTemplate() {
        Client client = ClientBuilder.newBuilder().build();

        // Create user
        User userEntity = new User(1, "a");
        WebTarget target = client.target(URL + "/create/");
        Response response = target.request().post(Entity.json(userEntity));
        String value = response.readEntity(String.class);
        response.close();

        // Find User
        response = client.target(URL + "/get/1").request().get();
        response.close();

        //Modify user
        User updateUserEntity = new User(1, "b");
        response = client.target(URL + "/update/1").request().put(Entity.json(updateUserEntity));
        response.close();

        //Delete user
        response = client.target(URL + "/delete/1").request().delete();
        response.close();

        client.close();

        return Response.ok(SUCCESS).build();
    }

    @GET
    @Path("healthCheck")
    public String healthCheck() {
        return SUCCESS;
    }

}