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

import java.util.Collections;
import java.util.HashMap;
import java.util.Map;
import java.util.Objects;

public class MapELResolver extends ELResolver {

	private static final Class<?> UNMODIFIABLE = Collections.unmodifiableMap(new HashMap<>()).getClass();

	private final boolean readOnly;

	public MapELResolver() {
		this(false);
	}

	public MapELResolver(boolean readOnly) {
		this.readOnly = readOnly;
	}

	@Override
	public Class<?> getType(ELContext context, Object base, Object property) {
		Objects.requireNonNull(context, "context is null");

		if (isResolvable(base)) {
			context.setPropertyResolved(base, property);

			if (readOnly || base.getClass() == UNMODIFIABLE) {
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
			context.setPropertyResolved(base, property);
			return ((Map<?, ?>) base).get(property);
		}

		return null;
	}

	@Override
	public void setValue(ELContext context, Object base, Object property, Object value) {
		Objects.requireNonNull(context, "context is null");

		if (isResolvable(base)) {
			context.setPropertyResolved(base, property);

			if (readOnly) {
				throw new PropertyNotWritableException("resolver is read-only");
			}

			try {
				@SuppressWarnings("unchecked") // Must be OK
				Map<Object, Object> map = ((Map<Object, Object>) base);
				map.put(property, value);
			} catch (UnsupportedOperationException e) {
				throw new PropertyNotWritableException(e);
			}

		}
	}

	@Override
	public boolean isReadOnly(ELContext context, Object base, Object property) {
		Objects.requireNonNull(context, "context is null");

		if (isResolvable(base)) {
			context.setPropertyResolved(base, property);
			return this.readOnly || UNMODIFIABLE.equals(base.getClass());
		}

		return readOnly;
	}

	@Override
	public Class<?> getCommonPropertyType(ELContext context, Object base) {
		return isResolvable(base) ? Object.class : null;
	}

	/**
	 * Test whether the given base should be resolved by this ELResolver.
	 * 
	 * @param base
	 *            The bean to analyze.
	 * @return base instanceof Map
	 */
	private boolean isResolvable(Object base) {
		return base instanceof Map<?,?>;
	}
}
