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

import net.bytebuddy.asm.AsmVisitorWrapper;
import net.bytebuddy.description.field.FieldDescription;
import net.bytebuddy.description.field.FieldList;
import net.bytebuddy.description.method.MethodList;
import net.bytebuddy.description.type.TypeDescription;
import net.bytebuddy.implementation.Implementation;
import net.bytebuddy.jar.asm.ClassVisitor;
import net.bytebuddy.jar.asm.FieldVisitor;
import net.bytebuddy.jar.asm.MethodVisitor;
import net.bytebuddy.jar.asm.Opcodes;
import net.bytebuddy.pool.TypePool;

import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;

/**
 * Remove duplicated fields of instrumentedType
 */
public class SWAsmVisitorWrapper implements AsmVisitorWrapper {

    private String nameTrait = "$";

    public SWAsmVisitorWrapper() {
    }

    @Override
    public int mergeWriter(int flags) {
        return flags;
    }

    @Override
    public int mergeReader(int flags) {
        return flags;
    }

    @Override
    public ClassVisitor wrap(TypeDescription instrumentedType, ClassVisitor classVisitor, Implementation.Context implementationContext,
                             TypePool typePool, FieldList<FieldDescription.InDefinedShape> fields, MethodList<?> methods,
                             int writerFlags, int readerFlags) {
        if (classVisitor instanceof RemoveDuplicatedElementsVisitor) {
            return classVisitor;
        }
        return new RemoveDuplicatedElementsVisitor(Opcodes.ASM8, classVisitor);
    }

    class RemoveDuplicatedElementsVisitor extends ClassVisitor {

        private Map<String, Map<String, String>> fieldCache = new ConcurrentHashMap<>();
        private Map<String, Map<String, List<String>>> methodCache = new ConcurrentHashMap<>();
        private String className;

        public RemoveDuplicatedElementsVisitor(int api, ClassVisitor classVisitor) {
            super(api, classVisitor);
        }

        @Override
        public void visit(int version, int access, String name, String signature, String superName, String[] interfaces) {
            super.visit(version, access, name, signature, superName, interfaces);
            this.className = name;
        }

        @Override
        public FieldVisitor visitField(int access, String name, String descriptor, String signature, Object value) {
            if (name.contains(nameTrait)) {
                Map<String, String> fieldData = getFieldData(className);
                String prevDescriptor = fieldData.get(name);
                if (prevDescriptor != null && prevDescriptor.equals(descriptor)) {
                    // ignore duplicated field of class
                    return null;
                }
                fieldData.put(name, descriptor);
            }
            return super.visitField(access, name, descriptor, signature, value);
        }

        @Override
        public MethodVisitor visitMethod(int access, String name, String descriptor, String signature, String[] exceptions) {
            if (name.equals("<init>") || name.contains(nameTrait)) {
                Map<String, List<String>> methodData = getMethodData(className);
                List<String> descriptorList = methodData.computeIfAbsent(name, k -> new ArrayList<>());
                if (descriptorList.contains(descriptor)) {
                    // ignore duplicated method of class
                    return null;
                }
                descriptorList.add(descriptor);
            }
            return super.visitMethod(access, name, descriptor, signature, exceptions);
        }

        private Map<String, String> getFieldData(String className) {
            return fieldCache.computeIfAbsent(className, k -> new ConcurrentHashMap<>());
        }

        private Map<String, List<String>> getMethodData(String className) {
            return methodCache.computeIfAbsent(className, k -> new ConcurrentHashMap<>());
        }
    }
}
