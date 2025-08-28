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

import java.lang.reflect.Array;
import java.lang.reflect.Constructor;
import java.lang.reflect.Method;
import java.lang.reflect.Modifier;
import java.util.ArrayList;
import java.util.Iterator;
import java.util.List;
import java.util.Set;

// This class is copied from https://github.com/apache/tomcat/tree/febda9acf2a9d6ed833382c4c49eec8964bc1431/java/jakarta/el
// This class has been slightly modified in order to support the use cases for Flowable.
// The following modifications have been done:
// Remove getExpressionFactory static method -> We are instead passing the expression Factory to the appropriate methods in this class
// Remove nullTcclFactory and factoryCache static fields -> They are only relevant to the getExpressionFactory method, and we don't need that one
// Remove private static CacheKey, CacheValue and PrivilegedGetTccl classes -> They are only relevant to the getExpressionFactory method, and we don't need that one
// Remove private static getContextLoader method -> Only relevant for the getExpressionFactory method, and we don't need that one
// Remove handleThrowable static method -> We are not using it
// Remove findConstructor static method -> We are not using it
// Throw fixed error messages instead of using LocalString resource bundle
// Remove message static method -> we are not using it
// Add ExpressionFactory as last method parameter to findMethod, findWrapper, isCoercibleFrom and buildParameters
// Make public methods accessible if they aren't
// The findWrapper method has been enhanced with the implementation from https://github.com/eclipse-ee4j/el-ri/blob/4e7c61bce9e7750c2fa6fb85476e33f17b0246b4/api/src/main/java/jakarta/el/ELUtil.java
// This method follows the JLS more closely and allows picking of ambiguous overloaded methods better.
// The code in findWrapper is not identical to the one from ELUtil in order to make it more readable for the maintainers of Flowable

// In order for this class to be more easily kept in sync with the Tomcat implementation, we should not do style changes, nor fix warnings.
// Keeping the modifications to minimum would make it easier to keep this class in sync
class Util {

    private static final Class<?>[] EMPTY_CLASS_ARRAY = new Class<?>[0];
    private static final Object[] EMPTY_OBJECT_ARRAY = new Object[0];

    /*
     * This method duplicates code in org.apache.el.util.ReflectionUtil. When
     * making changes keep the code in sync.
     */
    static Method findMethod(Class<?> clazz, Object base, String methodName,
            Class<?>[] paramTypes, Object[] paramValues, ExpressionFactory factory) {

        if (clazz == null || methodName == null) {
            throw new MethodNotFoundException("Method not found: " + clazz + "." + methodName + "(" + paramString(paramTypes) + ")");
        }

        if (paramTypes == null) {
            paramTypes = getTypesFromValues(paramValues);
        }

        Method[] methods = clazz.getMethods();

        List<Wrapper<Method>> wrappers = Wrapper.wrap(methods, methodName);

        Wrapper<Method> result = findWrapper(clazz, wrappers, methodName, paramTypes, paramValues, factory);

        return getMethod(clazz, base, result.unWrap());
    }

    /*
     * This method duplicates code in org.apache.el.util.ReflectionUtil. When
     * making changes keep the code in sync.
     *
     * Parts of this method and related methods (distributed under the Apache 2.0 License):
     * Copyright (c) 1997, 2019 Oracle and/or its affiliates and others.
     * All rights reserved.
     * Copyright 2004 The Apache Software Foundation
     */
    @SuppressWarnings("null")
    private static <T> Wrapper<T> findWrapper(Class<?> clazz, List<Wrapper<T>> wrappers,
            String name, Class<?>[] paramTypes, Object[] paramValues, ExpressionFactory factory) {

        // Flowable comment: The implementation in ELUtil has 3 candidates lists.
        // This is changed here to have one list and tracking the current candidates type.
        // Since only the most specific one will be used
        List<Wrapper<T>> candidates = new ArrayList<>();
        CandidatesType candidatesType = CandidatesType.UNKNOWN;

        int paramCount = paramTypes.length;

        for (Wrapper<T> w : wrappers) {
            Class<?>[] mParamTypes = w.getParameterTypes();
            int mParamCount;
            if (mParamTypes == null) {
                mParamCount = 0;
            } else {
                mParamCount = mParamTypes.length;
            }

            // Check the number of parameters
            // Multiple tests to improve readability
            if (!w.isVarArgs() && paramCount != mParamCount) {
                // Method has wrong number of parameters
                continue;
            }
            if (w.isVarArgs() && paramCount < mParamCount -1) {
                // Method has wrong number of parameters
                continue;
            }
            if (w.isVarArgs() && paramCount == mParamCount && paramValues != null &&
                    paramValues.length > paramCount && !paramTypes[mParamCount -1].isArray()) {
                // Method arguments don't match
                continue;
            }
            if (w.isVarArgs() && paramCount > mParamCount && paramValues != null &&
                    paramValues.length != paramCount) {
                // Might match a different varargs method
                continue;
            }
            if (!w.isVarArgs() && paramValues != null && paramCount != paramValues.length) {
                // Might match a different varargs method
                continue;
            }

            // Check the parameters match
            boolean assignable = false;
            boolean coercible = false;
            boolean varArgs = false;
            int exactMatch = 0;
            boolean noMatch = false;
            for (int i = 0; i < mParamCount; i++) {
                // Can't be null
                if (w.isVarArgs() && i == (mParamCount - 1)) {
                    varArgs = true;
                    // exact var array type match
                    if (mParamCount == paramCount) {
                        if (mParamTypes[i] == paramTypes[i]) {
                            continue;
                        }
                    }

                    if (i == paramCount || (paramValues != null && paramValues.length == i)) {
                        // Nothing is passed as varargs
                        break;
                    }
                    Class<?> varType = mParamTypes[i].getComponentType();
                    for (int j = i; j < paramCount; j++) {
                        if (!isAssignableFrom(paramTypes[j], varType)) {
                            if (paramValues == null) {
                                noMatch = true;
                                break;
                            } else {
                                if (!isCoercibleFrom(paramValues[j], varType, factory)) {
                                    noMatch = true;
                                    break;
                                }
                            }
                        }
                    }
                } else {
                    if (mParamTypes[i].equals(paramTypes[i])) {
                        exactMatch++;
                    } else if (paramTypes[i] != null && isAssignableFrom(paramTypes[i], mParamTypes[i])) {
                        assignable = true;
                    } else {
                        if (paramValues == null) {
                            noMatch = true;
                            break;
                        } else {
                            if (isCoercibleFrom(paramValues[i], mParamTypes[i], factory)) {
                                coercible = true;
                            } else {
                                noMatch = true;
                                break;
                            }
                        }
                    }
                }
            }
            if (noMatch) {
                continue;
            }

            // If a method is found where every parameter matches exactly,
            // return it
            if (exactMatch == paramCount) {
                return w;
            }

            if (varArgs) {
                if (candidatesType == CandidatesType.VAR_ARGS) {
                    candidates.add(w);
                } else if (candidatesType.compareTo(CandidatesType.VAR_ARGS) < 0) {
                    candidatesType = CandidatesType.VAR_ARGS;
                    candidates.clear();
                    candidates.add(w);
                }
            } else if (coercible) {
                if (candidatesType == CandidatesType.COERCIBLE) {
                    candidates.add(w);
                } else if (candidatesType.compareTo(CandidatesType.COERCIBLE) < 0) {
                    candidatesType = CandidatesType.COERCIBLE;
                    candidates.clear();
                    candidates.add(w);
                }
            } else if (assignable) {
                if (candidatesType == CandidatesType.ASSIGNABLE) {
                    candidates.add(w);
                } else if (candidatesType.compareTo(CandidatesType.ASSIGNABLE) < 0) {
                    candidatesType = CandidatesType.ASSIGNABLE;
                    candidates.clear();
                    candidates.add(w);
                }
            }
        }

        if (!candidates.isEmpty()) {
            String errorMsg = "Unable to find unambiguous method: " + clazz + "." + name + "(" + paramString(paramTypes) + ")";
            return findMostSpecificWrapper(candidates, paramTypes, candidatesType == CandidatesType.ASSIGNABLE, errorMsg);
        }

        throw new MethodNotFoundException("Method not found: " + clazz + "." + name + "(" + paramString(paramTypes) + ")");
    }

    /*
     * This method duplicates code in com.sun.el.util.ReflectionUtil. When making changes keep the code in sync.
     */
    private static <T> Wrapper<T> findMostSpecificWrapper(List<Wrapper<T>> candidates, Class<?>[] matchingTypes, boolean elSpecific, String errorMsg) {
        List<Wrapper<T>> ambiguouses = new ArrayList<>();
        for (Wrapper<T> candidate : candidates) {
            boolean lessSpecific = false;

            Iterator<Wrapper<T>> it = ambiguouses.iterator();
            while (it.hasNext()) {
                int result = isMoreSpecific(candidate, it.next(), matchingTypes, elSpecific);
                if (result == 1) {
                    it.remove();
                } else if (result == -1) {
                    lessSpecific = true;
                }
            }

            if (!lessSpecific) {
                ambiguouses.add(candidate);
            }
        }

        if (ambiguouses.size() > 1) {
            throw new MethodNotFoundException(errorMsg);
        }

        return ambiguouses.get(0);
    }

    /*
     * This method duplicates code in com.sun.el.util.ReflectionUtil. When making changes keep the code in sync.
     */
    private static <T> int isMoreSpecific(Wrapper<T> wrapper1, Wrapper<T> wrapper2, Class<?>[] matchingTypes, boolean elSpecific) {
        Class<?>[] paramTypes1 = wrapper1.getParameterTypes();
        Class<?>[] paramTypes2 = wrapper2.getParameterTypes();

        if (wrapper1.isVarArgs()) {
            // JLS8 15.12.2.5 Choosing the Most Specific Method
            int length = Math.max(Math.max(paramTypes1.length, paramTypes2.length), matchingTypes.length);
            paramTypes1 = getComparingParamTypesForVarArgsMethod(paramTypes1, length);
            paramTypes2 = getComparingParamTypesForVarArgsMethod(paramTypes2, length);

            if (length > matchingTypes.length) {
                Class<?>[] matchingTypes2 = new Class<?>[length];
                System.arraycopy(matchingTypes, 0, matchingTypes2, 0, matchingTypes.length);
                matchingTypes = matchingTypes2;
            }
        }

        int result = 0;
        for (int i = 0; i < paramTypes1.length; i++) {
            if (paramTypes1[i] != paramTypes2[i]) {
                int r2 = isMoreSpecific(paramTypes1[i], paramTypes2[i], matchingTypes[i], elSpecific);
                if (r2 == 1) {
                    if (result == -1) {
                        return 0;
                    }
                    result = 1;
                } else if (r2 == -1) {
                    if (result == 1) {
                        return 0;
                    }
                    result = -1;
                } else {
                    return 0;
                }
            }
        }

        if (result == 0) {
            // The nature of bridge methods is such that it actually
            // doesn't matter which one we pick as long as we pick
            // one. That said, pick the 'right' one (the non-bridge
            // one) anyway.
            result = Boolean.compare(wrapper1.isBridge(), wrapper2.isBridge());
        }

        return result;
    }

    /*
     * This method duplicates code in com.sun.el.util.ReflectionUtil. When making changes keep the code in sync.
     */
    private static int isMoreSpecific(Class<?> type1, Class<?> type2, Class<?> matchingType, boolean elSpecific) {
        type1 = getBoxingTypeIfPrimitive(type1);
        type2 = getBoxingTypeIfPrimitive(type2);
        if (type2.isAssignableFrom(type1)) {
            return 1;
        } else if (type1.isAssignableFrom(type2)) {
            return -1;
        } else {
            if (elSpecific) {
                /*
                 * Number will be treated as more specific
                 *
                 * ASTInteger only return Long or BigInteger, no Byte / Short / Integer. ASTFloatingPoint also.
                 *
                 */
                if (matchingType != null && Number.class.isAssignableFrom(matchingType)) {
                    boolean b1 = Number.class.isAssignableFrom(type1) || type1.isPrimitive();
                    boolean b2 = Number.class.isAssignableFrom(type2) || type2.isPrimitive();
                    if (b1 && !b2) {
                        return 1;
                    } else if (b2 && !b1) {
                        return -1;
                    } else {
                        return 0;
                    }
                }

                return 0;
            } else {
                return 0;
            }
        }
    }

    /*
     * This method duplicates code in com.sun.el.util.ReflectionUtil. When making changes keep the code in sync.
     */
    private static Class<?> getBoxingTypeIfPrimitive(Class<?> clazz) {
        if (clazz.isPrimitive()) {
            if (clazz == Boolean.TYPE) {
                return Boolean.class;
            }
            if (clazz == Character.TYPE) {
                return Character.class;
            }
            if (clazz == Byte.TYPE) {
                return Byte.class;
            }
            if (clazz == Short.TYPE) {
                return Short.class;
            }
            if (clazz == Integer.TYPE) {
                return Integer.class;
            }
            if (clazz == Long.TYPE) {
                return Long.class;
            }
            if (clazz == Float.TYPE) {
                return Float.class;
            }

            return Double.class;
        } else {
            return clazz;
        }
    }

    /*
     * This method duplicates code in com.sun.el.util.ReflectionUtil. When making changes keep the code in sync.
     */
    private static Class<?>[] getComparingParamTypesForVarArgsMethod(Class<?>[] paramTypes, int length) {
        Class<?>[] result = new Class<?>[length];
        System.arraycopy(paramTypes, 0, result, 0, paramTypes.length - 1);
        Class<?> type = paramTypes[paramTypes.length - 1].getComponentType();
        for (int i = paramTypes.length - 1; i < length; i++) {
            result[i] = type;
        }

        return result;
    }


    private static final String paramString(Class<?>[] types) {
        if (types != null) {
            StringBuilder sb = new StringBuilder();
            for (Class<?> type : types) {
                if (type == null) {
                    sb.append("null, ");
                } else {
                    sb.append(type.getName()).append(", ");
                }
            }
            if (sb.length() > 2) {
                sb.setLength(sb.length() - 2);
            }
            return sb.toString();
        }
        return null;
    }


    /*
     * This method duplicates code in org.apache.el.util.ReflectionUtil. When
     * making changes keep the code in sync.
     */
    private static <T> Wrapper<T> resolveAmbiguousWrapper(Set<Wrapper<T>> candidates,
            Class<?>[] paramTypes) {
        // Identify which parameter isn't an exact match
        Wrapper<T> w = candidates.iterator().next();

        int nonMatchIndex = 0;
        Class<?> nonMatchClass = null;

        for (int i = 0; i < paramTypes.length; i++) {
            if (w.getParameterTypes()[i] != paramTypes[i]) {
                nonMatchIndex = i;
                nonMatchClass = paramTypes[i];
                break;
            }
        }

        if (nonMatchClass == null) {
            // Null will always be ambiguous
            return null;
        }

        for (Wrapper<T> c : candidates) {
            if (c.getParameterTypes()[nonMatchIndex] ==
                    paramTypes[nonMatchIndex]) {
                // Methods have different non-matching parameters
                // Result is ambiguous
                return null;
            }
        }

        // Can't be null
        Class<?> superClass = nonMatchClass.getSuperclass();
        while (superClass != null) {
            for (Wrapper<T> c : candidates) {
                if (c.getParameterTypes()[nonMatchIndex].equals(superClass)) {
                    // Found a match
                    return c;
                }
            }
            superClass = superClass.getSuperclass();
        }

        // Treat instances of Number as a special case
        Wrapper<T> match = null;
        if (Number.class.isAssignableFrom(nonMatchClass)) {
            for (Wrapper<T> c : candidates) {
                Class<?> candidateType = c.getParameterTypes()[nonMatchIndex];
                if (Number.class.isAssignableFrom(candidateType) ||
                        candidateType.isPrimitive()) {
                    if (match == null) {
                        match = c;
                    } else {
                        // Match still ambiguous
                        match = null;
                        break;
                    }
                }
            }
        }

        return match;
    }


    /*
     * This method duplicates code in org.apache.el.util.ReflectionUtil. When
     * making changes keep the code in sync.
     */
    static boolean isAssignableFrom(Class<?> src, Class<?> target) {
        // src will always be an object
        // Short-cut. null is always assignable to an object and in EL null
        // can always be coerced to a valid value for a primitive
        if (src == null) {
            return true;
        }

        Class<?> targetClass;
        if (target.isPrimitive()) {
            if (target == Boolean.TYPE) {
                targetClass = Boolean.class;
            } else if (target == Character.TYPE) {
                targetClass = Character.class;
            } else if (target == Byte.TYPE) {
                targetClass = Byte.class;
            } else if (target == Short.TYPE) {
                targetClass = Short.class;
            } else if (target == Integer.TYPE) {
                targetClass = Integer.class;
            } else if (target == Long.TYPE) {
                targetClass = Long.class;
            } else if (target == Float.TYPE) {
                targetClass = Float.class;
            } else {
                targetClass = Double.class;
            }
        } else {
            targetClass = target;
        }
        return targetClass.isAssignableFrom(src);
    }


    /*
     * This method duplicates code in org.apache.el.util.ReflectionUtil. When
     * making changes keep the code in sync.
     */
    private static boolean isCoercibleFrom(Object src, Class<?> target, ExpressionFactory factory) {
        // TODO: This isn't pretty but it works. Significant refactoring would
        //       be required to avoid the exception.
        try {
            factory.coerceToType(src, target);
        } catch (ELException e) {
            return false;
        }
        return true;
    }


    private static Class<?>[] getTypesFromValues(Object[] values) {
        if (values == null) {
            return EMPTY_CLASS_ARRAY;
        }

        Class<?> result[] = new Class<?>[values.length];
        for (int i = 0; i < values.length; i++) {
            if (values[i] == null) {
                result[i] = null;
            } else {
                result[i] = values[i].getClass();
            }
        }
        return result;
    }


    /*
     * This method duplicates code in org.apache.el.util.ReflectionUtil. When
     * making changes keep the code in sync.
     */
    static Method getMethod(Class<?> type, Object base, Method m) {
        JreCompat jreCompat = JreCompat.getInstance();
        // If base is null, method MUST be static
        // If base is non-null, method may be static or non-static
        if (m == null ||
                (Modifier.isPublic(type.getModifiers()) &&
                        (jreCompat.canAccess(base, m) || base != null && jreCompat.canAccess(null, m)))) {
            return m;
        }

        if (Modifier.isPublic(m.getModifiers())) {
            if (jreCompat.trySetAccessible(m)) {
                return m;
            }
        }

        Class<?>[] interfaces = type.getInterfaces();
        Method mp = null;
        for (Class<?> iface : interfaces) {
            try {
                mp = iface.getMethod(m.getName(), m.getParameterTypes());
                mp = getMethod(mp.getDeclaringClass(), base, mp);
                if (mp != null) {
                    return mp;
                }
            } catch (NoSuchMethodException e) {
                // Ignore
            }
        }
        Class<?> sup = type.getSuperclass();
        if (sup != null) {
            try {
                mp = sup.getMethod(m.getName(), m.getParameterTypes());
                mp = getMethod(mp.getDeclaringClass(), base, mp);
                if (mp != null) {
                    return mp;
                }
            } catch (NoSuchMethodException e) {
                // Ignore
            }
        }
        return null;
    }


    static Object[] buildParameters(Class<?>[] parameterTypes,
            boolean isVarArgs,Object[] params, ExpressionFactory factory) {
        Object[] parameters = null;
        if (parameterTypes.length > 0) {
            parameters = new Object[parameterTypes.length];
            int paramCount;
            if (params == null) {
                params = EMPTY_OBJECT_ARRAY;
            }
            paramCount = params.length;
            if (isVarArgs) {
                int varArgIndex = parameterTypes.length - 1;
                // First argCount-1 parameters are standard
                for (int i = 0; (i < varArgIndex); i++) {
                    parameters[i] = coerceValue(params[i], parameterTypes[i], factory);
                }
                // Last parameter is the varargs
                Class<?> varArgClass =
                        parameterTypes[varArgIndex].getComponentType();
                final Object varargs = Array.newInstance(
                        varArgClass,
                        (paramCount - varArgIndex));
                for (int i = (varArgIndex); i < paramCount; i++) {
                    Array.set(varargs, i - varArgIndex,
                            coerceValue(params[i], varArgClass, factory));
                }
                parameters[varArgIndex] = varargs;
            } else {
                parameters = new Object[parameterTypes.length];
                for (int i = 0; i < parameterTypes.length; i++) {
                    parameters[i] = coerceValue(params[i], parameterTypes[i], factory);
                }
            }
        }
        return parameters;
    }

    static Object coerceValue(Object value, Class<?> type, ExpressionFactory factory) {
        if (value != null || type.isPrimitive()) {
            return factory.coerceToType(value, type);
        }

        return null;
    }


    private abstract static class Wrapper<T> {

        public static List<Wrapper<Method>> wrap(Method[] methods, String name) {
            List<Wrapper<Method>> result = new ArrayList<>();
            for (Method method : methods) {
                if (method.getName().equals(name)) {
                    result.add(new MethodWrapper(method));
                }
            }
            return result;
        }

        public static List<Wrapper<Constructor<?>>> wrap(Constructor<?>[] constructors) {
            List<Wrapper<Constructor<?>>> result = new ArrayList<>();
            for (Constructor<?> constructor : constructors) {
                result.add(new ConstructorWrapper(constructor));
            }
            return result;
        }

        public abstract T unWrap();
        public abstract Class<?>[] getParameterTypes();
        public abstract boolean isVarArgs();
        public abstract boolean isBridge();
    }


    private static class MethodWrapper extends Wrapper<Method> {
        private final Method m;

        public MethodWrapper(Method m) {
            this.m = m;
        }

        @Override
        public Method unWrap() {
            return m;
        }

        @Override
        public Class<?>[] getParameterTypes() {
            return m.getParameterTypes();
        }

        @Override
        public boolean isVarArgs() {
            return m.isVarArgs();
        }

        @Override
        public boolean isBridge() {
            return m.isBridge();
        }
    }

    private static class ConstructorWrapper extends Wrapper<Constructor<?>> {
        private final Constructor<?> c;

        public ConstructorWrapper(Constructor<?> c) {
            this.c = c;
        }

        @Override
        public Constructor<?> unWrap() {
            return c;
        }

        @Override
        public Class<?>[] getParameterTypes() {
            return c.getParameterTypes();
        }

        @Override
        public boolean isVarArgs() {
            return c.isVarArgs();
        }

        @Override
        public boolean isBridge() {
            return false;
        }
    }

    private enum CandidatesType {
        UNKNOWN,
        VAR_ARGS,
        COERCIBLE,
        ASSIGNABLE,

    }

}
