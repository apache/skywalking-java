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

import java.lang.instrument.ClassFileTransformer;
import java.security.ProtectionDomain;

public class SWExtractionClassFileTransformer implements ClassFileTransformer {

    /**
     * An indicator that an attempted class file transformation did not alter the handed class file.
     */
    private static final byte[] DO_NOT_TRANSFORM = null;

    /**
     * The name of the type to look up.
     */
    private final String typeName;

    /**
     * The binary representation of the looked-up class.
     */
    private volatile byte[] binaryRepresentation;

    /**
     * Creates a class file transformer for the purpose of extraction.
     *
     * @param typeName The name of the type to look up.
     */
    public SWExtractionClassFileTransformer(String typeName) {
        this.typeName = typeName;
    }

    @Override
    public byte[] transform(ClassLoader classLoader,
                            String internalName,
                            Class<?> redefinedType,
                            ProtectionDomain protectionDomain,
                            byte[] binaryRepresentation) {
        if (internalName != null && typeName.equals(internalName.replace('/', '.'))) {
            this.binaryRepresentation = binaryRepresentation.clone();
        }
        return DO_NOT_TRANSFORM;
    }

    /**
     * Returns the binary representation of the class file that was looked up. The returned array must never be modified.
     *
     * @return The binary representation of the class file or {@code null} if no such class file could
     * be located.
     */
    public byte[] getBinaryRepresentation() {
        return binaryRepresentation;
    }
}