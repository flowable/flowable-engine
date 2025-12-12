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

import java.util.Objects;
import java.util.Optional;

/**
 * Defines property resolution, method invocation and type conversion behaviour on {@link Optional}s.
 * <p>
 * This resolver handles base objects that are instances of {@link Optional}.
 * <p>
 * This resolver is always a read-only resolver since {@link Optional} instances are immutable.
 *
 * @since EL 6.0
 */
public class OptionalELResolver extends ELResolver {

    /**
     * {@inheritDoc}
     *
     * @return If the base object is an {@link Optional} and {@link Optional#isEmpty()} returns {@code true} then the
     * resulting value is {@code null}.
     * <p>
     * If the base object is an {@link Optional}, {@link Optional#isPresent()} returns {@code true} and the
     * property is {@code null} then the resulting value is the result of calling {@link Optional#get()} on
     * the base object.
     * <p>
     * If the base object is an {@link Optional}, {@link Optional#isPresent()} returns {@code true} and the
     * property is not {@code null} then the resulting value is the result of calling
     * {@link ELResolver#getValue(ELContext, Object, Object)} using the {@link ELResolver} obtained from
     * {@link ELContext#getELResolver()} with the following parameters:
     * <ul>
     * <li>The {@link ELContext} is the current context</li>
     * <li>The base object is the result of calling {@link Optional#get()} on the current base object</li>
     * <li>The property object is the current property object</li>
     * </ul>
     * <p>
     * If the base object is not an {@link Optional} then the return value is undefined.
     */
    @Override
    public Object getValue(ELContext context, Object base, Object property) {
        Objects.requireNonNull(context);

        if (base instanceof Optional) {
            context.setPropertyResolved(base, property);
            if (((Optional<?>) base).isPresent()) {
                if (property == null) {
                    return ((Optional<?>) base).get();
                } else {
                    Object resolvedBase = ((Optional<?>) base).get();
                    return context.getELResolver().getValue(context, resolvedBase, property);
                }
            }
        }

        return null;
    }

    /**
     * {@inheritDoc}
     *
     * @return If the base object is an {@link Optional} this method always returns {@code null} since instances of this
     * resolver are always read-only.
     * <p>
     * If the base object is not an {@link Optional} then the return value is undefined.
     */
    @Override
    public Class<?> getType(ELContext context, Object base, Object property) {
        Objects.requireNonNull(context);

        if (base instanceof Optional) {
            context.setPropertyResolved(base, property);
        }

        return null;
    }

    /**
     * {@inheritDoc}
     * <p>
     * If the base object is an {@link Optional} this method always throws a {@link PropertyNotWritableException} since
     * instances of this resolver are always read-only.
     */
    @Override
    public void setValue(ELContext context, Object base, Object property, Object value) {
        Objects.requireNonNull(context);

        if (base instanceof Optional) {
            throw new PropertyNotWritableException("ELResolver not writable for type '" + base.getClass().getName() + "'");
        }
    }

    /**
     * {@inheritDoc}
     *
     * @return If the base object is an {@link Optional} this method always returns {@code true} since instances of this
     * resolver are always read-only.
     * <p>
     * If the base object is not an {@link Optional} then the return value is undefined.
     */
    @Override
    public boolean isReadOnly(ELContext context, Object base, Object property) {
        Objects.requireNonNull(context);

        if (base instanceof Optional) {
            context.setPropertyResolved(base, property);
            return true;
        }

        return false;
    }

    /**
     * {@inheritDoc}
     *
     * @return If the base object is an {@link Optional} this method always returns {@code Object.class}.
     * <p>
     * If the base object is not an {@link Optional} then the return value is undefined.
     */
    @Override
    public Class<?> getCommonPropertyType(ELContext context, Object base) {
        if (base instanceof Optional) {
            return Object.class;
        }

        return null;
    }

    /**
     * {@inheritDoc}
     *
     * @return If the base object is an {@link Optional} and {@link Optional#isEmpty()} returns {@code true} then this
     * method returns the result of coercing {@code null} to the requested {@code type}.
     * <p>
     * If the base object is an {@link Optional} and {@link Optional#isPresent()} returns {@code true} then
     * this method returns the result of coercing {@code Optional#get()} to the requested {@code type}.
     * <p>
     * If the base object is not an {@link Optional} then the return value is undefined.
     */
    @Override
    public <T> T convertToType(ELContext context, Object obj, Class<T> type) {
        Objects.requireNonNull(context);
        if (obj instanceof Optional) {
            Object value = null;
            if (((Optional<?>) obj).isPresent()) {
                value = ((Optional<?>) obj).get();
                // If the value is assignable to the required type, do so.
                if (type.isAssignableFrom(value.getClass())) {
                    context.setPropertyResolved(true);
                    @SuppressWarnings("unchecked")
                    T result = (T) value;
                    return result;
                }
            }

            try {
                T convertedValue = context.convertToType(value, type);
                context.setPropertyResolved(true);
                return convertedValue;
            } catch (ELException e) {
                /*
                 * TODO: This isn't pretty but it works. Significant refactoring would be required to avoid the
                 * exception. See also Util.isCoercibleFrom().
                 */
            }
        }
        return null;
    }

    /**
     * {@inheritDoc}
     *
     * @return If the base object is an {@link Optional} and {@link Optional#isEmpty()} returns {@code true} then this
     * method returns {@code null}.
     * <p>
     * If the base object is an {@link Optional} and {@link Optional#isPresent()} returns {@code true} then
     * this method returns the result of invoking the specified method on the object obtained by calling
     * {@link Optional#get()} with the specified parameters.
     * <p>
     * If the base object is not an {@link Optional} then the return value is undefined.
     */
    @Override
    public Object invoke(ELContext context, Object base, Object method, Class<?>[] paramTypes, Object[] params) {
        Objects.requireNonNull(context);

        if (base instanceof Optional && method != null) {
            context.setPropertyResolved(base, method);
            if (((Optional<?>) base).isEmpty()) {
                return null;
            } else {
                Object resolvedBase = ((Optional<?>) base).get();
                return context.getELResolver().invoke(context, resolvedBase, method, paramTypes, params);
            }
        }

        return null;
    }
}
