package test.org.apache.skywalking.apm.testcase.spring.retry.controller;

import org.springframework.web.bind.annotation.ResponseBody;
import test.org.apache.skywalking.apm.testcase.spring.retry.service.CaseService;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Controller;
import org.springframework.web.bind.annotation.RequestMapping;

@Controller
@ResponseBody
@RequestMapping("/case")
public class CaseController {

    @Autowired
    private CaseService caseService;

    @RequestMapping("/healthCheck")
    public String health() {
        caseService.handle();
        return "success";
    }

}
