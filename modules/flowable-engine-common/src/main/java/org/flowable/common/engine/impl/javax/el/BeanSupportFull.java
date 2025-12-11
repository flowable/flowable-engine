/*
 * Licensed to the Apache Software Foundation (ASF) under one or more
 * contributor license agreements.  See the NOTICE file distributed with
 * this work for additional information regarding copyright ownership.
 * The ASF licenses this file to You under the Apache License, Version 2.0
 * (the "License"); you may not use this file except in compliance with
 * the License.  You may obtain a copy of the License at
 *
 *      http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */
package org.flowable.common.engine.impl.javax.el;

import java.beans.BeanInfo;
import java.beans.IntrospectionException;
import java.beans.Introspector;
import java.beans.PropertyDescriptor;
import java.lang.reflect.Method;

class BeanSupportFull extends BeanSupport {

    @Override
    BeanELResolver.BeanProperties getBeanProperties(Class<?> type) {
        return new BeanPropertiesFull(type);
    }

    static final class BeanPropertiesFull extends BeanELResolver.BeanProperties {

        BeanPropertiesFull(Class<?> type) throws ELException {
            super(type);
            try {
                BeanInfo info = Introspector.getBeanInfo(this.type);
                PropertyDescriptor[] pds = info.getPropertyDescriptors();
                for (PropertyDescriptor pd : pds) {
                    this.properties.put(pd.getName(), new BeanPropertyFull(type, pd));
                }
                /*
                 * https://bugs.openjdk.org/browse/JDK-8071693 - Introspector ignores default interface methods.
                 *
                 * This bug is fixed in Java 21 b21. This workaround can be removed once the minimum Java version is 21.
                 * Populating from any interfaces causes default methods to be included.
                 */
                populateFromInterfaces(type);
            } catch (IntrospectionException ie) {
                throw new ELException(ie);
            }
        }

        private void populateFromInterfaces(Class<?> aClass) throws IntrospectionException {
            Class<?>[] interfaces = aClass.getInterfaces();
            for (Class<?> ifs : interfaces) {
                BeanInfo info = Introspector.getBeanInfo(ifs);
                PropertyDescriptor[] pds = info.getPropertyDescriptors();
                for (PropertyDescriptor pd : pds) {
                    if (!this.properties.containsKey(pd.getName())) {
                        this.properties.put(pd.getName(), new BeanPropertyFull(this.type, pd));
                    }
                }
                populateFromInterfaces(ifs);
            }
            Class<?> superclass = aClass.getSuperclass();
            if (superclass != null) {
                populateFromInterfaces(superclass);
            }
        }
    }

    static final class BeanPropertyFull extends BeanELResolver.BeanProperty {

        private final PropertyDescriptor descriptor;

        BeanPropertyFull(Class<?> owner, PropertyDescriptor descriptor) {
            super(owner, descriptor.getPropertyType());
            this.descriptor = descriptor;
        }

        @Override
        Method getWriteMethod() {
            return descriptor.getWriteMethod();
        }

        @Override
        Method getReadMethod() {
            return descriptor.getReadMethod();
        }

        @Override
        String getName() {
            return descriptor.getName();
        }
    }
}
