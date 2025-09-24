package org.apache.skywalking.apm.plugin.spring.mvc.commons;


import java.util.Enumeration;
import java.util.Map;

public class HttpServletRequestWrappers {

    public static HttpServletRequestWrapper wrap(jakarta.servlet.http.HttpServletRequest request) {
        return new JakartaHttpServletRequest(request);
    }

    public static HttpServletRequestWrapper wrap(javax.servlet.http.HttpServletRequest request) {
        return new JavaxHttpServletRequest(request);
    }

    public static class JakartaHttpServletRequest implements HttpServletRequestWrapper {

        private jakarta.servlet.http.HttpServletRequest jakartaRequest;

        public JakartaHttpServletRequest(jakarta.servlet.http.HttpServletRequest jakartaRequest) {
            this.jakartaRequest = jakartaRequest;
        }

        @Override
        public String getHeader(String name) {
            return jakartaRequest.getHeader(name);
        }

        @Override
        public String getMethod() {
            return jakartaRequest.getMethod();
        }

        @Override
        public StringBuffer getRequestURL() {
            return jakartaRequest.getRequestURL();
        }

        @Override
        public String getRemoteHost() {
            return jakartaRequest.getRemoteHost();
        }

        @Override
        public Map<String, String[]> getParameterMap() {
            return jakartaRequest.getParameterMap();
        }

        @Override
        public Enumeration<String> getHeaders(String name) {
            return jakartaRequest.getHeaders(name);
        }


    }

    public static class JavaxHttpServletRequest implements HttpServletRequestWrapper {
        private javax.servlet.http.HttpServletRequest javaxRequest;

        public JavaxHttpServletRequest(javax.servlet.http.HttpServletRequest javaxRequest) {
            this.javaxRequest = javaxRequest;
        }

        @Override
        public String getHeader(String name) {
            return javaxRequest.getHeader(name);
        }

        @Override
        public String getMethod() {
            return javaxRequest.getMethod();
        }

        @Override
        public StringBuffer getRequestURL() {
            return javaxRequest.getRequestURL();
        }

        @Override
        public String getRemoteHost() {
            return javaxRequest.getRemoteHost();
        }

        @Override
        public Map<String, String[]> getParameterMap() {
            return javaxRequest.getParameterMap();
        }

        @Override
        public Enumeration<String> getHeaders(String name) {
            return javaxRequest.getHeaders(name);
        }


    }
}
