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

package org.apache.skywalking.apm.util;

import org.junit.Assert;
import org.junit.Before;
import org.junit.Test;

import java.util.List;
import java.util.Properties;
import java.util.Set;

public class ConfigInitializerTest {
    @Test
    public void testInitialize() throws IllegalAccessException {
        Properties properties = new Properties();
        properties.put("Level1Object.STR_ATTR".toLowerCase(), "stringValue");
        properties.put("Level1Object.Level2Object.INT_ATTR".toLowerCase(), "1000");
        properties.put("Level1Object.Level2Object.LONG_ATTR".toLowerCase(), "1000");
        properties.put("Level1Object.Level2Object.BOOLEAN_ATTR".toLowerCase(), "true");
        properties.put("Level1Object.LIST_STR_ATTR".toLowerCase(), "a,b,c");
        properties.put("Level1Object.LIST_INT_ATTR".toLowerCase(), "1,2,3");
        properties.put("Level1Object.LIST_STR_EMPTY_ATTR".toLowerCase(), "");
        properties.put("Level1Object.LIST_BOOL_ATTR".toLowerCase(), "true,false");
        properties.put("Level1Object.Level2Object.ENUM_ATTR".toLowerCase(), "RED");
        properties.put("Level1Object.SET_STR_ATTR".toLowerCase(), "a,b,c,d,b");
        properties.put("Level1Object.SET_STR_EMPTY_ATTR".toLowerCase(), "");
        properties.put("Level1Object.SET_INT_ATTR".toLowerCase(), "1,2,3,4,4");
        properties.put("Level1Object.SET_BOOL_ATTR".toLowerCase(), "true,true");

        ConfigInitializer.initialize(properties, TestPropertiesObject.class);

        Assert.assertEquals("stringValue", TestPropertiesObject.Level1Object.STR_ATTR);
        Assert.assertEquals(1000, TestPropertiesObject.Level1Object.Level2Object.INT_ATTR);
        Assert.assertEquals(1000L, TestPropertiesObject.Level1Object.Level2Object.LONG_ATTR);
        Assert.assertTrue(TestPropertiesObject.Level1Object.Level2Object.BOOLEAN_ATTR);
        Assert.assertArrayEquals(new String[]{}, TestPropertiesObject.Level1Object.LIST_STR_EMPTY_ATTR.toArray());
        Assert.assertArrayEquals(new String[]{"a", "b", "c"}, TestPropertiesObject.Level1Object.LIST_STR_ATTR.toArray());
        Assert.assertArrayEquals(new Integer[]{1, 2, 3}, TestPropertiesObject.Level1Object.LIST_INT_ATTR.toArray());
        Assert.assertArrayEquals(new String[]{}, TestPropertiesObject.Level1Object.SET_STR_EMPTY_ATTR.toArray());
        Assert.assertArrayEquals(new Boolean[]{true, false}, TestPropertiesObject.Level1Object.LIST_BOOL_ATTR.toArray());
        Assert.assertArrayEquals(new String[]{"a", "b", "c", "d"}, TestPropertiesObject.Level1Object.SET_STR_ATTR.toArray());
        Assert.assertArrayEquals(new Integer[]{1, 2, 3, 4}, TestPropertiesObject.Level1Object.SET_INT_ATTR.toArray());
        Assert.assertArrayEquals(new Boolean[]{true}, TestPropertiesObject.Level1Object.SET_BOOL_ATTR.toArray());
        Assert.assertEquals(TestColorEnum.RED, TestPropertiesObject.Level1Object.Level2Object.ENUM_ATTR);
        //make sure that when descs is empty,toString() work right;
        Assert.assertEquals(new ConfigDesc().toString(), "");
    }

    @Test
    public void testInitializeWithUnsupportedConfig() throws IllegalAccessException {
        Properties properties = new Properties();
        properties.put("Level1Object.noExistAttr".toLowerCase(), "stringValue");

        ConfigInitializer.initialize(properties, TestPropertiesObject.class);

        Assert.assertNull(TestPropertiesObject.Level1Object.STR_ATTR);
    }

    @Before
    public void clear() {
        TestPropertiesObject.Level1Object.STR_ATTR = null;
        TestPropertiesObject.Level1Object.LIST_STR_ATTR = null;
        TestPropertiesObject.Level1Object.LIST_STR_EMPTY_ATTR = null;
        TestPropertiesObject.Level1Object.LIST_INT_ATTR = null;
        TestPropertiesObject.Level1Object.LIST_BOOL_ATTR = null;
        TestPropertiesObject.Level1Object.SET_STR_ATTR = null;
        TestPropertiesObject.Level1Object.SET_STR_EMPTY_ATTR = null;
        TestPropertiesObject.Level1Object.SET_INT_ATTR = null;
        TestPropertiesObject.Level1Object.SET_BOOL_ATTR = null;
        TestPropertiesObject.Level1Object.Level2Object.INT_ATTR = 0;
        TestPropertiesObject.Level1Object.Level2Object.LONG_ATTR = 0;
        TestPropertiesObject.Level1Object.Level2Object.BOOLEAN_ATTR = false;
        TestPropertiesObject.Level1Object.Level2Object.ENUM_ATTR = null;
    }

    public static class TestPropertiesObject {
        public static class Level1Object {
            public static String STR_ATTR = null;
            public static List<String> LIST_STR_ATTR = null;
            public static List<String> LIST_STR_EMPTY_ATTR = null;
            public static List<Integer> LIST_INT_ATTR = null;
            public static List<Boolean> LIST_BOOL_ATTR = null;
            public static Set<String> SET_STR_ATTR = null;
            public static Set<String> SET_STR_EMPTY_ATTR = null;
            public static Set<Integer> SET_INT_ATTR = null;
            public static Set<Boolean> SET_BOOL_ATTR = null;

            public static class Level2Object {
                public static int INT_ATTR = 0;

                public static long LONG_ATTR;

                public static boolean BOOLEAN_ATTR;

                public static TestColorEnum ENUM_ATTR;
            }
        }
    }

    private enum TestColorEnum {
        RED, BLACK;
    }
}
