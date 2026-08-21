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

package org.apache.skywalking.apm.plugin.spring.ldap;

import java.util.Arrays;
import org.apache.skywalking.apm.agent.core.plugin.interceptor.enhance.EnhancedInstance;
import org.apache.skywalking.apm.plugin.spring.ldap.fixture.PackagePrivateContextSources;
import org.junit.Test;

import static org.hamcrest.CoreMatchers.is;
import static org.hamcrest.MatcherAssert.assertThat;

public class LdapEndpointResolverTest {

    @Test
    public void shouldResolveAndSanitizeLdapUrls() {
        UrlContextSource contextSource = new UrlContextSource(
            "ldap://admin:secret@ldap-one.example.org/dc=example,dc=org??sub?(uid=alice)",
            "ldaps://ldap-two.example.org:1636/dc=example,dc=org",
            "ldap://ldap-one.example.org:389/dc=another,dc=org"
        );

        SpringLdapEnhanceInfo info = LdapEndpointResolver.resolve(contextSource);

        assertThat(info.getPeer(), is("ldap-one.example.org:389,ldap-two.example.org:1636"));
    }

    @Test
    public void shouldUseLdapsDefaultPortAndFormatIpv6() {
        assertThat(
            LdapEndpointResolver.peerFromUrls(Arrays.asList("ldaps://[2001:db8::1]/dc=example,dc=org")),
            is("[2001:db8::1]:636")
        );
    }

    @Test
    public void shouldUnwrapPublicContextSourceDelegates() {
        ContextSourceWrapper wrapper = new ContextSourceWrapper(
            new ContextSourceTarget(new UrlContextSource("ldap://wrapped-ldap:1389/dc=example,dc=org")));

        assertThat(LdapEndpointResolver.resolve(wrapper).getPeer(), is("wrapped-ldap:1389"));
    }

    @Test
    public void shouldReadEndpointFromEnhancedObservationWrapper() {
        EnhancedContextSource contextSource = new EnhancedContextSource();
        contextSource.setSkyWalkingDynamicField(new SpringLdapEnhanceInfo("observed-ldap:389"));

        assertThat(LdapEndpointResolver.resolve(contextSource).getPeer(), is("observed-ldap:389"));
    }

    @Test
    public void shouldReturnUnknownForUnsupportedOrMalformedUrls() {
        UrlContextSource contextSource = new UrlContextSource(
            "https://example.org/dc=example,dc=org", "not-an-ldap-url");

        assertThat(LdapEndpointResolver.resolve(contextSource).getPeer(), is(SpringLdapConstants.UNKNOWN_PEER));
    }

    @Test
    public void shouldIgnoreInvalidOrOutOfRangePorts() {
        UrlContextSource contextSource = new UrlContextSource(
            "ldap://host:notaport",
            "ldap://host:0",
            "ldap://host:-1",
            "ldap://host:+389",
            "ldap://host:65536",
            "ldap://host:99999",
            "ldap://valid-ldap:1389"
        );

        assertThat(LdapEndpointResolver.resolve(contextSource).getPeer(), is("valid-ldap:1389"));
    }

    @Test
    public void shouldResolvePeerFromPackagePrivateContextSource() {
        assertThat(
            LdapEndpointResolver.resolve(PackagePrivateContextSources.create()).getPeer(),
            is("hidden-ldap:1389")
        );
    }

    public static class UrlContextSource {

        private final String[] urls;

        public UrlContextSource(String... urls) {
            this.urls = urls;
        }

        public String[] getUrls() {
            return urls;
        }
    }

    public static class ContextSourceWrapper {

        private final Object contextSource;

        public ContextSourceWrapper(Object contextSource) {
            this.contextSource = contextSource;
        }

        public Object getContextSource() {
            return contextSource;
        }
    }

    public static class ContextSourceTarget {

        private final Object target;

        public ContextSourceTarget(Object target) {
            this.target = target;
        }

        public Object getTarget() {
            return target;
        }
    }

    private static class EnhancedContextSource implements EnhancedInstance {

        private Object dynamicField;

        @Override
        public Object getSkyWalkingDynamicField() {
            return dynamicField;
        }

        @Override
        public void setSkyWalkingDynamicField(Object value) {
            dynamicField = value;
        }
    }
}
