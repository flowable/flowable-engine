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

import java.lang.reflect.InvocationTargetException;
import java.lang.reflect.Method;
import java.util.HashMap;
import java.util.Map;
import java.util.Objects;
import java.util.WeakHashMap;
import java.util.concurrent.ConcurrentHashMap;

// This class is adapted to match the BeanELResolver from Tomcat from https://github.com/apache/tomcat/tree/febda9acf2a9d6ed833382c4c49eec8964bc1431/java/jakarta/el
// The adaptations are done in order for us to use the Util class for finding methods
/**
 * Defines property resolution behavior on objects using the JavaBeans component architecture.
 *
 * <p>
 * If the {@code java.beans.*} package is available (part of the {@code java.desktop} module) the JavaBeans
 * implementation provided by the JRE is used. If the {@code java.beans.*} package is not available, a built-in
 * stand-alone implementation is used that just provides getter/setter support (as everything else requires classes from
 * {@code java.beans.*}).
 *
 * <p>
 * This resolver handles base objects of any type, as long as the base is not <code>null</code>. It accepts any object
 * as a property or method, and coerces it to a string.
 *
 * <p>
 * For property resolution, the property string is used to find a JavaBeans compliant property on the base object. The
 * value is accessed using JavaBeans getters and setters.
 * </p>
 *
 * <p>
 * For method resolution, the method string is the name of the method in the bean. The parameter types can be optionally
 * specified to identify the method. If the parameter types are not specified, the parameter objects are used in the
 * method resolution.
 * </p>
 *
 * <p>
 * The JavaBeans specification predates the introduction of default method implementations defined on an interface. In
 * addition to the JavaBeans specification requirements for looking up property getters, property setters and methods,
 * this resolver also considers default methods and includes them in the results.
 * </p>
 *
 * <p>
 * The JavaBeans specification predates the introduction of Modules. In addition to the JavaBeans specification
 * requirements for looking up property getters, property setters and methods, this resolver also considers module
 * visibility.
 * </p>
 *
 * <p>
 * This resolver can be constructed in read-only mode, which means that {@link #isReadOnly} will always return
 * <code>true</code> and {@link #setValue} will always throw <code>PropertyNotWritableException</code>.
 * </p>
 *
 * <p>
 * <code>ELResolver</code>s are combined together using {@link CompositeELResolver}s, to define rich semantics for
 * evaluating an expression. See the javadocs for {@link ELResolver} for details.
 * </p>
 *
 * <p>
 * Because this resolver handles base objects of any type, it should be placed near the end of a composite resolver.
 * Otherwise, it will claim to have resolved a property before any resolvers that come after it get a chance to test if
 * they can do so as well.
 * </p>
 *
 * @see CompositeELResolver
 * @see ELResolver
 *
 * @since 2.1
 */
public class BeanELResolver extends ELResolver {

	private static final int CACHE_SIZE;
	private static final String CACHE_SIZE_PROP = "org.flowable.common.engine.impl.el.BeanELResolver.CACHE_SIZE";

	static {
		CACHE_SIZE = Integer.getInteger(CACHE_SIZE_PROP, 1000);
	}

	private final boolean readOnly;

	private final ConcurrentCache<String, BeanProperties> cache;

	/**
	 * Creates a writable instance of the standard JavaBean resolver.
	 */
	public BeanELResolver() {
		this(false);
	}

	/**
	 * Creates an instance of the standard JavaBean resolver.
	 *
	 * @param readOnly {@code true} if the created instance should be read-only otherwise false.
	 */
	public BeanELResolver(boolean readOnly) {
		this.readOnly = readOnly;
		this.cache = new ConcurrentCache<>(CACHE_SIZE);
	}

	@Override
	public Class<?> getType(ELContext context, Object base, Object property) {
		Objects.requireNonNull(context, "context is null");

		if (base == null || property == null) {
			return null;
		}

		BeanProperty beanProperty = property(base, property);

		if (readOnly || beanProperty == null) {
			return null;
		}

		context.setPropertyResolved(base, property);
		if (beanProperty.isReadOnly(base)) {
			return null;
		}
		return beanProperty.getPropertyType();
	}

	@Override
	public Object getValue(ELContext context, Object base, Object property) {
		Objects.requireNonNull(context, "context is null");
		if (base == null || property == null) {
			return null;
		}

		BeanProperty beanProperty = property(base, property);
		if (beanProperty == null) {
			return null;
		}
		Method m = beanProperty.read(base);
		if (m == null) {
			return null;
		}
		context.setPropertyResolved(base, property);
		try {
			return invoke(m, base, (Object[]) null);
		} catch (InvocationTargetException e) {
			Throwable cause = e.getCause();
			Util.handleThrowable(cause);
            throw new ELException("Error reading '" + property + "' on type '" + base.getClass().getName() + "'", cause);
		} catch (Exception e) {
			throw new ELException(e);
		}
	}

	@Override
	public void setValue(ELContext context, Object base, Object property, Object value) {
		Objects.requireNonNull(context, "context is null");
		if (base == null || property == null) {
			return;
		}

		if (readOnly) {
			throw new PropertyNotWritableException("resolver is read-only");
		}
		BeanProperty beanProperty = property(base, property);

		if (beanProperty == null) {
			return;
		}

		Method method = beanProperty.write(base);
		if (method == null) {
			throw new PropertyNotWritableException("Cannot write property: '" + property + "' on type " + base.getClass().getName());
		}
		context.setPropertyResolved(base, property);
		try {
			invoke(method, base, value);
		} catch (InvocationTargetException e) {
			Throwable cause = e.getCause();
			Util.handleThrowable(cause);
			throw new ELException("Error '" + property + "' on type '" + base.getClass().getName() + "'", cause);
		} catch (Exception e) {
			throw new ELException(e);
		}
	}

	@Override
	public Object invoke(ELContext context, Object base, Object method, Class<?>[] paramTypes, Object[] params) {
		Objects.requireNonNull(context, "context is null");
		if (base == null || method == null) {
			return null;
		}

		Object result = null;
		if (params == null) {
			params = new Object[0];
		}
		String name = method.toString();
		Method target = Util.findMethod(context, base.getClass(), base, name, paramTypes, params);
		if (target == null) {
			throw new MethodNotFoundException("Cannot find method " + name + " with " + params.length + " parameters in " + base.getClass());
		}

		Object[] parameters = Util.buildParameters(context, target.getParameterTypes(), target.isVarArgs(), params);
		try {
			result = invoke(target, base, parameters);
		} catch (InvocationTargetException e) {
			Throwable cause = e.getCause();
			Util.handleThrowable(cause);
			throw new ELException(cause);
		} catch (IllegalArgumentException | IllegalAccessException e) {
			throw new ELException(e);
		}
		context.setPropertyResolved(base, method);
		return result;
	}

	protected Object invoke(Method target, Object base, Object... parameters) throws InvocationTargetException, IllegalAccessException {
		return target.invoke(base, parameters);
	}

	@Override
	public boolean isReadOnly(ELContext context, Object base, Object property) {
		Objects.requireNonNull(context, "context is null");
		if (base == null || property == null) {
			return false;
		}

		BeanProperty beanProperty = property(base, property);
		if (beanProperty == null) {
			return false;
		}

		context.setPropertyResolved(base, property);
		if (this.readOnly) {
			return true;
		}
		return beanProperty.isReadOnly(base);
	}

	@Override
	public Class<?> getCommonPropertyType(ELContext context, Object base) {
		if (base == null) {
			return null;
		}
		return Object.class;
	}

	abstract static class BeanProperties {

		protected final Map<String, BeanProperty> properties;
		protected final Class<?> type;

		BeanProperties(Class<?> type) throws ELException {
			this.type = type;
			this.properties = new HashMap<>();
		}

		private BeanProperty get(String name) {
			return this.properties.get(name);
		}

		private Class<?> getType() {
			return type;
		}
	}

	abstract static class BeanProperty {

		private final Class<?> type;

		private final Class<?> owner;

		private Method read;

		private Method write;

		BeanProperty(Class<?> owner, Class<?> type) {
			this.owner = owner;
			this.type = type;
		}

		public Class<?> getPropertyType() {
			return this.type;
		}

		public boolean isReadOnly(Object base) {
			return write(base) == null;
		}

		private Method write(Object base) {
			if (this.write == null) {
				this.write = Util.getMethod(this.owner, base, getWriteMethod());
			}
			return this.write;
		}

		private Method read(Object base) {
			if (this.read == null) {
				this.read = Util.getMethod(this.owner, base, getReadMethod());
			}
			return this.read;
		}

		abstract Method getWriteMethod();

		abstract Method getReadMethod();

		abstract String getName();
	}

	private BeanProperty property(Object base, Object property) {
		Class<?> type = base.getClass();
		String prop = property.toString();

		BeanProperties props = this.cache.get(type.getName());
		if (props == null || type != props.getType()) {
			props = BeanSupport.getInstance().getBeanProperties(type);
			this.cache.put(type.getName(), props);
		}

		return props.get(prop);
	}

	private static final class ConcurrentCache<K, V> {

		private final int size;
		private final Map<K, V> eden;
		private final Map<K, V> longterm;

		ConcurrentCache(int size) {
			this.size = size;
			this.eden = new ConcurrentHashMap<>(size);
			this.longterm = new WeakHashMap<>(size);
		}

		public V get(K key) {
			V value = this.eden.get(key);
			if (value == null) {
				synchronized (longterm) {
					value = this.longterm.get(key);
				}
				if (value != null) {
					this.eden.put(key, value);
				}
			}
			return value;
		}

		public void put(K key, V value) {
			if (this.eden.size() >= this.size) {
				synchronized (longterm) {
					this.longterm.putAll(this.eden);
				}
				this.eden.clear();
			}
			this.eden.put(key, value);
		}

	}
}
