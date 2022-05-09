package test.apache.skywalking.apm.testcase;

import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;
import test.apache.skywalking.apm.testcase.entity.User;

import javax.ws.rs.GET;
import javax.ws.rs.Path;
import javax.ws.rs.client.Client;
import javax.ws.rs.client.ClientBuilder;
import javax.ws.rs.client.Entity;
import javax.ws.rs.client.WebTarget;
import javax.ws.rs.core.Response;
import java.io.IOException;

@Path("/")
public class RestTemplateController {

    private static final String SUCCESS = "Success";

    private static final Logger LOGGER = LogManager.getLogger(RestTemplateController.class);

    private static final String URL = "http://localhost:8080/resteasy-4.x-scenario";

    @GET()
    @Path("/case/resttemplate")
    public Response restTemplate() throws IOException {
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
        response = client.target(URL + "/update/1").request().delete();
        response.close();

        client.close();

        return Response.ok(SUCCESS).build();
    }


    @GET
    @Path("/healthCheck")
    public String healthCheck() {
        return SUCCESS;
    }

}
