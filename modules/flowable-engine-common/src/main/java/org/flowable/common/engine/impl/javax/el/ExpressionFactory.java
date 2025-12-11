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

import java.io.File;
import java.io.FileInputStream;
import java.io.FileNotFoundException;
import java.io.IOException;
import java.io.InputStream;
import java.lang.ref.WeakReference;
import java.lang.reflect.Constructor;
import java.lang.reflect.InvocationTargetException;
import java.lang.reflect.Method;
import java.util.Iterator;
import java.util.Map;
import java.util.Properties;
import java.util.ServiceLoader;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.locks.Lock;
import java.util.concurrent.locks.ReadWriteLock;
import java.util.concurrent.locks.ReentrantReadWriteLock;

/**
 * Provides an implementation for creating and evaluating Jakarta Expression Language expressions.
 *
 * <p>
 * Classes that implement the Jakarta Expression Language expression language expose their functionality via this
 * abstract class. An implementation supports the following functionalities.
 *
 * <ul>
 *   <li>Parses a <code>String</code> into a {@link ValueExpression} or {@link MethodExpression} instance for later
 *   evaluation.</li>
 *   <li>Implements an <code>ELResolver</code> for query operators</li>
 *   <li>Provides a default type coercion</li>
 * </ul>
 *
 * <p>
 * The {@link #newInstance} method can be used to obtain an instance of the implementation. Technologies such as
 * Jakarta Server Pages and Jakarta Faces provide access to an implementation via factory methods.
 *
 * <p>
 * The {@link #createValueExpression} method is used to parse expressions that evaluate to values (both l-values and
 * r-values are supported). The {@link #createMethodExpression} method is used to parse expressions that evaluate to a
 * reference to a method on an object.
 *
 * <p>
 * Resolution of model objects is performed at evaluation time, via the {@link ELResolver} associated with the
 * {@link ELContext} passed to the <code>ValueExpression</code> or <code>MethodExpression</code>.
 *
 * <p>
 * The ELContext object also provides access to the {@link FunctionMapper} and {@link VariableMapper} to be used when
 * parsing the expression. Jakarta Expression Language function and variable mapping is performed at parse-time, and the
 * results are bound to the expression. Therefore, the {@link ELContext}, {@link FunctionMapper}, and
 * {@link VariableMapper} are not stored for future use and do not have to be <code>Serializable</code>.
 *
 * <p>
 * The <code>createValueExpression</code> and <code>createMethodExpression</code> methods must be thread-safe. That is,
 * multiple threads may call these methods on the same <code>ExpressionFactory</code> object simultaneously.
 * Implementations should synchronize access if they depend on transient state. Implementations should not, however,
 * assume that only one object of each <code>ExpressionFactory</code> type will be instantiated; global caching should
 * therefore be static.
 *
 * <p>
 * The <code>ExpressionFactory</code> must be able to handle the following types of input for the
 * <code>expression</code> parameter:
 * <ul>
 *   <li>Single expressions using the <code>${}</code> delimiter (e.g. <code>"${employee.lastName}"</code>).</li>
 *   <li>Single expressions using the <code>#{}</code> delimiter (e.g. <code>"#{employee.lastName}"</code>).</li>
 *   <li>Literal text containing no <code>${}</code> or <code>#{}</code> delimiters (e.g. <code>"John Doe"</code>).</li>
 *   <li>Multiple expressions using the same delimiter (e.g. <code>"${employee.firstName}${employee.lastName}"</code> or
 *   <code>"#{employee.firstName}#{employee.lastName}"</code>).</li>
 *   <li>Mixed literal text and expressions using the same delimiter (e.g.
 *   <code>"Name: ${employee.firstName} ${employee.lastName}"</code>).</li>
 * </ul>
 *
 * <p>
 * The following types of input are illegal and must cause an {@link ELException} to be thrown:
 * <ul>
 *   <li>Multiple expressions using different delimiters (e.g.
 *   <code>"${employee.firstName}#{employee.lastName}"</code>).</li>
 *   <li>Mixed literal text and expressions using different delimiters(e.g.
 *   <code>"Name: ${employee.firstName} #{employee.lastName}"</code>).</li>
 * </ul>
 * @since 2.1
 */
public abstract class ExpressionFactory {

    private static final String PROPERTY_NAME = "ExpressionFactory";

    private static final String PROPERTY_FILE =
            System.getProperty("java.home") + File.separator + "lib" + File.separator + "el.properties";

    private static final CacheValue nullTcclFactory = new CacheValue();
    private static final Map<CacheKey, CacheValue> factoryCache = new ConcurrentHashMap<>();

    /**
     * Create a new {@link ExpressionFactory}. The class to use is determined by the following search order:
     * <ol>
     * <li>services API (META-INF/services/ExpressionFactory)</li>
     * <li>$JRE_HOME/lib/el.properties - key ExpressionFactory</li>
     * <li>ExpressionFactory</li>
     * <li>Platform default implementation - org.flowable.common.engine.impl.de.odysseus.el.ExpressionFactoryImpl</li>
     * </ol>
     *
     * @return the new ExpressionFactory
     */
    public static ExpressionFactory newInstance() {
        return newInstance(null);
    }

    /**
     * Create a new {@link ExpressionFactory} passing in the provided {@link Properties}. Search order is the same as
     * {@link #newInstance()}.
     *
     * @param properties the properties to be passed to the new instance (might be null)
     * @return the new ExpressionFactory
     */
    public static ExpressionFactory newInstance(Properties properties) {

        ClassLoader tccl = Thread.currentThread().getContextClassLoader();

        CacheValue cacheValue;
        Class<?> clazz;

        if (tccl == null) {
            cacheValue = nullTcclFactory;
        } else {
            CacheKey key = new CacheKey(tccl);
            cacheValue = factoryCache.get(key);
            if (cacheValue == null) {
                CacheValue newCacheValue = new CacheValue();
                cacheValue = factoryCache.putIfAbsent(key, newCacheValue);
                if (cacheValue == null) {
                    cacheValue = newCacheValue;
                }
            }
        }

        final Lock readLock = cacheValue.getLock().readLock();
        readLock.lock();
        try {
            clazz = cacheValue.getFactoryClass();
        } finally {
            readLock.unlock();
        }

        if (clazz == null) {
            String className = null;
            try {
                final Lock writeLock = cacheValue.getLock().writeLock();
                writeLock.lock();
                try {
                    className = cacheValue.getFactoryClassName();
                    if (className == null) {
                        className = discoverClassName(tccl);
                        cacheValue.setFactoryClassName(className);
                    }
                    if (tccl == null) {
                        clazz = Class.forName(className);
                    } else {
                        clazz = tccl.loadClass(className);
                    }
                    cacheValue.setFactoryClass(clazz);
                } finally {
                    writeLock.unlock();
                }
            } catch (ClassNotFoundException e) {
                throw new ELException("Unabled to find ExpressionFactory of type " + className, e);
            }
        }

        ExpressionFactory result;

        try {
            Constructor<?> constructor = null;
            // Do we need to look for a constructor that will take properties?
            if (properties != null) {
                try {
                    constructor = clazz.getConstructor(Properties.class);
                } catch (SecurityException se) {
                    throw new ELException(se);
                } catch (NoSuchMethodException nsme) {
                    // This can be ignored
                    // This is OK for this constructor not to exist
                }
            }
            if (constructor == null) {
                result = (ExpressionFactory) clazz.getConstructor().newInstance();
            } else {
                result = (ExpressionFactory) constructor.newInstance(properties);
            }

        } catch (InvocationTargetException e) {
            Throwable cause = e.getCause();
            Util.handleThrowable(cause);
            throw new ELException("Unabled to create ExpressionFactory of type '" + clazz.getName() + "'", e);
        } catch (ReflectiveOperationException | IllegalArgumentException e) {
            throw new ELException("Unabled to create ExpressionFactory of type '" + clazz.getName() + "'", e);
        }

        return result;
    }

    /**
     * Create a new value expression.
     *
     * @param context The EL context for this evaluation
     * @param expression The String representation of the value expression
     * @param expectedType The expected type of the result of evaluating the expression
     * @return A new value expression formed from the input parameters
     * @throws NullPointerException If the expected type is <code>null</code>
     * @throws ELException If there are syntax errors in the provided expression
     */
    public abstract ValueExpression createValueExpression(ELContext context, String expression, Class<?> expectedType);

    public abstract ValueExpression createValueExpression(Object instance, Class<?> expectedType);

    /**
     * Create a new method expression instance.
     *
     * @param context The EL context for this evaluation
     * @param expression The String representation of the method expression
     * @param expectedReturnType The expected type of the result of invoking the method
     * @param expectedParamTypes The expected types of the input parameters
     * @return A new method expression formed from the input parameters.
     * @throws NullPointerException If the expected parameters types are <code>null</code>
     * @throws ELException If there are syntax errors in the provided expression
     */
    public abstract MethodExpression createMethodExpression(ELContext context, String expression,
            Class<?> expectedReturnType, Class<?>[] expectedParamTypes);

    /**
     * Coerce the supplied object to the requested type.
     *
     * @param <T> The type to which the object should be coerced
     * @param obj The object to be coerced
     * @param expectedType The type to which the object should be coerced
     * @return An instance of the requested type.
     * @throws ELException If the conversion fails
     */
    public abstract <T> T coerceToType(Object obj, Class<T> expectedType);

    /**
     * Retrieves an ELResolver that implements the operations in collections.
     *
     * <p>
     * This ELResolver resolves the method invocation on the pair (<code>base</code>, <code>property</code>) when
     * <code>base</code> is a <code>Collection</code> or a <code>Map</code>, and <code>property</code> is the name of the
     * operation.
     *
     * <p>
     * See the specification document for detailed descriptions of these operators, their arguments, and return values.
     *
     * @return The <code>ELResolver</code> that implements the Query Operators.
     * @since Jakarta Expression Language 3.0
     */
    public ELResolver getStreamELResolver() {
        return null;
    }

    /**
     * Retrieve a function map containing a pre-configured function mapping.
     *
     * @return A initial map for functions, null if there is none.
     * @since Jakarta Expression Language 3.0
     */
    public Map<String, Method> getInitFunctionMap() {
        return null;
    }

    /**
     * Key used to cache ExpressionFactory discovery information per class loader. The class loader reference is never
     * {@code null}, because {@code null} tccl is handled separately.
     */
    private static class CacheKey {

        private final int hash;
        private final WeakReference<ClassLoader> ref;

        CacheKey(ClassLoader cl) {
            hash = cl.hashCode();
            ref = new WeakReference<>(cl);
        }

        @Override
        public int hashCode() {
            return hash;
        }

        @Override
        public boolean equals(Object obj) {
            if (obj == this) {
                return true;
            }
            if (!(obj instanceof CacheKey)) {
                return false;
            }
            ClassLoader thisCl = ref.get();
            if (thisCl == null) {
                return false;
            }
            return thisCl == ((CacheKey) obj).ref.get();
        }
    }

    private static class CacheValue {

        private final ReadWriteLock lock = new ReentrantReadWriteLock();
        private String className;
        private WeakReference<Class<?>> ref;

        CacheValue() {
        }

        public ReadWriteLock getLock() {
            return lock;
        }

        public String getFactoryClassName() {
            return className;
        }

        public void setFactoryClassName(String className) {
            this.className = className;
        }

        public Class<?> getFactoryClass() {
            return ref != null ? ref.get() : null;
        }

        public void setFactoryClass(Class<?> clazz) {
            ref = new WeakReference<>(clazz);
        }
    }

    /**
     * Discover the name of class that implements ExpressionFactory.
     *
     * @param tccl {@code ClassLoader}
     * @return Class name. There is default, so it is never {@code null}.
     */
    private static String discoverClassName(ClassLoader tccl) {
        // First services API
        String className = getClassNameServices(tccl);
        if (className == null) {
            // Second el.properties file
            className = getClassNameJreDir();
        }
        if (className == null) {
            // Third system property
            className = getClassNameSysProp();
        }
        if (className == null) {
            // Fourth - default
            className = "org.flowable.common.engine.impl.de.odysseus.el.ExpressionFactoryImpl";
        }
        return className;
    }

    private static String getClassNameServices(ClassLoader tccl) {

        ExpressionFactory result = null;

        ServiceLoader<ExpressionFactory> serviceLoader = ServiceLoader.load(ExpressionFactory.class, tccl);
        Iterator<ExpressionFactory> iter = serviceLoader.iterator();
        while (result == null && iter.hasNext()) {
            result = iter.next();
        }

        if (result == null) {
            return null;
        }

        return result.getClass().getName();
    }

    private static String getClassNameJreDir() {
        File file = new File(PROPERTY_FILE);
        if (file.canRead()) {
            try (InputStream is = new FileInputStream(file)) {
                Properties props = new Properties();
                props.load(is);
                String value = props.getProperty(PROPERTY_NAME);
                if (value != null && !value.trim().isEmpty()) {
                    return value.trim();
                }
            } catch (FileNotFoundException e) {
                // Should not happen - ignore it if it does
            } catch (IOException ioe) {
                throw new ELException("Failed to read " + PROPERTY_NAME, ioe);
            }
        }
        return null;
    }

    private static String getClassNameSysProp() {
        String value = System.getProperty(PROPERTY_NAME);
        if (value != null && !value.trim().isEmpty()) {
            return value.trim();
        }
        return null;
    }

}