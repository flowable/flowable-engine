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

import java.lang.reflect.Method;
import java.lang.reflect.Modifier;
import java.util.ArrayList;
import java.util.Comparator;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

/*
 * Implements those parts of the JavaBeans Specification that can be implemented without reference to the java.beans
 * package.
 */
class BeanSupportStandalone extends BeanSupport {

    /*
     * The full JavaBeans implementation has a much more detailed definition of method order that applies to an entire
     * class. When ordering write methods for a single property, a much simpler comparator can be used because it is
     * known that the method names are the same, the return parameters are both void and the methods only have a single
     * parameter.
     */
    private static final Comparator<Method> WRITE_METHOD_COMPARATOR =
            Comparator.comparing(m -> m.getParameterTypes()[0].getName());

    @Override
    BeanELResolver.BeanProperties getBeanProperties(Class<?> type) {
        return new BeanPropertiesStandalone(type);
    }

    private static PropertyDescriptor[] getPropertyDescriptors(Class<?> type) {
        Map<String, PropertyDescriptor> pds = new HashMap<>();
        Method[] methods = type.getMethods();
        for (Method method : methods) {
            if (!Modifier.isStatic(method.getModifiers())) {
                String methodName = method.getName();
                if (methodName.startsWith("is")) {
                    if (method.getParameterCount() == 0 && method.getReturnType() == boolean.class) {
                        String propertyName = getPropertyName(methodName.substring(2));
                        PropertyDescriptor pd = pds.computeIfAbsent(propertyName, k -> new PropertyDescriptor());
                        pd.setName(propertyName);
                        pd.setReadMethodIs(method);
                    }
                } else if (methodName.startsWith("get")) {
                    if (method.getParameterCount() == 0) {
                        String propertyName = getPropertyName(methodName.substring(3));
                        PropertyDescriptor pd = pds.computeIfAbsent(propertyName, k -> new PropertyDescriptor());
                        pd.setName(propertyName);
                        pd.setReadMethod(method);
                    }
                } else if (methodName.startsWith("set")) {
                    if (method.getParameterCount() == 1 && method.getReturnType() == void.class) {
                        String propertyName = getPropertyName(methodName.substring(3));
                        PropertyDescriptor pd = pds.computeIfAbsent(propertyName, k -> new PropertyDescriptor());
                        pd.setName(propertyName);
                        pd.addWriteMethod(method);
                    }

                }
            }
        }
        return pds.values().toArray(new PropertyDescriptor[0]);
    }

    private static String getPropertyName(String input) {
        if (input.isEmpty()) {
            return null;
        }
        if (!Character.isUpperCase(input.charAt(0))) {
            return null;
        }
        if (input.length() > 1 && Character.isUpperCase(input.charAt(1))) {
            return input;
        }
        char[] chars = input.toCharArray();
        chars[0] = Character.toLowerCase(chars[0]);
        return new String(chars);
    }

    private static class PropertyDescriptor {

        private String name;
        private boolean usesIs;
        private Method readMethod;
        private Method writeMethod;
        private final List<Method> writeMethods = new ArrayList<>();

        String getName() {
            return name;
        }

        void setName(String name) {
            this.name = name;
        }

        Class<?> getType() {
            if (readMethod == null) {
                return getWriteMethod().getParameterTypes()[0];
            }
            return readMethod.getReturnType();
        }

        Method getReadMethod() {
            return readMethod;
        }

        void setReadMethod(Method readMethod) {
            if (usesIs) {
                return;
            }
            this.readMethod = readMethod;
        }

        void setReadMethodIs(Method readMethod) {
            this.readMethod = readMethod;
            this.usesIs = true;
        }

        Method getWriteMethod() {
            if (writeMethod == null) {
                Class<?> type;
                if (readMethod != null) {
                    type = readMethod.getReturnType();
                } else {
                    if (writeMethods.size() > 1) {
                        writeMethods.sort(WRITE_METHOD_COMPARATOR);
                    }
                    type = writeMethods.get(0).getParameterTypes()[0];
                }
                for (Method candidate : writeMethods) {
                    if (type.isAssignableFrom(candidate.getParameterTypes()[0])) {
                        type = candidate.getParameterTypes()[0];
                        this.writeMethod = candidate;
                    }
                }
            }
            return writeMethod;
        }

        void addWriteMethod(Method writeMethod) {
            this.writeMethods.add(writeMethod);
        }
    }

    static final class BeanPropertiesStandalone extends BeanELResolver.BeanProperties {

        BeanPropertiesStandalone(Class<?> type) throws ELException {
            super(type);
            PropertyDescriptor[] pds = getPropertyDescriptors(this.type);
            for (PropertyDescriptor pd : pds) {
                this.properties.put(pd.getName(), new BeanPropertyStandalone(type, pd));
            }
            /*
             * Populating from any interfaces causes default methods to be included.
             */
            populateFromInterfaces(type);
        }

        private void populateFromInterfaces(Class<?> aClass) {
            Class<?>[] interfaces = aClass.getInterfaces();
            for (Class<?> ifs : interfaces) {
                PropertyDescriptor[] pds = getPropertyDescriptors(type);
                for (PropertyDescriptor pd : pds) {
                    if (!this.properties.containsKey(pd.getName())) {
                        this.properties.put(pd.getName(), new BeanPropertyStandalone(this.type, pd));
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

    static final class BeanPropertyStandalone extends BeanELResolver.BeanProperty {

        private final String name;
        private final Method readMethod;
        private final Method writeMethod;

        BeanPropertyStandalone(Class<?> owner, PropertyDescriptor pd) {
            super(owner, pd.getType());
            name = pd.getName();
            readMethod = pd.getReadMethod();
            writeMethod = pd.getWriteMethod();
        }

        @Override
        String getName() {
            return name;
        }

        @Override
        Method getReadMethod() {
            return readMethod;
        }

        @Override
        Method getWriteMethod() {
            return writeMethod;
        }
    }
}
