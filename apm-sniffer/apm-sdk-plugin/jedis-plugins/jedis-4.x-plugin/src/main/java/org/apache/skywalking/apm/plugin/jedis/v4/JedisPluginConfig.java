/*
 *   Licensed to the Apache Software Foundation (ASF) under one or more
 *   contributor license agreements.  See the NOTICE file distributed with
 *   this work for additional information regarding copyright ownership.
 *   The ASF licenses this file to You under the Apache License, Version 2.0
 *   (the "License"); you may not use this file except in compliance with
 *   the License.  You may obtain a copy of the License at
 *
 *       http://www.apache.org/licenses/LICENSE-2.0
 *
 *   Unless required by applicable law or agreed to in writing, software
 *   distributed under the License is distributed on an "AS IS" BASIS,
 *   WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 *   See the License for the specific language governing permissions and
 *   limitations under the License.
 */

package org.apache.skywalking.apm.plugin.jedis.v4;

import org.apache.skywalking.apm.agent.core.boot.PluginConfig;

import java.util.Arrays;
import java.util.HashSet;
import java.util.Set;

public class JedisPluginConfig {

    public static class Plugin {
        @PluginConfig(root = JedisPluginConfig.class)
        public static class Jedis {
            /**
             * If set to true, the parameters of the Redis command would be collected.
             */
            public static boolean TRACE_REDIS_PARAMETERS = true;
            /**
             * For the sake of performance, SkyWalking won't save Redis parameter string into the tag.
             * If TRACE_REDIS_PARAMETERS is set to true, the first {@code REDIS_PARAMETER_MAX_LENGTH} parameter
             * characters would be collected.
             * <p>
             * Set a negative number to save specified length of parameter string to the tag.
             */
            public static int REDIS_PARAMETER_MAX_LENGTH = 128;


            /**
             * Operation represent a cache span is "write" or "read" action , and "op"(operation) is tagged with key "cache.op" usually
             * This config term define which command should be converted to write Operation .
             *
             * @see org.apache.skywalking.apm.agent.core.context.tag.Tags#CACHE_OP
             * @see AbstractConnectionInterceptor#parseOperation(String)
             */
            public static Set<String> OPERATION_MAPPING_WRITE = new HashSet<>(Arrays.asList(
                    "GETSET",
                    "SET",
                    "SETBIT",
                    "SETEX ",
                    "SETNX ",
                    "SETRANGE",
                    "STRLEN ",
                    "MSET",
                    "MSETNX ",
                    "PSETEX",
                    "INCR ",
                    "INCRBY ",
                    "INCRBYFLOAT",
                    "DECR ",
                    "DECRBY ",
                    "APPEND ",
                    "HMSET",
                    "HSET",
                    "HSETNX ",
                    "HINCRBY",
                    "HINCRBYFLOAT",
                    "HDEL",
                    "RPOPLPUSH",
                    "RPUSH",
                    "RPUSHX",
                    "LPUSH",
                    "LPUSHX",
                    "LREM",
                    "LTRIM",
                    "LSET",
                    "BRPOPLPUSH",
                    "LINSERT",
                    "SADD",
                    "SDIFF",
                    "SDIFFstore",
                    "SINTERSTORE",
                    "SISMEMBER",
                    "SREM",
                    "SUNION",
                    "SUNIONSTORE",
                    "SINTER",
                    "ZADD",
                    "ZINCRBY",
                    "ZINTERSTORE",
                    "ZRANGE",
                    "ZRANGEBYLEX",
                    "ZRANGEBYSCORE",
                    "ZRANK",
                    "ZREM",
                    "ZREMRANGEBYLEX",
                    "ZREMRANGEBYRANK",
                    "ZREMRANGEBYSCORE",
                    "ZREVRANGE",
                    "ZREVRANGEBYSCORE",
                    "ZREVRANK",
                    "ZUNIONSTORE",
                    "XADD",
                    "XDEL",
                    "DEL",
                    "XTRIM"
            ));
            /**
             * Operation represent a cache span is "write" or "read" action , and "op"(operation) is tagged with key "cache.op" usually
             * This config term define which command should be converted to write Operation .
             *
             * @see org.apache.skywalking.apm.agent.core.context.tag.Tags#CACHE_OP
             * @see AbstractConnectionInterceptor#parseOperation(String)
             */
            public static Set<String> OPERATION_MAPPING_READ = new HashSet<>(Arrays.asList("GET",
                    "GETRANGE",
                    "GETBIT ",
                    "MGET",
                    "HVALS",
                    "HKEYS",
                    "HLEN",
                    "HEXISTS",
                    "HGET",
                    "HGETALL",
                    "HMGET",
                    "BLPOP",
                    "BRPOP",
                    "LINDEX",
                    "LLEN",
                    "LPOP",
                    "LRANGE",
                    "RPOP",
                    "SCARD",
                    "SRANDMEMBER",
                    "SPOP",
                    "SSCAN",
                    "SMOVE",
                    "ZLEXCOUNT",
                    "ZSCORE",
                    "ZSCAN",
                    "ZCARD",
                    "ZCOUNT",
                    "XGET",
                    "GET",
                    "XREAD",
                    "XLEN",
                    "XRANGE",
                    "XREVRANGE"
            ));

        }
    }
}
