package test.org.apache.skywalking.apm.testcase.grizzly.controller;

import org.glassfish.grizzly.http.server.HttpHandler;
import org.glassfish.grizzly.http.server.Request;
import org.glassfish.grizzly.http.server.Response;

public class HealCheckHandler extends HttpHandler {

    @Override
    public void service(Request request, Response response) throws Exception {
        String hello = "hello";
        response.setContentType("text/plain");
        response.setContentLength(hello.length());
        response.getWriter().write(hello);
    }
}
