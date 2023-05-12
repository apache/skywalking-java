package test.org.apache.skywalking.apm.testcase.grizzly.controller;

import com.squareup.okhttp.OkHttpClient;
import java.io.IOException;
import org.glassfish.grizzly.http.server.HttpHandler;
import org.glassfish.grizzly.http.server.Request;
import org.glassfish.grizzly.http.server.Response;

public class CaseHandler extends HttpHandler {


    @Override
    public void service(Request request, Response response) throws Exception {

        com.squareup.okhttp.Request okhttpRequest = new com.squareup.okhttp.Request.Builder().url(
                        "http://127.0.0.1:18181/grizzly-2.3.x-4.x-scenario/case/receive-context")
                .build();
        try {
            new OkHttpClient().newCall(okhttpRequest).execute();
        } catch (IOException e) {
            throw new RuntimeException(e);
        }
        String hello = "hello";
        response.setContentType("text/plain");
        response.setContentLength(hello.length());
        response.getWriter().write(hello);
    }
}
