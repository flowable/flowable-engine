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
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Set;

// This class is copied from https://github.com/apache/tomcat/tree/febda9acf2a9d6ed833382c4c49eec8964bc1431/java/jakarta/el
// This class has been slightly modified in order to support the use cases for Flowable.
// The following modifications have been done:
// Remove getExpressionFactory static method -> We are instead passing the expression Factory to the appropriate methods in this class
// Remove nullTcclFactory and factoryCache static fields -> They are only relevant to the getExpressionFactory method, and we don't need that one
// Remove private static CacheKey, CacheValue and PrivilegedGetTccl classes -> They are only relevant to the getExpressionFactory method, and we don't need that one
// Remove private static getContextLoader method -> Only relevant for the getExpressionFactory method, abd we don't need that one
// Remove handleThrowable static method -> We are not using it
// Remove findConstructor static method -> We are not using it
// Throw fixed error messages instead of using LocalString resource bundle
// Remove message static method -> ew are not using it
// Add ExpressionFactory as last method parameter to findMethod, findWrapper, isCoercibleFrom and buildParameters
// Make public methods accessible if they aren't

// In order for this class to be more easily kept in sync with the Tomcat implementation we should not do style changes, nor fix warnings.
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
     */
    @SuppressWarnings("null")
    private static <T> Wrapper<T> findWrapper(Class<?> clazz, List<Wrapper<T>> wrappers,
            String name, Class<?>[] paramTypes, Object[] paramValues, ExpressionFactory factory) {

        Map<Wrapper<T>,MatchResult> candidates = new HashMap<>();

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
            int exactMatch = 0;
            int assignableMatch = 0;
            int coercibleMatch = 0;
            boolean noMatch = false;
            for (int i = 0; i < mParamCount; i++) {
                // Can't be null
                if (w.isVarArgs() && i == (mParamCount - 1)) {
                    if (i == paramCount || (paramValues != null && paramValues.length == i)) {
                        // Nothing is passed as varargs
                        assignableMatch++;
                        break;
                    }
                    Class<?> varType = mParamTypes[i].getComponentType();
                    for (int j = i; j < paramCount; j++) {
                        if (isAssignableFrom(paramTypes[j], varType)) {
                            assignableMatch++;
                        } else {
                            if (paramValues == null) {
                                noMatch = true;
                                break;
                            } else {
                                if (isCoercibleFrom(paramValues[j], varType, factory)) {
                                    coercibleMatch++;
                                } else {
                                    noMatch = true;
                                    break;
                                }
                            }
                        }
                        // Don't treat a varArgs match as an exact match, it can
                        // lead to a varArgs method matching when the result
                        // should be ambiguous
                    }
                } else {
                    if (mParamTypes[i].equals(paramTypes[i])) {
                        exactMatch++;
                    } else if (paramTypes[i] != null && isAssignableFrom(paramTypes[i], mParamTypes[i])) {
                        assignableMatch++;
                    } else {
                        if (paramValues == null) {
                            noMatch = true;
                            break;
                        } else {
                            if (isCoercibleFrom(paramValues[i], mParamTypes[i], factory)) {
                                coercibleMatch++;
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

            candidates.put(w, new MatchResult(
                    exactMatch, assignableMatch, coercibleMatch, w.isBridge()));
        }

        // Look for the method that has the highest number of parameters where
        // the type matches exactly
        MatchResult bestMatch = new MatchResult(0, 0, 0, false);
        Wrapper<T> match = null;
        boolean multiple = false;
        for (Map.Entry<Wrapper<T>, MatchResult> entry : candidates.entrySet()) {
            int cmp = entry.getValue().compareTo(bestMatch);
            if (cmp > 0 || match == null) {
                bestMatch = entry.getValue();
                match = entry.getKey();
                multiple = false;
            } else if (cmp == 0) {
                multiple = true;
            }
        }
        if (multiple) {
            if (bestMatch.getExact() == paramCount - 1) {
                // Only one parameter is not an exact match - try using the
                // super class
                match = resolveAmbiguousWrapper(candidates.keySet(), paramTypes);
            } else {
                match = null;
            }

            if (match == null) {
                // If multiple methods have the same matching number of parameters
                // the match is ambiguous so throw an exception
                throw new MethodNotFoundException("Unable to find unambiguous method: " + clazz + "." + name + "(" + paramString(paramTypes) + ")");
            }
        }

        // Handle case where no match at all was found
        if (match == null) {
            throw new MethodNotFoundException("Method not found: " + clazz + "." + name + "(" + paramString(paramTypes) + ")");
        }

        return match;
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
                    parameters[i] = factory.coerceToType(params[i],
                            parameterTypes[i]);
                }
                // Last parameter is the varargs
                Class<?> varArgClass =
                        parameterTypes[varArgIndex].getComponentType();
                final Object varargs = Array.newInstance(
                        varArgClass,
                        (paramCount - varArgIndex));
                for (int i = (varArgIndex); i < paramCount; i++) {
                    Array.set(varargs, i - varArgIndex,
                            factory.coerceToType(params[i], varArgClass));
                }
                parameters[varArgIndex] = varargs;
            } else {
                parameters = new Object[parameterTypes.length];
                for (int i = 0; i < parameterTypes.length; i++) {
                    parameters[i] = factory.coerceToType(params[i],
                            parameterTypes[i]);
                }
            }
        }
        return parameters;
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

    /*
     * This class duplicates code in org.apache.el.util.ReflectionUtil. When
     * making changes keep the code in sync.
     */
    private static class MatchResult implements Comparable<MatchResult> {

        private final int exact;
        private final int assignable;
        private final int coercible;
        private final boolean bridge;

        public MatchResult(int exact, int assignable, int coercible, boolean bridge) {
            this.exact = exact;
            this.assignable = assignable;
            this.coercible = coercible;
            this.bridge = bridge;
        }

        public int getExact() {
            return exact;
        }

        public int getAssignable() {
            return assignable;
        }

        public int getCoercible() {
            return coercible;
        }

        public boolean isBridge() {
            return bridge;
        }

        @Override
        public int compareTo(MatchResult o) {
            int cmp = Integer.compare(this.getExact(), o.getExact());
            if (cmp == 0) {
                cmp = Integer.compare(this.getAssignable(), o.getAssignable());
                if (cmp == 0) {
                    cmp = Integer.compare(this.getCoercible(), o.getCoercible());
                    if (cmp == 0) {
                        // The nature of bridge methods is such that it actually
                        // doesn't matter which one we pick as long as we pick
                        // one. That said, pick the 'right' one (the non-bridge
                        // one) anyway.
                        cmp = Boolean.compare(o.isBridge(), this.isBridge());
                    }
                }
            }
            return cmp;
        }

        @Override
        public boolean equals(Object o)
        {
            return o == this || (null != o &&
                    this.getClass().equals(o.getClass()) &&
                    ((MatchResult)o).getExact() == this.getExact() &&
                    ((MatchResult)o).getAssignable() == this.getAssignable() &&
                    ((MatchResult)o).getCoercible() == this.getCoercible() &&
                    ((MatchResult)o).isBridge() == this.isBridge());
        }

        @Override
        public int hashCode()
        {
            return (this.isBridge() ? 1 << 24 : 0) ^
                    this.getExact() << 16 ^
                    this.getAssignable() << 8 ^
                    this.getCoercible();
        }
    }

}
