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

public class ImplementationContextFactory implements Implementation.Context.Factory {
    @Override
    public Implementation.Context.ExtractableView make(TypeDescription instrumentedType, AuxiliaryType.NamingStrategy auxiliaryTypeNamingStrategy, TypeInitializer typeInitializer, ClassFileVersion classFileVersion, ClassFileVersion auxiliaryClassFileVersion) {
        return this.make(instrumentedType, auxiliaryTypeNamingStrategy, typeInitializer, classFileVersion, auxiliaryClassFileVersion, Implementation.Context.FrameGeneration.GENERATE);
    }

    @Override
    public Implementation.Context.ExtractableView make(TypeDescription instrumentedType, AuxiliaryType.NamingStrategy auxiliaryTypeNamingStrategy, TypeInitializer typeInitializer, ClassFileVersion classFileVersion, ClassFileVersion auxiliaryClassFileVersion, Implementation.Context.FrameGeneration frameGeneration) {
        return new Implementation.Context.Default(instrumentedType, classFileVersion, auxiliaryTypeNamingStrategy, typeInitializer, auxiliaryClassFileVersion, frameGeneration, RandomString.hashOf(instrumentedType.getName().hashCode()));
    }
}
