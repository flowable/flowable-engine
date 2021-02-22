/*
 *  Licensed to the Apache Software Foundation (ASF) under one or more
 *  contributor license agreements.  See the NOTICE file distributed with
 *  this work for additional information regarding copyright ownership.
 *  The ASF licenses this file to You under the Apache License, Version 2.0
 *  (the "License"); you may not use this file except in compliance with
 *  the License.  You may obtain a copy of the License at
 *
 *      http://www.apache.org/licenses/LICENSE-2.0
 *
 *  Unless required by applicable law or agreed to in writing, software
 *  distributed under the License is distributed on an "AS IS" BASIS,
 *  WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 *  See the License for the specific language governing permissions and
 *  limitations under the License.
 */
package org.flowable.common.engine.impl.javax.el;

import java.lang.reflect.AccessibleObject;
import java.lang.reflect.Method;

// This class is copied from https://github.com/apache/tomcat/tree/febda9acf2a9d6ed833382c4c49eec8964bc1431/java/jakarta/el
/*
 * This is a cut down version of org.apache.tomcat.util.Jre9Compat that provides
 * only the methods required by the EL implementation.
 *
 * This class is duplicated in org.apache.el.util
 * When making changes keep the two in sync.
 */
class Jre9Compat extends JreCompat {

    private static final Method canAccessMethod;
    private static final Method getModuleMethod;
    private static final Method isExportedMethod;
    private static final Method trySetAccessibleMethod;

    static {
        Method m1 = null;
        Method m2 = null;
        Method m3 = null;
        Method m4 = null;

        try {
            m1 = AccessibleObject.class.getMethod("canAccess", Object.class);
            m2 = Class.class.getMethod("getModule");
            Class<?> moduleClass = Class.forName("java.lang.Module");
            m3 = moduleClass.getMethod("isExported", String.class);
            m4 = AccessibleObject.class.getMethod("trySetAccessible");
        } catch (NoSuchMethodException e) {
            // Expected for Java 8
        } catch (ClassNotFoundException e) {
            // Can't log this so...
            throw new RuntimeException(e);
        }

        canAccessMethod = m1;
        getModuleMethod = m2;
        isExportedMethod = m3;
        trySetAccessibleMethod = m4;
    }


    public static boolean isSupported() {
        return canAccessMethod != null;
    }


    @Override
    public boolean canAccess(Object base, AccessibleObject accessibleObject) {
        try {
            return ((Boolean) canAccessMethod.invoke(accessibleObject, base)).booleanValue();
        } catch (ReflectiveOperationException | IllegalArgumentException e) {
            return false;
        }
    }


    @Override
    public boolean isExported(Class<?> type) {
        try {
            String packageName = type.getPackage().getName();
            Object module = getModuleMethod.invoke(type);
            return ((Boolean) isExportedMethod.invoke(module, packageName)).booleanValue();
        } catch (ReflectiveOperationException e) {
            return false;
        }
    }

    @Override
    public boolean trySetAccessible(AccessibleObject accessibleObject) {
        try {
            return ((Boolean) trySetAccessibleMethod.invoke(accessibleObject)).booleanValue();
        } catch (ReflectiveOperationException | SecurityException e) {
            return false;
        }
    }
}
