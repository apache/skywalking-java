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

package org.apache.skywalking.apm.agent.bytebuddy;

import net.bytebuddy.description.type.TypeDescription;
import net.bytebuddy.implementation.auxiliary.AuxiliaryType;
import net.bytebuddy.utility.RandomString;

/**
 * Generate predicated auxiliary type name for delegate method.
 */
public class SWAuxiliaryTypeNamingStrategy implements AuxiliaryType.NamingStrategy {
    private static final String DEFAULT_SUFFIX = "auxiliary$";
    private String suffix;

    public SWAuxiliaryTypeNamingStrategy(String nameTrait) {
        this.suffix = nameTrait + DEFAULT_SUFFIX;
    }

    @Override
    public String name(TypeDescription instrumentedType, AuxiliaryType auxiliaryType) {
        // Auxiliary type name pattern: <origin_class_name>$<name_trait>$auxiliary$<auxiliary_type_instance_hash>
        return instrumentedType.getName() + "$" + suffix + RandomString.hashOf(auxiliaryType.hashCode());
    }

}
