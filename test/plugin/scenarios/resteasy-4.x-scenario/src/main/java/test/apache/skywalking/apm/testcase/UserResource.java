package test.apache.skywalking.apm.testcase;

import test.apache.skywalking.apm.testcase.entity.User;

import javax.ws.rs.*;
import javax.ws.rs.core.MediaType;
import javax.ws.rs.core.Response;
import java.net.URI;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;

@Path("/")
public class UserResource {

    private static final Map<Integer, User> USERS = new ConcurrentHashMap<Integer, User>();

    @GET()
    @Path("/get/{id}")
    @Produces(MediaType.APPLICATION_JSON)
    public Response getUser(@PathParam("id") int id) throws InterruptedException {
        User currentUser = new User(id, "a");
        return Response.ok(currentUser).build();
    }

    @POST
    @Path("/create/")
    @Produces(MediaType.APPLICATION_JSON)
    @Consumes(MediaType.APPLICATION_JSON)
    public Response createUser(User user) throws InterruptedException {
        USERS.put(user.getId(), user);
        return Response.created(URI.create("")).build();
    }

    @PUT
    @Path("/update/{id}")
    @Produces(MediaType.APPLICATION_JSON)
    @Consumes(MediaType.APPLICATION_JSON)
    public Response updateUser(@PathParam("id") int id, User user) throws InterruptedException {
        User currentUser = new User(id, user.getUserName());
        return Response.ok(currentUser).build();
    }

    @DELETE()
    @Path("/delete/{id}")
    public Response deleteUser(@PathParam("id") int id) throws InterruptedException {
        User currentUser = USERS.get(id);
        if (currentUser == null) {
            return Response.noContent().build();
        }
        USERS.remove(id);
        return Response.noContent().build();
    }
}