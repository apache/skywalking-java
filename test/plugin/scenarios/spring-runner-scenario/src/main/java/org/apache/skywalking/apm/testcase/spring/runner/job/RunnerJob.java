package org.apache.skywalking.apm.testcase.spring.runner.job;

import okhttp3.OkHttpClient;
import okhttp3.Request;
import okhttp3.Response;
import org.springframework.boot.ApplicationRunner;
import org.springframework.boot.CommandLineRunner;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

import java.io.IOException;

@Configuration
public class RunnerJob {
    private static final OkHttpClient CLIENT = new OkHttpClient.Builder().build();

    private static void sendHttpRequest() throws IOException {
        Request request = new Request.Builder().url("http://localhost:8080/spring-runner-scenario/case/spring-runner-scenario").build();
        Response response = CLIENT.newCall(request).execute();
        response.body().close();
    }

    @Bean
    public ApplicationRunner applicationRunner() {
        return args -> sendHttpRequest();
    }

    @Bean
    public CommandLineRunner commandLineRunner() {
        return args -> sendHttpRequest();
    }
}
