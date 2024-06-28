package org.apache.skywalking.apm.plugin.solon;

import org.apache.skywalking.apm.agent.core.boot.PluginConfig;

public class SolonPluginConfig {

    public static class Plugin {
        @PluginConfig(root = SolonPluginConfig.class)
        public static class Solon {
            /**
             * save parameter string length, -1 save all, 0 save nothing, >0 save specified length of parameter string to the tag. default is 0
             */
            public static int HTTP_PARAMS_LENGTH_THRESHOLD = 0;
            /**
             * save header string length, -1 save all, 0 save nothing, >0 save specified length of header string to the tag. default is 0
             */
            public static int HTTP_HEADERS_LENGTH_THRESHOLD = 0;
            /**
             * save body string length, -1 save all, 0 save nothing, >0 save specified length of body string to the tag. default is 0
             */
            public static int HTTP_BODY_LENGTH_THRESHOLD = 0;
            /**
             * intercept class name, default is org.noear.solon.core.mvc.ActionDefault
             */
            public static String INTERCEPT_CLASS_NAME = "org.noear.solon.core.mvc.ActionDefault";
            /**
             * intercept method name, default is invoke
             */
            public static String INTERCEPT_METHOD_NAME = "invoke";
            /**
             * is after exception handling, default is false, if true, the plugin will intercept the method after the exception handling
             */
            public static boolean AFTER_EXCEPTION_HANDLING = false;
        }
    }
}
