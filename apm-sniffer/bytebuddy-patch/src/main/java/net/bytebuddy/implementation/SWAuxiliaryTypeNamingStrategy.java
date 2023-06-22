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

package net.bytebuddy.implementation;

import net.bytebuddy.description.field.FieldDescription;
import net.bytebuddy.description.method.MethodDescription;
import net.bytebuddy.description.type.TypeDescription;
import net.bytebuddy.implementation.Implementation.SpecialMethodInvocation;
import net.bytebuddy.implementation.auxiliary.AuxiliaryType;
import net.bytebuddy.utility.RandomString;
import org.apache.skywalking.apm.agent.core.logging.api.ILog;
import org.apache.skywalking.apm.agent.core.logging.api.LogManager;

import java.lang.reflect.Field;

/**
 * Generate fixed auxiliary type name of delegate method
 */
public class SWAuxiliaryTypeNamingStrategy implements AuxiliaryType.NamingStrategy {
    private static final String DEFAULT_SUFFIX = "auxiliary";
    private static ILog LOGGER = LogManager.getLogger(SWAuxiliaryTypeNamingStrategy.class);
    private String suffix;

    public SWAuxiliaryTypeNamingStrategy(String nameTrait) {
        this.suffix = nameTrait + "_" + DEFAULT_SUFFIX;
    }

    @Override
    public String name(TypeDescription instrumentedType, AuxiliaryType auxiliaryType) {
//        String description = findDescription(auxiliaryType);
//        if (description != null) {
//            return instrumentedType.getName() + "$" + suffix + "$" + RandomString.hashOf(description.hashCode());
//        }
        return instrumentedType.getName() + "$" + suffix + "$" + RandomString.hashOf(auxiliaryType.hashCode());
    }

    private String findDescription(AuxiliaryType auxiliaryType) {
        try {
            Class<? extends AuxiliaryType> auxiliaryTypeClass = auxiliaryType.getClass();
            String auxiliaryTypeClassName = auxiliaryTypeClass.getName();
            if (auxiliaryTypeClassName.endsWith("Morph$Binder$RedirectionProxy")
                    || auxiliaryTypeClassName.endsWith("MethodCallProxy")) {
                // get MethodDescription from field 'specialMethodInvocation.methodDescription'
                Field specialMethodInvocationField = auxiliaryTypeClass.getDeclaredField("specialMethodInvocation");
                specialMethodInvocationField.setAccessible(true);
                SpecialMethodInvocation specialMethodInvocation = (SpecialMethodInvocation) specialMethodInvocationField.get(auxiliaryType);
                MethodDescription methodDescription = specialMethodInvocation.getMethodDescription();
                return methodDescription.toString();

            } else if (auxiliaryTypeClassName.endsWith("Pipe$Binder$RedirectionProxy")) {
                // get MethodDescription from field 'sourceMethod'
                Field sourceMethodField = auxiliaryTypeClass.getDeclaredField("sourceMethod");
                sourceMethodField.setAccessible(true);
                MethodDescription sourceMethod = (MethodDescription) sourceMethodField.get(auxiliaryType);
                return sourceMethod.toString();

            } else if (auxiliaryTypeClassName.endsWith("FieldProxy$Binder$AccessorProxy")) {
                // get fieldDescription
                Field fieldDescriptionField = auxiliaryTypeClass.getDeclaredField("fieldDescription");
                fieldDescriptionField.setAccessible(true);
                FieldDescription fieldDescription = (FieldDescription) fieldDescriptionField.get(auxiliaryType);
                return fieldDescription.toString();
            }
        } catch (Throwable e) {
            LOGGER.error(e, "Find description of auxiliaryType failure. auxiliaryType: {}", auxiliaryType.getClass());
        }
        return null;
    }
}
