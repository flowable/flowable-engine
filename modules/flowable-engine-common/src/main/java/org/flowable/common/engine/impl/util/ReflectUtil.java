/* Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 * 
 *      http://www.apache.org/licenses/LICENSE-2.0
 * 
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */
package org.flowable.common.engine.impl.util;

import java.io.InputStream;
import java.lang.reflect.Constructor;
import java.lang.reflect.Field;
import java.lang.reflect.InvocationTargetException;
import java.lang.reflect.Method;
import java.net.URL;
import java.util.Arrays;
import java.util.Locale;
import java.util.regex.Pattern;

import org.flowable.common.engine.api.FlowableClassLoadingException;
import org.flowable.common.engine.api.FlowableException;
import org.flowable.common.engine.api.FlowableIllegalArgumentException;
import org.flowable.common.engine.impl.context.Context;
import org.flowable.common.engine.impl.interceptor.CommandContext;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

/**
 * @author Tom Baeyens
 */
public abstract class ReflectUtil {

    private static final Logger LOGGER = LoggerFactory.getLogger(ReflectUtil.class);

    private static final Pattern GETTER_PATTERN = Pattern.compile("(get|is)[A-Z].*");
    private static final Pattern SETTER_PATTERN = Pattern.compile("set[A-Z].*");

    public static ClassLoader getClassLoader() {
        ClassLoader loader = getCustomClassLoader();
        if (loader == null) {
            loader = Thread.currentThread().getContextClassLoader();
        }
        return loader;
    }

    public static Class<?> loadClass(String className) {
        Class<?> clazz = null;
        ClassLoader classLoader = getCustomClassLoader();

        // First exception in chain of classloaders will be used as cause when
        // no class is found in any of them
        Throwable throwable = null;

        if (classLoader != null) {
            try {
                LOGGER.trace("Trying to load class with custom classloader: {}", className);
                clazz = loadClass(classLoader, className);
            } catch (Throwable t) {
                throwable = t;
            }
        }
        if (clazz == null) {
            try {
                LOGGER.trace("Trying to load class with current thread context classloader: {}", className);
                clazz = loadClass(Thread.currentThread().getContextClassLoader(), className);
            } catch (Throwable t) {
                if (throwable == null) {
                    throwable = t;
                }
            }
            if (clazz == null) {
                try {
                    LOGGER.trace("Trying to load class with local classloader: {}", className);
                    clazz = loadClass(ReflectUtil.class.getClassLoader(), className);
                } catch (Throwable t) {
                    if (throwable == null) {
                        throwable = t;
                    }
                }
            }
        }

        if (clazz == null) {
            throw new FlowableClassLoadingException(className, throwable);
        }
        return clazz;
    }

    public static InputStream getResourceAsStream(String name) {
        InputStream resourceStream = null;
        ClassLoader classLoader = getCustomClassLoader();
        if (classLoader != null) {
            resourceStream = classLoader.getResourceAsStream(name);
        }

        if (resourceStream == null) {
            // Try the current Thread context classloader
            classLoader = Thread.currentThread().getContextClassLoader();
            resourceStream = classLoader.getResourceAsStream(name);
            if (resourceStream == null) {
                // Finally, try the classloader for this class
                classLoader = ReflectUtil.class.getClassLoader();
                resourceStream = classLoader.getResourceAsStream(name);
            }
        }
        return resourceStream;
    }

    public static URL getResource(String name) {
        URL url = null;
        ClassLoader classLoader = getCustomClassLoader();
        if (classLoader != null) {
            url = classLoader.getResource(name);
        }
        if (url == null) {
            // Try the current Thread context classloader
            classLoader = Thread.currentThread().getContextClassLoader();
            url = classLoader.getResource(name);
            if (url == null) {
                // Finally, try the classloader for this class
                classLoader = ReflectUtil.class.getClassLoader();
                url = classLoader.getResource(name);
            }
        }

        return url;
    }

    public static Object instantiate(String className) {
        try {
            Class<?> clazz = loadClass(className);
            return clazz.getConstructor().newInstance();
        } catch (Exception e) {
            throw new FlowableException("couldn't instantiate class " + className, e);
        }
    }

    public static Object invoke(Object target, String methodName, Object[] args) {
        try {
            Class<? extends Object> clazz = target.getClass();
            Method method = findMethod(clazz, methodName, args);
            method.setAccessible(true);
            return method.invoke(target, args);
        } catch (Exception e) {
            throw new FlowableException("couldn't invoke " + methodName + " on " + target, e);
        }
    }
    
    public static void invokeSetterOrField(Object target, String name, Object value, boolean throwExceptionOnMissingField) {
        Method setterMethod = getSetter(name, target.getClass(), value.getClass());

        if (setterMethod != null) {
            invokeSetter(setterMethod, target, name, value);
            
        } else {
            Field field = ReflectUtil.getField(name, target);
            if (field == null) {
                if (throwExceptionOnMissingField) {
                    throw new FlowableIllegalArgumentException("Field definition uses non-existent field '" + name + "' of class " + target.getClass().getName());
                } else {
                    return;
                }
            }

            // Check if the delegate field's type is correct
            if (!fieldTypeCompatible(value, field)) {
                throw new FlowableIllegalArgumentException("Incompatible type set on field declaration '" + name
                        + "' for class " + target.getClass().getName()
                        + ". Declared value has type " + value.getClass().getName()
                        + ", while expecting " + field.getType().getName());
            }
            
            setField(field, target, value);
        }
    }

    /**
     * Returns the field of the given object or null if it doesn't exist.
     */
    public static Field getField(String fieldName, Object object) {
        return getField(fieldName, object.getClass());
    }

    /**
     * Returns the field of the given class or null if it doesn't exist.
     */
    public static Field getField(String fieldName, Class<?> clazz) {
        Field field = null;
        try {
            field = clazz.getDeclaredField(fieldName);
        } catch (SecurityException e) {
            throw new FlowableException("not allowed to access field " + field + " on class " + clazz.getCanonicalName(), e);
        } catch (NoSuchFieldException e) {
            // for some reason getDeclaredFields doesn't search superclasses
            // (which getFields() does ... but that gives only public fields)
            Class<?> superClass = clazz.getSuperclass();
            if (superClass != null) {
                return getField(fieldName, superClass);
            }
        }
        return field;
    }

    public static void setField(Field field, Object object, Object value) {
        try {
            field.setAccessible(true);
            field.set(object, value);
        } catch (IllegalArgumentException | IllegalAccessException e) {
            throw new FlowableException("Could not set field " + field, e);
        }
    }

    /**
     * Returns the setter-method for the given field name or null if no setter exists.
     */
    public static Method getSetter(String fieldName, Class<?> clazz, Class<?> fieldType) {
        String setterName = "set" + Character.toTitleCase(fieldName.charAt(0)) + fieldName.substring(1);
        try {
            // Using getMethods(), getMethod(...) expects exact parameter type
            // matching and ignores inheritance-tree.
            Method[] methods = clazz.getMethods();
            for (Method method : methods) {
                if (method.getName().equals(setterName)) {
                    Class<?>[] paramTypes = method.getParameterTypes();
                    if (paramTypes != null && paramTypes.length == 1 && paramTypes[0].isAssignableFrom(fieldType)) {
                        return method;
                    }
                }
            }
            return null;
        } catch (SecurityException e) {
            throw new FlowableException("Not allowed to access method " + setterName + " on class " + clazz.getCanonicalName(), e);
        }
    }
    
    public static void invokeSetter(Method setterMethod, Object target, String name, Object value) {
        try {
            setterMethod.invoke(target, value);
        } catch (IllegalArgumentException e) {
            throw new FlowableException("Error while invoking '" + name + "' on class " + target.getClass().getName(), e);
        } catch (IllegalAccessException e) {
            throw new FlowableException("Illegal access when calling '" + name + "' on class " + target.getClass().getName(), e);
        } catch (InvocationTargetException e) {
            throw new FlowableException("Exception while invoking '" + name + "' on class " + target.getClass().getName(), e);
        }
    }

    private static Method findMethod(Class<? extends Object> clazz, String methodName, Object[] args) {
        for (Method method : clazz.getDeclaredMethods()) {
            // TODO add parameter matching
            if (method.getName().equals(methodName) && matches(method.getParameterTypes(), args)) {
                return method;
            }
        }
        Class<?> superClass = clazz.getSuperclass();
        if (superClass != null) {
            return findMethod(superClass, methodName, args);
        }
        return null;
    }

    public static Object instantiate(String className, Object[] args) {
        Class<?> clazz = loadClass(className);
        Constructor<?> constructor = findMatchingConstructor(clazz, args);
        if (constructor == null) {
            throw new FlowableException("couldn't find constructor for " + className + " with args " + Arrays.asList(args));
        }
        try {
            return constructor.newInstance(args);
        } catch (Exception e) {
            throw new FlowableException("couldn't find constructor for " + className + " with args " + Arrays.asList(args), e);
        }
    }

    @SuppressWarnings({ "unchecked", "rawtypes" })
    protected static <T> Constructor<T> findMatchingConstructor(Class<T> clazz, Object[] args) {
        for (Constructor constructor : clazz.getDeclaredConstructors()) { // cannot use <?> or <T> due to JDK 5/6 incompatibility
            if (matches(constructor.getParameterTypes(), args)) {
                return constructor;
            }
        }
        return null;
    }

    protected static boolean matches(Class<?>[] parameterTypes, Object[] args) {
        if ((parameterTypes == null) || (parameterTypes.length == 0)) {
            return ((args == null) || (args.length == 0));
        }
        if ((args == null) || (parameterTypes.length != args.length)) {
            return false;
        }
        for (int i = 0; i < parameterTypes.length; i++) {
            if ((args[i] != null) && !parameterTypes[i].isAssignableFrom(args[i].getClass())) {
                return false;
            }
        }
        return true;
    }
    
    protected static boolean fieldTypeCompatible(Object value, Field field) {
        if (value != null) {
            return field.getType().isAssignableFrom(value.getClass());
        } else {
            // Null can be set any field type
            return true;
        }
    }

    protected static ClassLoader getCustomClassLoader() {
        CommandContext commandContext = Context.getCommandContext();
        if (commandContext != null) {
            final ClassLoader classLoader = commandContext.getClassLoader();
            if (classLoader != null) {
                return classLoader;
            }
        }
        return null;
    }

    protected static Class loadClass(ClassLoader classLoader, String className) throws ClassNotFoundException {
        CommandContext commandContext = Context.getCommandContext();
        boolean useClassForName = commandContext == null || commandContext.isUseClassForNameClassLoading();
        return useClassForName ? Class.forName(className, true, classLoader) : classLoader.loadClass(className);
    }

    public static boolean isGetter(Method method) {
        String name = method.getName();
        Class<?> type = method.getReturnType();
        Class<?>[] params = method.getParameterTypes();

        if (!GETTER_PATTERN.matcher(name).matches()) {
            return false;
        }

        // special for isXXX boolean
        if (name.startsWith("is")) {
            return params.length == 0 && "boolean".equalsIgnoreCase(type.getSimpleName());
        }

        return params.length == 0 && !type.equals(Void.TYPE);
    }

    public static boolean isSetter(Method method, boolean allowBuilderPattern) {
        String name = method.getName();
        Class<?> type = method.getReturnType();
        Class<?>[] params = method.getParameterTypes();

        if (!SETTER_PATTERN.matcher(name).matches()) {
            return false;
        }

        return params.length == 1 && (type.equals(Void.TYPE) || (allowBuilderPattern && method.getDeclaringClass().isAssignableFrom(type)));
    }

    public static boolean isSetter(Method method) {
        return isSetter(method, false);
    }

    public static String getGetterShorthandName(Method method) {
        if (!isGetter(method)) {
            return method.getName();
        }

        String name = method.getName();
        if (name.startsWith("get")) {
            name = name.substring(3);
            name = name.substring(0, 1).toLowerCase(Locale.ENGLISH) + name.substring(1);
        } else if (name.startsWith("is")) {
            name = name.substring(2);
            name = name.substring(0, 1).toLowerCase(Locale.ENGLISH) + name.substring(1);
        }

        return name;
    }

    public static String getSetterShorthandName(Method method) {
        if (!isSetter(method)) {
            return method.getName();
        }

        String name = method.getName();
        if (name.startsWith("set")) {
            name = name.substring(3);
            name = name.substring(0, 1).toLowerCase(Locale.ENGLISH) + name.substring(1);
        }

        return name;
    }
}
