package test.org.apache.skywalking.apm.testcase.spring.retry.service;

import org.apache.skywalking.apm.toolkit.trace.Trace;
import org.springframework.retry.annotation.Backoff;
import org.springframework.retry.annotation.Retryable;
import org.springframework.stereotype.Service;

@Service
public class CaseService {

    @Retryable(value = Exception.class, backoff = @Backoff(delay = 1000, multiplier = 2))
    @Trace
    public void handle() {
        System.out.println("handle");
    }

}
