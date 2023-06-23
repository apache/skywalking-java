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

import net.bytebuddy.ClassFileVersion;
import net.bytebuddy.description.type.TypeDescription;
import net.bytebuddy.dynamic.scaffold.TypeInitializer;
import net.bytebuddy.implementation.auxiliary.AuxiliaryType;
import net.bytebuddy.utility.RandomString;

/**
 * Support custom suffix name trait, using in cache value field name, field getter/setter delegation, accessor method and so on.
 */
public class SWImplementationContextFactory implements Implementation.Context.Factory {

    private String suffixNameTrait;

    public SWImplementationContextFactory(String suffixNameTrait) {
        this.suffixNameTrait = suffixNameTrait;
    }

    @Override
    public Implementation.Context.ExtractableView make(TypeDescription instrumentedType,
                                                       AuxiliaryType.NamingStrategy auxiliaryTypeNamingStrategy,
                                                       TypeInitializer typeInitializer,
                                                       ClassFileVersion classFileVersion,
                                                       ClassFileVersion auxiliaryClassFileVersion) {
        return this.make(instrumentedType, auxiliaryTypeNamingStrategy, typeInitializer, classFileVersion,
                auxiliaryClassFileVersion, Implementation.Context.FrameGeneration.GENERATE);
    }

    @Override
    public Implementation.Context.ExtractableView make(TypeDescription instrumentedType,
                                                       AuxiliaryType.NamingStrategy auxiliaryTypeNamingStrategy,
                                                       TypeInitializer typeInitializer,
                                                       ClassFileVersion classFileVersion,
                                                       ClassFileVersion auxiliaryClassFileVersion,
                                                       Implementation.Context.FrameGeneration frameGeneration) {
        // Method cache value field pattern: cachedValue$<name_trait>$<origin_class_name_hash>$<field_value_hash>
        // Accessor method name pattern: <renamed_origin_method>$accessor$<name_trait>$<origin_class_name_hash>
        return new Implementation.Context.Default(instrumentedType, classFileVersion, auxiliaryTypeNamingStrategy,
                typeInitializer, auxiliaryClassFileVersion, frameGeneration,
                suffixNameTrait + RandomString.hashOf(instrumentedType.getName().hashCode()));
    }
}
