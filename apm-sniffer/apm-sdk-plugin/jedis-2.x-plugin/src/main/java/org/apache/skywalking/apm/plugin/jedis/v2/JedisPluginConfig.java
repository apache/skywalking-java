package org.apache.skywalking.apm.plugin.jedis.v2;

import org.apache.skywalking.apm.agent.core.boot.PluginConfig;

public class JedisPluginConfig {
    public static class Plugin {
        @PluginConfig(root = JedisPluginConfig.class)
        public static class Jedis {
            /**
             * If set to true, the parameters of the Redis command would be collected.
             */
            public static boolean TRACE_REDIS_PARAMETERS = false;
            /**
             * For the sake of performance, SkyWalking won't save Redis parameter string into the tag.
             * If TRACE_REDIS_PARAMETERS is set to true, the first {@code REDIS_PARAMETER_MAX_LENGTH} parameter
             * characters would be collected.
             * <p>
             * Set a negative number to save specified length of parameter string to the tag.
             */
            public static int REDIS_PARAMETER_MAX_LENGTH = 128;
        }
    }
}
