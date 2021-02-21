/*
 * Licensed to the Apache Software Foundation (ASF) under one or more
 * contributor license agreements.  See the NOTICE file distributed with
 * this work for additional information regarding copyright ownership.
 * The ASF licenses this file to You under the Apache License, Version 2.0
 * (the "License"); you may not use this file except in compliance with
 * the License.  You may obtain a copy of the License at
 *
 *      http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */
import ClientMonitor from 'skywalking-client-js';
import Vue from 'vue';

ClientMonitor.register({
    service: 'test-ui',
    pagePath: 'index.html',
    serviceVersion: 'v1.0.0',
    vue: Vue,
    useFmp: true
});

// vue error
new Vue({
    methods: {
        test() {
            throw {
                msg: 'vue error',
                status: 3000
            }
        }
    },
    created() {
        this.test();
    }
})

fetch('/info', {
    method: 'POST',
    headers: {
        'Content-Type': 'application/json'
    },
}).then((data) => {
    console.log(data.body);
})