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

/**
 * A convenient class for writing an ELResolver to do custom type conversions.
 *
 * <p>
 * For example, to convert a String to an instance of MyDate, one can write
 *
 * <pre>
 * <code>
 *     ELProcessor elp = new ELProcessor();
 *     elp.getELManager().addELResolver(new TypeConverter() {
 *         Object convertToType(ELContext context, Object obj, Class&lt;?&gt; type) {
 *             if ((obj instanceof String) &amp;&amp; type == MyDate.class) {
 *                 context.setPropertyResolved(obj, type);
 *                 return (obj == null)? null: new MyDate(obj.toString());
 *             }
 *             return null;
 *         }
 *      };
 * </code>
 * </pre>
 *
 * @since Jakarta Expression Language 3.0
 */
public abstract class TypeConverter extends ELResolver {

    @Override
    public Object getValue(ELContext context, Object base, Object property) {
        return null;
    }

    @Override
    public Class<?> getType(ELContext context, Object base, Object property) {
        return null;
    }

    @Override
    public void setValue(ELContext context, Object base, Object property, Object value) {
        // NO-OP
    }

    @Override
    public boolean isReadOnly(ELContext context, Object base, Object property) {
        return false;
    }

    @Override
    public Class<?> getCommonPropertyType(ELContext context, Object base) {
        return null;
    }

    /**
     * Converts an object to a specific type.
     *
     * <p>
     * An <code>ELException</code> is thrown if an error occurs during the conversion.
     * </p>
     *
     * @param context The context of this evaluation.
     * @param obj The object to convert.
     * @param targetType The target type for the conversion.
     * @throws ELException thrown if errors occur.
     */
    @Override
    public abstract <T> T convertToType(ELContext context, Object obj, Class<T> targetType);
}
