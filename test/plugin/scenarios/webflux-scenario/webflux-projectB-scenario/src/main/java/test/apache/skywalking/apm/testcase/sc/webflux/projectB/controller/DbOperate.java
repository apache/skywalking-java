package test.apache.skywalking.apm.testcase.sc.webflux.projectB.controller;

import org.springframework.stereotype.Service;
import reactor.core.publisher.Mono;
import reactor.core.scheduler.Schedulers;


@Service
public class DbOperate {
    // This method is to generate an annotation trace for testing
    public String selectOne(String param) {
        return "hello"+param;
    }

}
