package org.apache.skywalking.apm.testcase.dubbo3.services.impl;


import org.apache.skywalking.apm.testcase.dubbo3.services.ExceptionService;

public class ExceptionServiceImpl implements ExceptionService {
    @Override
    public void exceptionCall() {
        throw new RuntimeException("test exception!");
    }
}
