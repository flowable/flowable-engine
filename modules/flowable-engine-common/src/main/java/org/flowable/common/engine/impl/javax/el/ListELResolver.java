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

import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import java.util.Objects;

public class ListELResolver extends ELResolver {
	private final boolean readOnly;

	private static final Class<?> UNMODIFIABLE = Collections.unmodifiableList(new ArrayList<>()).getClass();

	public ListELResolver() {
		this(false);
	}

	public ListELResolver(boolean readOnly) {
		this.readOnly = readOnly;
	}

	@Override
	public Class<?> getType(ELContext context, Object base, Object property) {
		Objects.requireNonNull(context, "context is null");

		if (isResolvable(base)) {
			int idx = coerce(property);
			checkBounds((List<?>) base, idx);
			context.setPropertyResolved(base, property);

			/*
			 * Not perfect as a custom list implementation may be read-only but consistent with isReadOnly().
			 */
			if (base.getClass() == UNMODIFIABLE || readOnly) {
				return null;
			}

			return Object.class;
		}

		return null;
	}

	@Override
	public Object getValue(ELContext context, Object base, Object property) {
		Objects.requireNonNull(context, "context is null");


		if (isResolvable(base)) {
			int idx = coerce(property);
			context.setPropertyResolved(base, property);
			List<?> list = (List<?>) base;
			if (idx < 0 || idx >= list.size()) {
				return null;
			}
			return list.get(idx);
		}

		return null;
	}

	@Override
	public void setValue(ELContext context, Object base, Object property, Object value) {
		Objects.requireNonNull(context, "context is null");

		if (isResolvable(base)) {
			context.setPropertyResolved(base, property);
			@SuppressWarnings("unchecked") // Must be OK to cast to Object
			List<Object> list = (List<Object>) base;
			if (readOnly) {
				throw new PropertyNotWritableException("resolver is read-only");
			}

			int idx = coerce(property);
			try {
				list.set(idx, value);
			} catch (UnsupportedOperationException e) {
				throw new PropertyNotWritableException(e);
			} catch (IndexOutOfBoundsException e) {
				throw new PropertyNotFoundException(e);
			}
		}
	}

	@Override
	public boolean isReadOnly(ELContext context, Object base, Object property) {
		Objects.requireNonNull(context, "context is null");

		if (isResolvable(base)) {
			List<?> list = (List<?>) base;
			context.setPropertyResolved(base, property);
			try {
				int idx = coerce(property);
				checkBounds(list, idx);
			} catch (IllegalArgumentException e) {
				// ignore
			}
			return this.readOnly || UNMODIFIABLE.equals(list.getClass());
		}
		return readOnly;
	}

	@Override
	public Class<?> getCommonPropertyType(ELContext context, Object base) {
		return isResolvable(base) ? Integer.class : null;
	}

	/**
	 * Test whether the given base should be resolved by this ELResolver.
	 * 
	 * @param base
	 *            The bean to analyze.
	 * @return base instanceof List
	 */
	private static boolean isResolvable(Object base) {
		return base instanceof List<?>;
	}

	private static void checkBounds(List<?> list, int idx) {
		if (idx < 0 || idx >= list.size()) {
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
				throw new IllegalArgumentException("Cannot parse list index: " + property, e);
			}
		}
		throw new IllegalArgumentException("Cannot coerce property to list index: " + property);
	}
}
