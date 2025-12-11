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

/*
 * Provides an abstraction so the BeanELResolver can obtain JavaBeans specification support via different
 * implementations.
 */
abstract class BeanSupport {

    // For testing purposes only.
    static BeanSupport beanSupport;

    static {
        // Only intended for unit tests. Not intended to be part of public API.
        boolean doNotCacheInstance = Boolean.getBoolean("jakarta.el.BeanSupport.doNotCacheInstance");
        if (doNotCacheInstance) {
            beanSupport = null;
        } else {
            beanSupport = createInstance();
        }
    }

    private static BeanSupport createInstance() {
        // Only intended for unit tests. Not intended to be part of public API.
        boolean useFull = !Boolean.getBoolean("jakarta.el.BeanSupport.useStandalone");

        if (useFull) {
            // If not explicitly configured to use standalone, use the full implementation unless it is not available.
            try {
                Class.forName("java.beans.BeanInfo");
            } catch (Exception e) {
                // Ignore: Expected if using modules and java.desktop module is not present
                useFull = false;
            }
        }
        if (useFull) {
            // The full implementation provided by the java.beans package
            return new BeanSupportFull();
        } else {
            // The cut-down local implementation that does not depend on the java.beans package
            return new BeanSupportStandalone();
        }
    }

    static BeanSupport getInstance() {
        if (beanSupport == null) {
            return createInstance();
        }
        return beanSupport;
    }

    abstract BeanELResolver.BeanProperties getBeanProperties(Class<?> type);
}
