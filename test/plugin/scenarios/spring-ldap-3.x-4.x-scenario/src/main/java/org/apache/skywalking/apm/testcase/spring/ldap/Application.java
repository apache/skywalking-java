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

package org.apache.skywalking.apm.testcase.spring.ldap;

import io.micrometer.observation.ObservationRegistry;
import io.undertow.Undertow;
import io.undertow.server.HttpServerExchange;
import io.undertow.util.Headers;
import io.undertow.util.StatusCodes;
import java.lang.reflect.InvocationTargetException;
import java.lang.reflect.Method;
import java.util.List;
import javax.naming.NameClassPair;
import javax.naming.NameNotFoundException;
import javax.naming.NamingException;
import javax.naming.directory.Attribute;
import javax.naming.directory.Attributes;
import javax.naming.directory.BasicAttribute;
import javax.naming.directory.BasicAttributes;
import javax.naming.directory.DirContext;
import javax.naming.directory.ModificationItem;
import org.springframework.ldap.core.AttributesMapper;
import org.springframework.ldap.core.ContextSource;
import org.springframework.ldap.core.LdapClient;
import org.springframework.ldap.core.LdapTemplate;
import org.springframework.ldap.core.NameClassPairMapper;
import org.springframework.ldap.core.support.LdapContextSource;
import org.springframework.ldap.core.support.ObservationContextSource;
import org.springframework.ldap.query.LdapQuery;
import org.springframework.ldap.query.LdapQueryBuilder;
import org.springframework.ldap.support.LdapUtils;

public final class Application {

    private static final String CASE_PATH = "/spring-ldap-3.x-4.x-scenario/case/spring-ldap";

    private static final String HEALTH_PATH = "/spring-ldap-3.x-4.x-scenario/case/healthCheck";

    private static final String ORGANIZATION_UNIT_DN = "ou=skywalking";

    private static final String USER_DN = "cn=alice," + ORGANIZATION_UNIT_DN;

    private static final String RENAMED_USER_DN = "cn=alice-renamed," + ORGANIZATION_UNIT_DN;

    private static final String TEMP_USER_DN = "cn=temp," + ORGANIZATION_UNIT_DN;

    private static final String USER_PASSWORD = "secret";

    private final LdapContextSource contextSource;

    private final LdapTemplate ldapTemplate;

    private final LdapClient ldapClient;

    private Application(final LdapContextSource contextSource) {
        this.contextSource = contextSource;
        this.ldapTemplate = new LdapTemplate(contextSource);
        final ObservationContextSource observationContextSource =
            new ObservationContextSource(contextSource, ObservationRegistry.NOOP);
        this.ldapClient = LdapClient.create(observationContextSource);
    }

    public static void main(String[] args) throws Exception {
        final LdapContextSource contextSource = createContextSource();
        waitForLdap(contextSource);
        final Application application = new Application(contextSource);

        final Undertow server = Undertow.builder()
                                        .addHttpListener(8080, "0.0.0.0")
                                        .setHandler(application::handleRequest)
                                        .build();
        Runtime.getRuntime().addShutdownHook(new Thread(server::stop));
        server.start();
    }

    private static LdapContextSource createContextSource() throws Exception {
        LdapContextSource source = new LdapContextSource();
        source.setUrl("ldap://openldap:389");
        source.setBase("dc=example,dc=org");
        source.setUserDn("cn=admin,dc=example,dc=org");
        source.setPassword("admin");
        source.afterPropertiesSet();
        return source;
    }

    private static void waitForLdap(ContextSource source) throws InterruptedException {
        RuntimeException lastFailure = null;
        for (int attempt = 0; attempt < 60; attempt++) {
            DirContext context = null;
            try {
                context = source.getReadOnlyContext();
                return;
            } catch (RuntimeException failure) {
                lastFailure = failure;
                Thread.sleep(500L);
            } finally {
                LdapUtils.closeContext(context);
            }
        }
        throw new IllegalStateException("OpenLDAP did not become ready", lastFailure);
    }

    private void handleRequest(HttpServerExchange exchange) {
        exchange.getResponseHeaders().put(Headers.CONTENT_TYPE, "text/plain");
        if (HEALTH_PATH.equals(exchange.getRequestPath())) {
            exchange.getResponseSender().send("Success");
            return;
        }
        if (!CASE_PATH.equals(exchange.getRequestPath())) {
            exchange.setStatusCode(StatusCodes.NOT_FOUND);
            exchange.getResponseSender().send("Not Found");
            return;
        }

        exchange.dispatch(() -> {
            try {
                runScenario();
                exchange.getResponseSender().send("Success");
            } catch (Throwable failure) {
                failure.printStackTrace();
                cleanupRaw();
                exchange.setStatusCode(StatusCodes.INTERNAL_SERVER_ERROR);
                exchange.getResponseSender().send("Failure");
            }
        });
    }

    private synchronized void runScenario() throws Throwable {
        cleanupRaw();
        boolean completed = false;
        try {
            ldapTemplate.bind(ORGANIZATION_UNIT_DN, null, organizationUnitAttributes());
            ldapTemplate.bind(USER_DN, null, personAttributes("alice", "SkyWalking", "alice"));

            LdapQuery aliceQuery = aliceQuery();
            List<String> templateSearch = ldapTemplate.search(aliceQuery, uidMapper());
            require(templateSearch.size() == 1 && "alice".equals(templateSearch.get(0)),
                "LdapTemplate search returned an unexpected result");

            String lookedUpUid = ldapTemplate.lookup(USER_DN, uidMapper());
            require("alice".equals(lookedUpUid), "LdapTemplate lookup returned an unexpected result");

            ldapTemplate.authenticate(aliceQuery, USER_PASSWORD);
            boolean invalidCredentialsAccepted = ldapTemplate.authenticate(
                ORGANIZATION_UNIT_DN, "(uid=alice)", "invalid-password");
            require(!invalidCredentialsAccepted, "LdapTemplate accepted invalid credentials");
            ldapTemplate.modifyAttributes(USER_DN, new ModificationItem[] {
                replaceAttribute("sn", "SkyWalkingUpdated")
            });
            ldapTemplate.rename(USER_DN, RENAMED_USER_DN);

            List<String> children = ldapClient.list(ORGANIZATION_UNIT_DN).toList(NameClassPair::getName);
            require(children.contains("cn=alice-renamed"), "LdapClient list did not return the renamed entry");

            List<String> bindings = ldapClient.listBindings(ORGANIZATION_UNIT_DN)
                                              .toList((NameClassPairMapper<String>) NameClassPair::getName);
            require(bindings.contains("cn=alice-renamed"),
                "LdapClient listBindings did not return the renamed entry");

            List<String> clientSearch = searchWithClient(aliceQuery);
            require(clientSearch.size() == 1 && "alice".equals(clientSearch.get(0)),
                "LdapClient search returned an unexpected result");

            ldapClient.authenticate().query(aliceQuery).password(USER_PASSWORD).execute();
            ldapClient.bind(TEMP_USER_DN).attributes(personAttributes("temp", "Temporary", "temp")).execute();
            ldapClient.modify(TEMP_USER_DN).attributes(replaceAttribute("sn", "TemporaryUpdated")).execute();
            ldapClient.unbind(TEMP_USER_DN).execute();

            ldapTemplate.unbind(RENAMED_USER_DN);
            ldapTemplate.unbind(ORGANIZATION_UNIT_DN);
            completed = true;
        } finally {
            if (!completed) {
                cleanupRaw();
            }
        }
    }

    @SuppressWarnings("unchecked")
    private List<String> searchWithClient(LdapQuery query) throws Throwable {
        LdapClient.SearchSpec search = ldapClient.search().query(query);
        AttributesMapper<String> mapper = uidMapper();
        try {
            Method map = LdapClient.SearchSpec.class.getMethod("map", AttributesMapper.class);
            Object mappedSearch = map.invoke(search, mapper);
            Class<?> mappedSearchSpec = Class.forName("org.springframework.ldap.core.LdapClient$MappedSearchSpec");
            Method list = mappedSearchSpec.getMethod("list");
            return (List<String>) list.invoke(mappedSearch);
        } catch (NoSuchMethodException ignored) {
            return search.toList(mapper);
        } catch (InvocationTargetException failure) {
            throw failure.getCause();
        }
    }

    private static LdapQuery aliceQuery() {
        return LdapQueryBuilder.query().base(ORGANIZATION_UNIT_DN).where("uid").is("alice");
    }

    private static AttributesMapper<String> uidMapper() {
        return attributes -> String.valueOf(attributes.get("uid").get());
    }

    private static Attributes organizationUnitAttributes() {
        BasicAttributes attributes = new BasicAttributes(true);
        Attribute objectClass = new BasicAttribute("objectClass");
        objectClass.add("top");
        objectClass.add("organizationalUnit");
        attributes.put(objectClass);
        attributes.put("ou", "skywalking");
        return attributes;
    }

    private static Attributes personAttributes(String commonName, String surname, String uid) {
        BasicAttributes attributes = new BasicAttributes(true);
        Attribute objectClass = new BasicAttribute("objectClass");
        objectClass.add("top");
        objectClass.add("person");
        objectClass.add("organizationalPerson");
        objectClass.add("inetOrgPerson");
        attributes.put(objectClass);
        attributes.put("cn", commonName);
        attributes.put("sn", surname);
        attributes.put("uid", uid);
        attributes.put("userPassword", USER_PASSWORD);
        return attributes;
    }

    private static ModificationItem replaceAttribute(String name, String value) {
        return new ModificationItem(DirContext.REPLACE_ATTRIBUTE, new BasicAttribute(name, value));
    }

    private static void require(boolean condition, String message) {
        if (!condition) {
            throw new IllegalStateException(message);
        }
    }

    private void cleanupRaw() {
        DirContext context = null;
        try {
            context = contextSource.getReadWriteContext();
            unbindQuietly(context, TEMP_USER_DN);
            unbindQuietly(context, RENAMED_USER_DN);
            unbindQuietly(context, USER_DN);
            unbindQuietly(context, ORGANIZATION_UNIT_DN);
        } catch (RuntimeException failure) {
            failure.printStackTrace();
        } finally {
            LdapUtils.closeContext(context);
        }
    }

    private static void unbindQuietly(DirContext context, String name) {
        try {
            context.unbind(name);
        } catch (NameNotFoundException ignored) {
            // The test entry is already absent.
        } catch (NamingException failure) {
            throw new IllegalStateException("Failed to remove test LDAP entry", failure);
        }
    }
}
