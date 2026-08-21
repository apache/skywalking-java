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

import java.lang.reflect.Method;
import java.util.List;
import org.apache.skywalking.apm.agent.core.context.ContextManager;
import org.apache.skywalking.apm.agent.core.context.trace.AbstractSpan;
import org.apache.skywalking.apm.agent.core.context.trace.AbstractTracingSpan;
import org.apache.skywalking.apm.agent.core.context.trace.SpanLayer;
import org.apache.skywalking.apm.agent.core.context.trace.TraceSegment;
import org.apache.skywalking.apm.agent.core.context.util.TagValuePair;
import org.apache.skywalking.apm.agent.core.plugin.interceptor.enhance.EnhancedInstance;
import org.apache.skywalking.apm.agent.core.plugin.interceptor.enhance.v2.MethodInvocationContext;
import org.apache.skywalking.apm.agent.test.helper.SegmentHelper;
import org.apache.skywalking.apm.agent.test.helper.SpanHelper;
import org.apache.skywalking.apm.agent.test.tools.AgentServiceRule;
import org.apache.skywalking.apm.agent.test.tools.SegmentStorage;
import org.apache.skywalking.apm.agent.test.tools.SegmentStoragePoint;
import org.apache.skywalking.apm.agent.test.tools.SpanAssert;
import org.apache.skywalking.apm.agent.test.tools.TracingSegmentRunner;
import org.apache.skywalking.apm.network.trace.component.ComponentsDefine;
import org.junit.After;
import org.junit.Before;
import org.junit.Rule;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.springframework.ldap.core.AuthenticatedLdapEntryContextMapper;
import org.springframework.ldap.core.ContextSource;
import org.springframework.ldap.core.LdapClient;
import org.springframework.ldap.core.LdapTemplate;
import org.springframework.ldap.query.LdapQuery;

import static org.hamcrest.CoreMatchers.is;
import static org.hamcrest.MatcherAssert.assertThat;

@RunWith(TracingSegmentRunner.class)
public class SpringLdapInterceptorTest {

    @SegmentStoragePoint
    private SegmentStorage segmentStorage;

    @Rule
    public AgentServiceRule serviceRule = new AgentServiceRule();

    private SpringLdapOperationInterceptor operationInterceptor;

    private TestEnhancedInstance ldapTemplate;

    private Method searchMethod;

    private Method primitiveAuthenticateMethod;

    private Method templateMapperAuthenticateMethod;

    private Method clientMapperExecuteMethod;

    private Method contextSourceSetterMethod;

    @Before
    public void setUp() throws Exception {
        SpringLdapPluginConfig.Plugin.SpringLDAP.COLLECT_EXCEPTION_DETAILS = false;
        operationInterceptor = new SpringLdapOperationInterceptor();
        ldapTemplate = new TestEnhancedInstance();
        ldapTemplate.setSkyWalkingDynamicField(new SpringLdapEnhanceInfo("ldap-server:389"));
        searchMethod = TestOperations.class.getMethod("search", String.class, String.class);
        primitiveAuthenticateMethod = LdapTemplate.class.getMethod(
            "authenticate", String.class, String.class, String.class);
        templateMapperAuthenticateMethod = LdapTemplate.class.getMethod(
            "authenticate", LdapQuery.class, String.class, AuthenticatedLdapEntryContextMapper.class);
        clientMapperExecuteMethod = LdapClient.AuthenticateSpec.class.getMethod(
            "execute", AuthenticatedLdapEntryContextMapper.class);
        contextSourceSetterMethod = LdapTemplate.class.getMethod("setContextSource", ContextSource.class);
    }

    @After
    public void tearDown() {
        SpringLdapPluginConfig.Plugin.SpringLDAP.COLLECT_EXCEPTION_DETAILS = false;
    }

    @Test
    public void shouldCreatePrivacySafeDatabaseExitSpan() throws Throwable {
        Object[] arguments = {"ou=people", "(uid=alice)"};
        MethodInvocationContext context = new MethodInvocationContext();

        operationInterceptor.beforeMethod(ldapTemplate, searchMethod, arguments, null, context);
        operationInterceptor.afterMethod(ldapTemplate, searchMethod, arguments, null, null, context);

        AbstractTracingSpan span = onlySpan();
        assertThat(span.getOperationName(), is("SpringLDAP/search"));
        assertThat(span.isExit(), is(true));
        assertThat(SpanHelper.getPeer(span), is("ldap-server:389"));
        SpanAssert.assertComponent(span, ComponentsDefine.SPRING_LDAP);
        SpanAssert.assertLayer(span, SpanLayer.DB);
        SpanAssert.assertTagSize(span, 2);
        assertTag(span, 0, "db.type", "LDAP");
        assertTag(span, 1, "ldap.operation", "search");
    }

    @Test
    public void shouldMarkFalsePrimitiveAuthenticationAsError() throws Throwable {
        TestEnhancedInstance authentication = new TestEnhancedInstance();
        authentication.setSkyWalkingDynamicField(new SpringLdapEnhanceInfo("ldap-server:389", "authenticate"));
        MethodInvocationContext context = new MethodInvocationContext();
        Object[] arguments = {"ou=people", "(uid=alice)", "invalid-password"};

        operationInterceptor.beforeMethod(authentication, primitiveAuthenticateMethod, arguments, null, context);
        operationInterceptor.afterMethod(authentication, primitiveAuthenticateMethod, arguments, null, false, context);

        AbstractTracingSpan span = onlySpan();
        assertThat(span.getOperationName(), is("SpringLDAP/authenticate"));
        SpanAssert.assertOccurException(span, true);
        SpanAssert.assertLogSize(span, 0);
    }

    @Test
    public void shouldNotMarkSuccessfulTemplateMapperBooleanFalseAsError() throws Throwable {
        TestEnhancedInstance authentication = new TestEnhancedInstance();
        authentication.setSkyWalkingDynamicField(new SpringLdapEnhanceInfo("ldap-server:389", "authenticate"));
        MethodInvocationContext context = new MethodInvocationContext();
        Object[] arguments = {new Object(), "secret", new Object()};

        operationInterceptor.beforeMethod(authentication, templateMapperAuthenticateMethod, arguments, null, context);
        operationInterceptor.afterMethod(
            authentication, templateMapperAuthenticateMethod, arguments, null, Boolean.FALSE, context);

        AbstractTracingSpan span = onlySpan();
        assertThat(span.getOperationName(), is("SpringLDAP/authenticate"));
        SpanAssert.assertOccurException(span, false);
        SpanAssert.assertLogSize(span, 0);
    }

    @Test
    public void shouldNotMarkSuccessfulClientMapperBooleanFalseAsError() throws Throwable {
        TestEnhancedInstance authentication = new TestEnhancedInstance();
        authentication.setSkyWalkingDynamicField(new SpringLdapEnhanceInfo("ldap-server:389", "authenticate"));
        MethodInvocationContext context = new MethodInvocationContext();
        Object[] arguments = {new Object()};

        operationInterceptor.beforeMethod(authentication, clientMapperExecuteMethod, arguments, null, context);
        operationInterceptor.afterMethod(
            authentication, clientMapperExecuteMethod, arguments, null, Boolean.FALSE, context);

        AbstractTracingSpan span = onlySpan();
        assertThat(span.getOperationName(), is("SpringLDAP/authenticate"));
        SpanAssert.assertOccurException(span, false);
        SpanAssert.assertLogSize(span, 0);
    }

    @Test
    public void shouldSuppressDelegatingOverloadSpansForSameTemplate() throws Throwable {
        MethodInvocationContext outerContext = new MethodInvocationContext();
        MethodInvocationContext innerContext = new MethodInvocationContext();

        operationInterceptor.beforeMethod(ldapTemplate, searchMethod, new Object[0], null, outerContext);
        operationInterceptor.beforeMethod(ldapTemplate, searchMethod, new Object[0], null, innerContext);
        operationInterceptor.afterMethod(ldapTemplate, searchMethod, new Object[0], null, null, innerContext);
        operationInterceptor.afterMethod(ldapTemplate, searchMethod, new Object[0], null, null, outerContext);

        assertThat(spans().size(), is(1));
    }

    @Test
    public void shouldNotMutateActiveExitSpanForNestedLdapOperations() throws Throwable {
        TestEnhancedInstance nestedClient = new TestEnhancedInstance();
        nestedClient.setSkyWalkingDynamicField(new SpringLdapEnhanceInfo("second-ldap:636", "lookup"));
        MethodInvocationContext outerContext = new MethodInvocationContext();
        MethodInvocationContext nestedContext = new MethodInvocationContext();

        operationInterceptor.beforeMethod(ldapTemplate, searchMethod, new Object[0], null, outerContext);
        operationInterceptor.beforeMethod(nestedClient, searchMethod, new Object[0], null, nestedContext);
        operationInterceptor.afterMethod(nestedClient, searchMethod, new Object[0], null, null, nestedContext);
        operationInterceptor.afterMethod(ldapTemplate, searchMethod, new Object[0], null, null, outerContext);

        AbstractTracingSpan span = onlySpan();
        assertThat(span.getOperationName(), is("SpringLDAP/search"));
        assertThat(SpanHelper.getPeer(span), is("ldap-server:389"));
        SpanAssert.assertTagSize(span, 2);
    }

    @Test
    public void shouldMarkExceptionWithoutCollectingDetailsByDefault() throws Throwable {
        MethodInvocationContext context = new MethodInvocationContext();
        IllegalStateException exception = new IllegalStateException(
            "[LDAP: error code 32 - No Such Object]; remaining name 'uid=alice,ou=people'");

        operationInterceptor.beforeMethod(ldapTemplate, searchMethod, new Object[0], null, context);
        operationInterceptor.handleMethodException(ldapTemplate, searchMethod, new Object[0], null, exception, context);
        operationInterceptor.afterMethod(ldapTemplate, searchMethod, new Object[0], null, null, context);

        AbstractTracingSpan span = onlySpan();
        SpanAssert.assertOccurException(span, true);
        SpanAssert.assertLogSize(span, 0);
    }

    @Test
    public void shouldCollectExceptionDetailsWhenExplicitlyEnabled() throws Throwable {
        SpringLdapPluginConfig.Plugin.SpringLDAP.COLLECT_EXCEPTION_DETAILS = true;
        MethodInvocationContext context = new MethodInvocationContext();
        IllegalStateException exception = new IllegalStateException(
            "[LDAP: error code 32 - No Such Object]; remaining name 'uid=alice,ou=people'");

        operationInterceptor.beforeMethod(ldapTemplate, searchMethod, new Object[0], null, context);
        operationInterceptor.handleMethodException(ldapTemplate, searchMethod, new Object[0], null, exception, context);
        operationInterceptor.afterMethod(ldapTemplate, searchMethod, new Object[0], null, null, context);

        AbstractTracingSpan span = onlySpan();
        SpanAssert.assertOccurException(span, true);
        SpanAssert.assertLogSize(span, 1);
        SpanAssert.assertException(
            SpanHelper.getLogs(span).get(0),
            IllegalStateException.class,
            "[LDAP: error code 32 - No Such Object]; remaining name 'uid=alice,ou=people'");
    }

    @Test
    public void shouldRecordExceptionOnLdapSpanWhenCallbackSpanIsActive() throws Throwable {
        MethodInvocationContext context = new MethodInvocationContext();
        IllegalStateException exception = new IllegalStateException(
            "[LDAP: error code 32 - No Such Object]; remaining name 'uid=alice,ou=people'");

        operationInterceptor.beforeMethod(ldapTemplate, searchMethod, new Object[0], null, context);
        AbstractSpan callbackSpan = ContextManager.createLocalSpan("ldap-callback");
        operationInterceptor.handleMethodException(
            ldapTemplate, searchMethod, new Object[0], null, exception, context);
        ContextManager.stopSpan(callbackSpan);
        operationInterceptor.afterMethod(ldapTemplate, searchMethod, new Object[0], null, null, context);

        AbstractTracingSpan ldapSpan = span("SpringLDAP/search");
        AbstractTracingSpan nestedSpan = span("ldap-callback");
        SpanAssert.assertOccurException(ldapSpan, true);
        SpanAssert.assertLogSize(ldapSpan, 0);
        SpanAssert.assertOccurException(nestedSpan, false);
        SpanAssert.assertLogSize(nestedSpan, 0);
    }

    @Test
    public void shouldNotStopUnexpectedActiveSpan() throws Throwable {
        MethodInvocationContext context = new MethodInvocationContext();

        operationInterceptor.beforeMethod(ldapTemplate, searchMethod, new Object[0], null, context);
        AbstractSpan ldapSpan = ContextManager.activeSpan();
        AbstractSpan callbackSpan = ContextManager.createLocalSpan("ldap-callback");
        boolean unexpectedSpanDetected = false;
        try {
            operationInterceptor.afterMethod(ldapTemplate, searchMethod, new Object[0], null, null, context);
        } catch (IllegalStateException expected) {
            unexpectedSpanDetected = true;
            assertThat(ContextManager.activeSpan() == callbackSpan, is(true));
        } finally {
            if (ContextManager.isActive() && ContextManager.activeSpan() == callbackSpan) {
                ContextManager.stopSpan(callbackSpan);
            }
            if (ContextManager.isActive() && ContextManager.activeSpan() == ldapSpan) {
                ContextManager.stopSpan(ldapSpan);
            }
        }

        assertThat(unexpectedSpanDetected, is(true));
        assertThat(spans().size(), is(2));
    }

    @Test
    public void shouldSkipInternalClientSpecWithoutPropagationInfo() throws Throwable {
        TestEnhancedInstance internalSearchSpec = new TestEnhancedInstance();
        MethodInvocationContext context = new MethodInvocationContext();

        operationInterceptor.beforeMethod(internalSearchSpec, searchMethod, new Object[0], null, context);
        operationInterceptor.afterMethod(internalSearchSpec, searchMethod, new Object[0], null, null, context);

        assertThat(segmentStorage.getTraceSegments().size(), is(0));
    }

    @Test
    public void shouldPropagateEndpointAndOperationToClientSpecs() throws Throwable {
        TestEnhancedInstance client = new TestEnhancedInstance();
        TestEnhancedInstance searchSpec = new TestEnhancedInstance();
        TestEnhancedInstance mappedSpec = new TestEnhancedInstance();
        client.setSkyWalkingDynamicField(new SpringLdapEnhanceInfo("client-ldap:389"));
        Method factoryMethod = TestOperations.class.getMethod("search");
        LdapClientSpecFactoryInterceptor factoryInterceptor = new LdapClientSpecFactoryInterceptor();
        LdapClientSpecPropagationInterceptor propagationInterceptor = new LdapClientSpecPropagationInterceptor();

        factoryInterceptor.afterMethod(
            client, factoryMethod, new Object[0], null, searchSpec, new MethodInvocationContext());
        propagationInterceptor.afterMethod(
            searchSpec, factoryMethod, new Object[0], null, mappedSpec, new MethodInvocationContext());

        SpringLdapEnhanceInfo info = (SpringLdapEnhanceInfo) mappedSpec.getSkyWalkingDynamicField();
        assertThat(info.getPeer(), is("client-ldap:389"));
        assertThat(info.getOperation(), is("search"));
    }

    @Test
    public void shouldRefreshEndpointWhenContextSourceChanges() throws Throwable {
        LdapEndpointResolverTest.UrlContextSource contextSource =
            new LdapEndpointResolverTest.UrlContextSource("ldaps://replacement-ldap/dc=example,dc=org");
        LdapEndpointConstructorInterceptor constructorInterceptor = new LdapEndpointConstructorInterceptor();
        LdapContextSourceSetterInterceptor setterInterceptor = new LdapContextSourceSetterInterceptor();
        constructorInterceptor.onConstruct(ldapTemplate, new Object[0]);

        setterInterceptor.afterMethod(
            ldapTemplate, contextSourceSetterMethod, new Object[] {contextSource}, null, null,
            new MethodInvocationContext());

        SpringLdapEnhanceInfo info = (SpringLdapEnhanceInfo) ldapTemplate.getSkyWalkingDynamicField();
        assertThat(info.getPeer(), is("replacement-ldap:636"));
    }

    @Test
    public void shouldPreservePeerWhenContextSourceSetterFails() throws Throwable {
        ldapTemplate.setSkyWalkingDynamicField(new SpringLdapEnhanceInfo("ldap-server:389"));
        LdapContextSourceSetterInterceptor setterInterceptor = new LdapContextSourceSetterInterceptor();
        MethodInvocationContext context = new MethodInvocationContext();
        Object[] arguments = {null};

        setterInterceptor.beforeMethod(ldapTemplate, contextSourceSetterMethod, arguments, null, context);
        setterInterceptor.handleMethodException(
            ldapTemplate, contextSourceSetterMethod, arguments, null,
            new IllegalArgumentException("contextSource must not be null"), context);
        setterInterceptor.afterMethod(ldapTemplate, contextSourceSetterMethod, arguments, null, null, context);

        SpringLdapEnhanceInfo info = (SpringLdapEnhanceInfo) ldapTemplate.getSkyWalkingDynamicField();
        assertThat(info.getPeer(), is("ldap-server:389"));
    }

    private AbstractTracingSpan onlySpan() {
        List<AbstractTracingSpan> spans = spans();
        assertThat(spans.size(), is(1));
        return spans.get(0);
    }

    private List<AbstractTracingSpan> spans() {
        assertThat(segmentStorage.getTraceSegments().size(), is(1));
        TraceSegment segment = segmentStorage.getTraceSegments().get(0);
        return SegmentHelper.getSpans(segment);
    }

    private AbstractTracingSpan span(String operationName) {
        for (AbstractTracingSpan span : spans()) {
            if (operationName.equals(span.getOperationName())) {
                return span;
            }
        }
        throw new AssertionError("Span not found: " + operationName);
    }

    private static void assertTag(AbstractTracingSpan span, int index, String key, String value) {
        TagValuePair tag = SpanHelper.getTags(span).get(index);
        assertThat(tag.getKey().key(), is(key));
        assertThat(tag.getValue(), is(value));
    }

    public static class TestOperations {

        public void search(String base, String filter) {
        }

        public Object search() {
            return null;
        }
    }

    private static class TestEnhancedInstance implements EnhancedInstance {

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
