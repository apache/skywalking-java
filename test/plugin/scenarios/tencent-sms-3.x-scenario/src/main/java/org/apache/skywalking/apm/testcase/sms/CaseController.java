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

package org.apache.skywalking.apm.testcase.sms;

import com.tencentcloudapi.common.Credential;
import com.tencentcloudapi.common.exception.TencentCloudSDKException;
import com.tencentcloudapi.common.profile.ClientProfile;
import com.tencentcloudapi.common.profile.HttpProfile;
import com.tencentcloudapi.sms.v20190711.SmsClient;
import com.tencentcloudapi.sms.v20190711.models.SendSmsResponse;
import com.tencentcloudapi.sms.v20190711.models.SendSmsRequest;
import lombok.extern.log4j.Log4j2;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.ResponseBody;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("/case")
@Log4j2
public class CaseController {

    private static final String SUCCESS = "Success";

    @RequestMapping("/send")
    @ResponseBody
    public String testcase() {
        Credential cred = new Credential("xxxx", "xxx");
        HttpProfile httpProfile = new HttpProfile();
        httpProfile.setEndpoint("sms.tencentcloudapi.com");
        ClientProfile clientProfile = new ClientProfile();
        clientProfile.setHttpProfile(httpProfile);
        SmsClient client = new SmsClient(cred, "ap-beijing", clientProfile);
        SendSmsRequest req = new SendSmsRequest();
        String[] phoneNumberSet1 = {"xxxx"};
        req.setPhoneNumberSet(phoneNumberSet1);

        req.setSmsSdkAppid("xxxx");
        req.setSign("xxxx");
        req.setTemplateID("xxxx");

        String[] templateParamSet1 = {"1212"};
        req.setTemplateParamSet(templateParamSet1);

        SendSmsResponse resp = null;
        try {
            resp = client.SendSms(req);
            System.out.println(SendSmsResponse.toJsonString(resp));

        } catch (TencentCloudSDKException e) {
            throw new RuntimeException(e);
        }
        return SUCCESS;
    }

    @RequestMapping("/healthCheck")
    @ResponseBody
    public String healthCheck() {
        // your codes
        return SUCCESS;
    }

}
