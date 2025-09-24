package org.apache.skywalking.apm.plugin.spring.mvc.commons;


import java.util.Enumeration;
import java.util.Map;

public interface HttpServletRequestWrapper {

    String getHeader(String name);

    String getMethod();

    StringBuffer getRequestURL();

    String getRemoteHost();

    Map<String, String[]> getParameterMap();

    public Enumeration<String> getHeaders(String name);
}
