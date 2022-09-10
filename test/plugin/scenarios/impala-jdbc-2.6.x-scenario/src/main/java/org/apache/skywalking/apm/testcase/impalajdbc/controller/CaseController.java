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

package org.apache.skywalking.apm.testcase.impalajdbc.controller;

import lombok.extern.log4j.Log4j2;
import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;
import org.apache.skywalking.apm.testcase.impalajdbc.SQLExecutor;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.ResponseBody;
import org.springframework.web.bind.annotation.RestController;

import java.io.IOException;
import java.net.InetSocketAddress;
import java.net.Socket;

@RestController
@RequestMapping("/case")
@Log4j2
public class CaseController {

    private static final Logger LOGGER = LogManager.getLogger(CaseController.class);

    private static final String SUCCESS = "Success";

    private static final String STATEMENT_INSERT_DATA_SQL = "INSERT INTO impala_test VALUES (123, 'test');";
    private static final String STATEMENT_QUERY_DATA_SQL = "SELECT COUNT(*) FROM impala_test;";
    private static final String STATEMENT_QUERY_DATA_SQL_PARAM = "SELECT COUNT(*) FROM impala_test WHERE test_id = ?;";

    @RequestMapping("/impala-jdbc-2.6.x-scenario")
    @ResponseBody
    public String testcase() throws Exception {
        Thread.sleep(2000);
        return SUCCESS;
    }

    @RequestMapping("/healthCheck")
    @ResponseBody
    public String healthCheck() throws Exception {
        if (!telnet("impala-server", 21050, 1000)) { //
            Thread.sleep(5000); // WAIT UTIL CLIENT TIMEOUT
        } else {
            try (SQLExecutor sqlExecute = new SQLExecutor()) {
                sqlExecute.execute(STATEMENT_INSERT_DATA_SQL);
                sqlExecute.queryData(STATEMENT_QUERY_DATA_SQL);
                sqlExecute.queryData(STATEMENT_QUERY_DATA_SQL_PARAM, 123);
            } catch (Exception ex) {
                LOGGER.error("Failed to execute sql.", ex);
                throw ex;
            }
        }
        return SUCCESS;
    }

    private boolean telnet(String hostname, int port, int timeout) {
        Socket socket = new Socket();
        boolean isConnected = false;
        try {
            socket.connect(new InetSocketAddress(hostname, port), timeout);
            isConnected = socket.isConnected();
        } catch (IOException e) {
            LOGGER.warn("connect to impala server failed");
        } finally {
            try {
                socket.close();
            } catch (IOException e) {
                LOGGER.warn("close failed");
            }
        }
        return isConnected;
    }
}
