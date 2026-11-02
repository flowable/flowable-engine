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
import java.util.Objects;

/**
 * Defines property resolution behavior on arrays.
 *
 * <p>
 * This resolver handles base objects that are Java language arrays. It accepts the case sensitive string {@code
 * "length"} or any other object as a property and coerces that object into an integer index into the array. The
 * resulting value is the value in the array at that index.
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
 * @see CompositeELResolver
 * @see ELResolver
 *
 * @since 2.1
 */
public class ArrayELResolver extends ELResolver {

	private static final String LENGTH_PROPERTY_NAME = "length";

	private final boolean readOnly;

	/**
	 * Creates a writable instance of the standard array resolver.
	 */
	public ArrayELResolver() {
		this(false);
	}

	/**
	 * Creates an instance of the standard array resolver.
	 *
	 * @param readOnly {@code true} if the created instance should be read-only otherwise false.
	 */
	public ArrayELResolver(boolean readOnly) {
		this.readOnly = readOnly;
	}

	/**
	 * If the base object is an array, returns the most general acceptable type for a value in this array.
	 *
	 * <p>
	 * If the base is a <code>array</code>, the <code>propertyResolved</code> property of the <code>ELContext</code> object
	 * must be set to <code>true</code> by this resolver, before returning. If this property is not <code>true</code> after
	 * this method is called, the caller should ignore the return value.
	 * </p>
	 *
	 * <p>
	 * Assuming the base is an <code>array</code>, that this resolver was not constructed in read-only mode, and that
	 * the provided property can be coerced to a valid index for base array, this method will return
	 * <code>base.getClass().getComponentType()</code>, which is the most general type of component that can be stored
	 * at any given index in the array.
	 * </p>
	 *
	 * @param context The context of this evaluation.
	 * @param base The array to analyze. Only bases that are Java language arrays are handled by this resolver.
	 * @param property The case sensitive string {@code "length"} or the index of the element in the array to return the
	 * acceptable type for. If not the case sensitive string {@code "length"}, will be coerced into an integer and
	 * checked if it is a valid index for the array, but otherwise ignored by this resolver.
	 * @return If the <code>propertyResolved</code> property of <code>ELContext</code> was set to <code>true</code>, then
	 * the most general acceptable type which must be {@code null} if the either the property or the resolver is
	 * read-only; otherwise undefined
	 * @throws PropertyNotFoundException if the given index is out of bounds for this array.
	 * @throws NullPointerException if context is <code>null</code>
	 * @throws ELException if an exception was thrown while performing the property or variable resolution. The thrown
	 * exception must be included as the cause property of this exception, if available.
	 */
	@Override
	public Class<?> getType(ELContext context, Object base, Object property) {
		Objects.requireNonNull(context, "context is null");

		if (isResolvable(base)) {
			context.setPropertyResolved(base, property);

			if (LENGTH_PROPERTY_NAME.equals(property)) {
				// Always read-only
				return null;
			}

			try {
				int idx = coerce(property);
				checkBounds(base, idx);
			} catch (IllegalArgumentException e) {
				// ignore
			}
			/*
			 * The resolver may have been created in read-only mode but the array and its elements will always be
			 * read-write.
			 */
			if (readOnly) {
				return null;
			}
			return base.getClass().getComponentType();
		}
		return null;
	}

	/**
	 * If the base object is a Java language array, returns the length of the array or the value at the given index. If
	 * the {@code property} argument is the case sensitive string {@code "length"}, then the length of the array is
	 * returned. Otherwise, the {@code property} argument is coerced into an integer and used as the index of the value
	 * to be returned. If the coercion could not be performed, an {@code IllegalArgumentException} is thrown. If the
	 * index is out of bounds, {@code null} is returned.
	 *
	 * <p>
	 * If the base is a Java language array, the {@code propertyResolved} property of the {@code ELContext} object must
	 * be set to {@code true} by this resolver, before returning. If this property is not {@code true} after this method
	 * is called, the caller should ignore the return value.
	 * </p>
	 *
	 * @param context The context of this evaluation.
	 * @param base The array to analyze. Only bases that are Java language arrays are handled by this resolver.
	 * @param property Either the string {@code "length"} or the index of the value to be returned. An index value will
	 * be coerced into an integer.
	 * @return If the {@code propertyResolved} property of {@code ELContext} was set to {@code true}, then the length of
	 * the array, the value at the given index or {@code null} if the index was out of bounds. Otherwise, undefined.
	 * @throws IllegalArgumentException if the property was not the string {@code "length"} and could not be coerced
	 * into an integer.
	 * @throws NullPointerException if context is {@code null}.
	 * @throws ELException if an exception was thrown while performing the property or variable resolution. The thrown
	 * exception must be included as the cause property of this exception, if available.
	 */
	@Override
	public Object getValue(ELContext context, Object base, Object property) {
		Objects.requireNonNull(context, "context is null");

		if (isResolvable(base)) {
			context.setPropertyResolved(base, property);
			if (LENGTH_PROPERTY_NAME.equals(property)) {
				return Array.getLength(base);
			}
			int idx = coerce(property);
			if (idx < 0 || idx >= Array.getLength(base)) {
				return null;
			}
			return Array.get(base, idx);
		}

		return null;
	}

	/**
	 * If the base object is a Java language array and the property is not the case sensitive string {@code length},
	 * attempts to set the value at the given index with the given value. The index is specified by the
	 * <code>property</code> argument, and coerced into an integer. If the coercion could not be performed, an
	 * <code>IllegalArgumentException</code> is thrown. If the index is out of bounds, a
	 * <code>PropertyNotFoundException</code> is thrown.
	 *
	 * <p>
	 * If the base is a Java language array, the <code>propertyResolved</code> property of the <code>ELContext</code> object
	 * must be set to <code>true</code> by this resolver, before returning. If this property is not <code>true</code> after
	 * this method is called, the caller can safely assume no value was set.
	 * </p>
	 *
	 * <p>
	 * If this resolver was constructed in read-only mode or the property is the case sensitive string {@code length},
	 * this method will always throw <code>PropertyNotWritableException</code>.
	 * </p>
	 *
	 * @param context The context of this evaluation.
	 * @param base The array to be modified. Only bases that are Java language arrays are handled by this resolver.
	 * @param property The case sensitive string {@code length} or an object to coerce to an integer to provide the
	 * index of the value to be set.
	 * @param val The value to be set at the given index.
	 * @throws ClassCastException if the class of the specified element prevents it from being added to this array.
	 * @throws NullPointerException if context is <code>null</code>.
	 * @throws IllegalArgumentException if the property could not be coerced into an integer, or if some aspect of the
	 * specified element prevents it from being added to this array.
	 * @throws PropertyNotWritableException if this resolver was constructed in read-only mode or the property was the
	 * case sensitive string {@code length}.
	 * @throws PropertyNotFoundException if the given index is out of bounds for this array.
	 * @throws ELException if an exception was thrown while performing the property or variable resolution. The thrown
	 * exception must be included as the cause property of this exception, if available.
	 */
	@Override
	public void setValue(ELContext context, Object base, Object property, Object value) {
		Objects.requireNonNull(context, "context is null");

		if (isResolvable(base)) {
			context.setPropertyResolved(base, property);

			if (LENGTH_PROPERTY_NAME.equals(property)) {
				throw new PropertyNotWritableException("Property '" + property + "' is not writable on '" + base.getClass().getName() + "'");
			}

			if (readOnly) {
				throw new PropertyNotWritableException("resolver is read-only");
			}
			int idx = coerce(property);
			checkBounds(base, idx);
			if (value != null && !Util.isAssignableFrom(value.getClass(), base.getClass().getComponentType())) {
				throw new ClassCastException(
						"Unable to add an object of type '" + value.getClass().getName() + "' to an array of objects of type '" + base.getClass()
								.getComponentType().getName() + "'");
			}
			Array.set(base, idx, value);
		}
	}

	/**
	 * If the base object is a Java language array, returns whether a call to {@link #setValue} will always fail.
	 *
	 * <p>
	 * If the base is a Java language array, the <code>propertyResolved</code> property of the <code>ELContext</code> object
	 * must be set to <code>true</code> by this resolver, before returning. If this property is not <code>true</code> after
	 * this method is called, the caller should ignore the return value.
	 * </p>
	 *
	 * <p>
	 * If this resolver was constructed in read-only mode or the property is the case sensitive string {@code length},
	 * this method will always return <code>true</code>. Otherwise, it returns <code>false</code>.
	 * </p>
	 *
	 * @param context The context of this evaluation.
	 * @param base The array to analyze. Only bases that are a Java language array are handled by this resolver.
	 * @param property The case sensitive string {@code length} or an object to coerce to an integer to provide the
	 * index to check if an attempt to call {@link #setValue} with that index will always fail.
	 * @return If the <code>propertyResolved</code> property of <code>ELContext</code> was set to <code>true</code>, then
	 * <code>true</code> if calling the <code>setValue</code> method will always fail or <code>false</code> if it is
	 * possible that such a call may succeed; otherwise undefined.
	 * @throws PropertyNotFoundException if the given index is out of bounds for this array.
	 * @throws NullPointerException if context is <code>null</code>
	 * @throws ELException if an exception was thrown while performing the property or variable resolution. The thrown
	 * exception must be included as the cause property of this exception, if available.
	 */
	@Override
	public boolean isReadOnly(ELContext context, Object base, Object property) {
		Objects.requireNonNull(context, "context is null");

		if (isResolvable(base)) {
			context.setPropertyResolved(base, property);
			if (LENGTH_PROPERTY_NAME.equals(property)) {
				// Always read-only
				return true;
			}
			try {
				int idx = coerce(property);
				checkBounds(base, idx);
			} catch (IllegalArgumentException e) {
				// ignore
			}
		}
		return readOnly;
	}

	/**
	 * If the base object is a Java language array, returns the most general type that this resolver accepts for the
	 * <code>property</code> argument. Otherwise, returns <code>null</code>.
	 *
	 * <p>
	 * Assuming the base is an array, this method will always return <code>Integer.class</code>. This is because arrays
	 * accept integers for their index.
	 * </p>
	 *
	 * @param context The context of this evaluation.
	 * @param base The array to analyze. Only bases that are a Java language array are handled by this resolver.
	 * @return <code>null</code> if base is not a Java language array; otherwise <code>Integer.class</code>.
	 */
	@Override
	public Class<?> getCommonPropertyType(ELContext context, Object base) {
		return isResolvable(base) ? Integer.class : null;
	}

	/**
	 * Test whether the given base should be resolved by this ELResolver.
	 * 
	 * @param base
	 *            The bean to analyze.
	 * @return base != null && base.getClass().isArray()
	 */
	private final boolean isResolvable(Object base) {
		return base != null && base.getClass().isArray();
	}

	private static void checkBounds(Object base, int idx) {
		if (idx < 0 || idx >= Array.getLength(base)) {
			throw new PropertyNotFoundException(new ArrayIndexOutOfBoundsException(idx).getMessage());
		}
	}

	private static int coerce(Object property) {
		if (property instanceof Number) {
			return ((Number) property).intValue();
		}
		if (property instanceof Character) {
			return (Character) property;
		}
		if (property instanceof Boolean) {
			return (Boolean) property ? 1 : 0;
		}
		if (property instanceof String) {
			try {
				return Integer.parseInt((String) property);
			} catch (NumberFormatException e) {
				throw new IllegalArgumentException("Cannot parse array index: " + property, e);
			}
		}

		throw new IllegalArgumentException("Cannot coerce property to array index: " + property);
	}
}
