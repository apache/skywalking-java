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
import org.mockito.internal.util.collections.Sets;

import java.util.Arrays;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
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
        properties.put("Level1Object.LIST_INT_ATTR_ED".toLowerCase(), ""); // clear list
        properties.put("Level1Object.Level2Object.ENUM_ATTR".toLowerCase(), "RED");
        properties.put("Level1Object.SET_STR_ATTR".toLowerCase(), "a,b,c,d,b");
        properties.put("Level1Object.SET_STR_EMPTY_ATTR".toLowerCase(), "");
        properties.put("Level1Object.SET_INT_ATTR".toLowerCase(), "1,2,3,4,4");
        properties.put("Level1Object.SET_BOOL_ATTR".toLowerCase(), "true,true");
        properties.put("Level1Object.MAP_1[a]".toLowerCase(), "1");
        properties.put("Level1Object.MAP_1[b]".toLowerCase(), "2");
        properties.put("Level1Object.MAP_2[]".toLowerCase(), ""); // clear map
        properties.put("Level1Object.MAP_4[c]".toLowerCase(), "3");

        ConfigInitializer.initialize(properties, TestPropertiesObject.class);

        Assert.assertEquals("stringValue", TestPropertiesObject.Level1Object.STR_ATTR);
        Assert.assertEquals(1000, TestPropertiesObject.Level1Object.Level2Object.INT_ATTR);
        Assert.assertEquals(1000, TestPropertiesObject.Level1Object.Level2Object.INT_ATTR_100);
        Assert.assertEquals(1000L, TestPropertiesObject.Level1Object.Level2Object.LONG_ATTR);
        Assert.assertTrue(TestPropertiesObject.Level1Object.Level2Object.BOOLEAN_ATTR);
        Assert.assertTrue(TestPropertiesObject.Level1Object.Level2Object.BOOLEAN_ATTR_TRUE);
        Assert.assertArrayEquals(new String[]{}, TestPropertiesObject.Level1Object.LIST_STR_EMPTY_ATTR.toArray());
        Assert.assertArrayEquals(new String[]{"a", "b", "c"}, TestPropertiesObject.Level1Object.LIST_STR_ATTR.toArray());
        Assert.assertArrayEquals(new Integer[]{1, 2, 3}, TestPropertiesObject.Level1Object.LIST_INT_ATTR.toArray());
        Assert.assertArrayEquals(new Integer[]{}, TestPropertiesObject.Level1Object.LIST_INT_ATTR_ED.toArray());
        Assert.assertArrayEquals(new Integer[]{1, 2, 3, 4}, TestPropertiesObject.Level1Object.LIST_INT_ATTR_ED2.toArray());
        Assert.assertArrayEquals(new String[]{}, TestPropertiesObject.Level1Object.SET_STR_EMPTY_ATTR.toArray());
        Assert.assertArrayEquals(new Boolean[]{true, false}, TestPropertiesObject.Level1Object.LIST_BOOL_ATTR.toArray());
        Assert.assertEquals(Sets.newSet("a", "b", "c", "d"), TestPropertiesObject.Level1Object.SET_STR_ATTR);
        Assert.assertEquals(Sets.newSet(1, 2, 3, 4), TestPropertiesObject.Level1Object.SET_INT_ATTR);
        Assert.assertArrayEquals(new Boolean[]{true}, TestPropertiesObject.Level1Object.SET_BOOL_ATTR.toArray());
        Assert.assertEquals(TestColorEnum.RED, TestPropertiesObject.Level1Object.Level2Object.ENUM_ATTR);
        //make sure that when descs is empty,toString() work right;
        Assert.assertEquals(new ConfigDesc().toString(), "");

        HashMap<String, Integer> objectObjectHashMap = new HashMap<>();
        objectObjectHashMap.put("a", 1);
        objectObjectHashMap.put("b", 2);
        Assert.assertEquals(TestPropertiesObject.Level1Object.MAP_1, objectObjectHashMap);
        Assert.assertEquals(TestPropertiesObject.Level1Object.MAP_2, new HashMap<>());

        Assert.assertEquals(TestPropertiesObject.Level1Object.MAP_3.size(), 2);
        Assert.assertEquals(TestPropertiesObject.Level1Object.MAP_3.get("a"), Integer.valueOf(1));
        Assert.assertEquals(TestPropertiesObject.Level1Object.MAP_3.get("b"), Integer.valueOf(2));

        Assert.assertEquals(Integer.valueOf(TestPropertiesObject.Level1Object.MAP_4.size()), Integer.valueOf(1));
        Assert.assertEquals(TestPropertiesObject.Level1Object.MAP_4.get("c"), Integer.valueOf(3));
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
        TestPropertiesObject.Level1Object.LIST_INT_ATTR_ED = Arrays.asList(1, 2, 3, 4);
        TestPropertiesObject.Level1Object.LIST_INT_ATTR_ED2 = Arrays.asList(1, 2, 3, 4);
        TestPropertiesObject.Level1Object.Level2Object.INT_ATTR = 0;
        TestPropertiesObject.Level1Object.Level2Object.LONG_ATTR = 0;
        TestPropertiesObject.Level1Object.Level2Object.BOOLEAN_ATTR = false;
        TestPropertiesObject.Level1Object.Level2Object.ENUM_ATTR = null;
        TestPropertiesObject.Level1Object.Level2Object.BOOLEAN_ATTR_TRUE = true;
        TestPropertiesObject.Level1Object.Level2Object.INT_ATTR_100 = 1000;
    }

    public static class TestPropertiesObject {
        public static class Level1Object {
            public static String STR_ATTR = null;
            public static List<String> LIST_STR_ATTR = null;
            public static List<String> LIST_STR_EMPTY_ATTR = null;
            public static List<Integer> LIST_INT_ATTR = null;
            public static List<Integer> LIST_INT_ATTR_ED = Arrays.asList(1, 2, 3, 4);
            public static List<Integer> LIST_INT_ATTR_ED2 = Arrays.asList(1, 2, 3, 4);
            public static List<Boolean> LIST_BOOL_ATTR = null;
            public static Set<String> SET_STR_ATTR = null;
            public static Set<String> SET_STR_EMPTY_ATTR = null;
            public static Set<Integer> SET_INT_ATTR = null;
            public static Set<Boolean> SET_BOOL_ATTR = null;
            public static Map<String, Integer> MAP_1;
            public static Map<String, Integer> MAP_2 = new HashMap<String, Integer>() {
                {
                    put("a", 1);
                    put("b", 2);
                }
            };
            public static Map<String, Integer> MAP_3 = new HashMap<String, Integer>() {
                {
                    put("a", 1);
                    put("b", 2);
                }
            };
            public static Map<String, Integer> MAP_4 = new HashMap<String, Integer>() {
                {
                    put("a", 1);
                    put("b", 2);
                }
            };

            public static class Level2Object {
                public static int INT_ATTR = 0;

                public static int INT_ATTR_100 = 1000;

                public static long LONG_ATTR;

                public static boolean BOOLEAN_ATTR;

                public static boolean BOOLEAN_ATTR_TRUE = true;

                public static TestColorEnum ENUM_ATTR;
            }
        }
    }

    private enum TestColorEnum {
        RED, BLACK;
    }
}
