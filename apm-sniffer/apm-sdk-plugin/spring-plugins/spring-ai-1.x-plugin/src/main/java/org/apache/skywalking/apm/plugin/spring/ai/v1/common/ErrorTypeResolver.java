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

package org.apache.skywalking.apm.plugin.spring.ai.v1.common;

import org.apache.skywalking.apm.agent.core.context.tag.AbstractTag;
import org.apache.skywalking.apm.agent.core.context.tag.Tags;
import org.apache.skywalking.apm.agent.core.context.trace.AbstractSpan;

import javax.net.ssl.SSLHandshakeException;
import java.net.SocketTimeoutException;
import java.security.cert.CertPathValidatorException;
import java.security.cert.CertificateException;
import java.util.concurrent.TimeoutException;

public final class ErrorTypeResolver {

    private static final AbstractTag<String> ERROR_TYPE = Tags.ofKey("error.type");
    private static final String TIMEOUT = "timeout";
    private static final String SERVER_CERTIFICATE_INVALID = "server_certificate_invalid";
    private static final String OTHER = "_OTHER";

    private ErrorTypeResolver() {
    }

    public static void setErrorType(AbstractSpan span, Throwable throwable) {
        span.tag(ERROR_TYPE, resolve(throwable));
    }

    private static String resolve(Throwable throwable) {
        if (matches(throwable, ErrorTypeResolver::isTimeout)) {
            return TIMEOUT;
        }
        if (matches(throwable, ErrorTypeResolver::isCertificateInvalid)) {
            return SERVER_CERTIFICATE_INVALID;
        }
        return throwable.getClass().getName();
    }

    private static boolean isTimeout(Throwable throwable) {
        return throwable instanceof SocketTimeoutException
                || throwable instanceof TimeoutException
                || throwable.getClass().getName().contains("TimeoutException");
    }

    private static boolean isCertificateInvalid(Throwable throwable) {
        return throwable instanceof SSLHandshakeException
                || throwable instanceof CertificateException
                || throwable instanceof CertPathValidatorException;
    }

    private static boolean matches(Throwable throwable, Matcher matcher) {
        Throwable current = throwable;
        while (current != null) {
            if (matcher.matches(current)) {
                return true;
            }
            current = current.getCause();
        }
        return false;
    }

    private interface Matcher {
        boolean matches(Throwable throwable);
    }
}
