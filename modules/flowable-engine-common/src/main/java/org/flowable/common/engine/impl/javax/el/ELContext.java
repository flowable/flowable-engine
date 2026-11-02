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
import java.util.ArrayDeque;
import java.util.ArrayList;
import java.util.Collections;
import java.util.Deque;
import java.util.HashMap;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import java.util.Objects;

public abstract class ELContext {

	private Locale locale;

    private Map<Class<?>, Object> map;

	private boolean resolved;

    private List<EvaluationListener> listeners;

    private Deque<Map<String, Object>> lambdaArguments = null;

    public ELContext() {
        this.resolved = false;
    }

    private Deque<Map<String, Object>> getLambdaArguments() {
        if (lambdaArguments == null) {
            lambdaArguments = new ArrayDeque<>(4);
        }
        return lambdaArguments;
    }

    public void setPropertyResolved(boolean resolved) {
        this.resolved = resolved;
    }

    /**
     * Mark the given property as resolved and notify any interested listeners.
     *
     * @param base The base object on which the property was found
     * @param property The property that was resolved
     * @since EL 3.0
     */
    public void setPropertyResolved(Object base, Object property) {
        setPropertyResolved(true);
        notifyPropertyResolved(base, property);
    }

    public boolean isPropertyResolved() {
        return this.resolved;
    }

    /**
     * Add an object to this EL context under the given key.
     *
     * @param key The key under which to store the object
     * @param contextObject The object to add
     * @throws NullPointerException If the supplied key or context is <code>null</code>
     */
    public void putContext(Class<?> key, Object contextObject) {
        Objects.requireNonNull(key);

        if (this.map == null) {
            this.map = new HashMap<>();
        }

        this.map.put(key, contextObject);
    }

	/**
     * Obtain the context object for the given key.
     *
     * @param key The key of the required context object
     *
     * @return The value of the context object associated with the given key
     *
     * @throws NullPointerException If the supplied key is <code>null</code>
     */
    public Object getContext(Class<?> key) {
		if (key == null) {
			throw new NullPointerException("key is null");
		}
		if (this.map == null) {
            return null;
        }
        return this.map.get(key);
    }

    public abstract ELResolver getELResolver();

    public abstract FunctionMapper getFunctionMapper();

	public Locale getLocale() {
        return this.locale;
    }

    public void setLocale(Locale locale) {
		this.locale = locale;
	}

    public abstract VariableMapper getVariableMapper();

    /**
     * Register an EvaluationListener with this ELContext.
     *
     * @param listener The EvaluationListener to register
     * @since EL 3.0
     */
    public void addEvaluationListener(EvaluationListener listener) {
        if (listeners == null) {
            listeners = new ArrayList<>();
        }

        listeners.add(listener);
    }

    /**
     * Obtain the list of registered EvaluationListeners.
     *
     * @return A list of the EvaluationListener registered with this ELContext
     * @since EL 3.0
     */
    public List<EvaluationListener> getEvaluationListeners() {
        return listeners == null ? Collections.emptyList() : listeners;
    }

    /**
     * Notify interested listeners that an expression will be evaluated.
     *
     * @param expression The expression that will be evaluated
     * @since EL 3.0
     */
    public void notifyBeforeEvaluation(String expression) {
        if (listeners == null) {
            return;
        }

        for (EvaluationListener listener : listeners) {
            try {
                listener.beforeEvaluation(this, expression);
            } catch (Throwable t) {
                Util.handleThrowable(t);
                // Ignore - no option to log
            }
        }
    }

    /**
     * Notify interested listeners that an expression has been evaluated.
     *
     * @param expression The expression that was evaluated
     * @since EL 3.0
     */
    public void notifyAfterEvaluation(String expression) {
        if (listeners == null) {
            return;
        }

        for (EvaluationListener listener : listeners) {
            try {
                listener.afterEvaluation(this, expression);
            } catch (Throwable t) {
                Util.handleThrowable(t);
                // Ignore - no option to log
            }
        }
    }

    /**
     * Notify interested listeners that a property has been resolved.
     *
     * @param base The object on which the property was resolved
     * @param property The property that was resolved
     * @since EL 3.0
     */
    public void notifyPropertyResolved(Object base, Object property) {
        if (listeners == null) {
            return;
        }

        for (EvaluationListener listener : listeners) {
            try {
                listener.propertyResolved(this, base, property);
            } catch (Throwable t) {
                Util.handleThrowable(t);
                // Ignore - no option to log
            }
        }
    }

    /**
     * Determine if the specified name is recognised as the name of a lambda argument.
     *
     * @param name The name of the lambda argument
     * @return <code>true</code> if the name is recognised as the name of a lambda argument, otherwise
     * <code>false</code>
     * @since EL 3.0
     */
    public boolean isLambdaArgument(String name) {
        for (Map<String, Object> arguments : getLambdaArguments()) {
            if (arguments.containsKey(name)) {
                return true;
            }
        }
        return false;
    }

    /**
     * Obtain the value of the lambda argument with the given name.
     *
     * @param name The name of the lambda argument
     *
     * @return The value of the specified argument
     *
     * @since EL 3.0
     */
    public Object getLambdaArgument(String name) {
        for (Map<String, Object> arguments : getLambdaArguments()) {
            Object result = arguments.get(name);
            if (result != null) {
                return result;
            }
        }
        return null;
    }

    /**
     * Called when starting to evaluate a lambda expression so that the arguments are available to the EL context during
     * evaluation.
     *
     * @param arguments The arguments in scope for the current lambda expression.
     * @since EL 3.0
     */
    public void enterLambdaScope(Map<String, Object> arguments) {
        getLambdaArguments().push(arguments);
    }

    /**
     * Called after evaluating a lambda expression to signal that the arguments are no longer required.
     *
     * @since EL 3.0
     */
    public void exitLambdaScope() {
        getLambdaArguments().pop();
    }

    /**
     * Coerce the supplied object to the requested type.
     *
     * @param <T> The type to which the object should be coerced
     * @param obj The object to be coerced
     * @param type The type to which the object should be coerced
     * @return An instance of the requested type.
     * @throws ELException If the conversion fails
     * @since EL 3.0
     */
    public <T> T convertToType(Object obj, Class<T> type) {

        boolean originalResolved = isPropertyResolved();
        setPropertyResolved(false);
        try {
            ELResolver resolver = getELResolver();
            if (resolver != null) {
                T result = resolver.convertToType(this, obj, type);
                if (isPropertyResolved()) {
                    return result;
                }
            }
        } finally {
            setPropertyResolved(originalResolved);
        }

        if (obj instanceof LambdaExpression && isFunctionalInterface(type)) {
            ((LambdaExpression) obj).setELContext(this);
        }

        return getExpressionFactory().coerceToType(obj, type);
    }

    /**
     * Lookup an expression factory used to coerce method parameters in context under key
     * <code>"javax.el.ExpressionFactory"</code>.
     * If no expression factory can be found under that key, use a default instance created with
     * {@link ExpressionFactory#newInstance()}.
     *
     * @return expression factory instance
     */
    protected ExpressionFactory getExpressionFactory() {
        Object obj = getContext(ExpressionFactory.class);
        if (obj instanceof ExpressionFactory) {
            return (ExpressionFactory) obj;
        }
        return getDefaultExpressionFactory();
    }

    protected ExpressionFactory getDefaultExpressionFactory() {
        return ExpressionFactory.newInstance();
    }

    /*
     * Copied from org.apache.el.lang.ELSupport - keep in sync
     */
    static boolean isFunctionalInterface(Class<?> type) {

        if (!type.isInterface()) {
            return false;
        }

        boolean foundAbstractMethod = false;
        Method[] methods = type.getMethods();
        for (Method method : methods) {
            if (Modifier.isAbstract(method.getModifiers())) {
                // Abstract methods that override one of the public methods
                // of Object don't count
                if (overridesObjectMethod(method)) {
                    continue;
                }
                if (foundAbstractMethod) {
                    // Found more than one
                    return false;
                } else {
                    foundAbstractMethod = true;
                }
            }
        }
        return foundAbstractMethod;
    }

    /*
     * Copied from org.apache.el.lang.ELSupport - keep in sync
     */
    private static boolean overridesObjectMethod(Method method) {
        // There are three methods that can be overridden
        if ("equals".equals(method.getName())) {
            if (method.getReturnType().equals(boolean.class)) {
                if (method.getParameterCount() == 1) {
                    return method.getParameterTypes()[0].equals(Object.class);
                }
            }
        } else if ("hashCode".equals(method.getName())) {
            if (method.getReturnType().equals(int.class)) {
                return method.getParameterCount() == 0;
            }
        } else if ("toString".equals(method.getName())) {
            if (method.getReturnType().equals(String.class)) {
                return method.getParameterCount() == 0;
            }
        }

        return false;
    }
}
