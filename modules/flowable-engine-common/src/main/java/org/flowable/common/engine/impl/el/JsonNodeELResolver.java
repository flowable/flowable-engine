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
package org.flowable.common.engine.impl.el;

import java.math.BigDecimal;
import java.util.Date;

import org.flowable.common.engine.impl.javax.el.CompositeELResolver;
import org.flowable.common.engine.impl.javax.el.ELContext;
import org.flowable.common.engine.impl.javax.el.ELException;
import org.flowable.common.engine.impl.javax.el.ELResolver;
import org.flowable.common.engine.impl.javax.el.PropertyNotWritableException;
import org.flowable.common.engine.impl.json.FlowableArrayNode;
import org.flowable.common.engine.impl.json.FlowableJsonNode;
import org.flowable.common.engine.impl.json.FlowableObjectNode;
import org.flowable.common.engine.impl.util.JsonUtil;

/**
 * Defines property resolution behavior on JsonNodes.
 * 
 * @see CompositeELResolver
 * @see ELResolver
 */
public class JsonNodeELResolver extends ELResolver {

    private final boolean readOnly;

    /**
     * Creates a new read/write JsonNodeELResolver.
     */
    public JsonNodeELResolver() {
        this(false);
    }

    /**
     * Creates a new JsonNodeELResolver whose read-only status is determined by the given parameter.
     */
    public JsonNodeELResolver(boolean readOnly) {
        this.readOnly = readOnly;
    }

    /**
     * If the base object is not null, returns the most general type that this resolver accepts for the property argument. Otherwise, returns null. Assuming the base is not null, this method will
     * always return Object.class. This is because any object is accepted as a key and is coerced into a string.
     * 
     * @param context
     *            The context of this evaluation.
     * @param base
     *            The bean to analyze.
     * @return null if base is null; otherwise Object.class.
     */
    @Override
    public Class<?> getCommonPropertyType(ELContext context, Object base) {
        return isResolvable(base) ? Object.class : null;
    }

    /**
     * If the base object is a map, returns the most general acceptable type for a value in this map. If the base is a Map, the propertyResolved property of the ELContext object must be set to true by
     * this resolver, before returning. If this property is not true after this method is called, the caller should ignore the return value. Assuming the base is a Map, this method will always return
     * Object.class. This is because Maps accept any object as the value for a given key.
     * 
     * @param context
     *            The context of this evaluation.
     * @param base
     *            The map to analyze. Only bases of type Map are handled by this resolver.
     * @param property
     *            The key to return the acceptable type for. Ignored by this resolver.
     * @return If the propertyResolved property of ELContext was set to true, then the most general acceptable type; otherwise undefined.
     * @throws NullPointerException
     *             if context is null
     * @throws ELException
     *             if an exception was thrown while performing the property or variable resolution. The thrown exception must be included as the cause property of this exception, if available.
     */
    @Override
    public Class<?> getType(ELContext context, Object base, Object property) {
        if (context == null) {
            throw new NullPointerException("context is null");
        }
        Class<?> result = null;
        if (isResolvable(base)) {
            result = Object.class;
            context.setPropertyResolved(true);
        }
        return result;
    }

    /**
     * If the base object is a map, returns the value associated with the given key, as specified by the property argument. If the key was not found, null is returned. If the base is a Map, the
     * propertyResolved property of the ELContext object must be set to true by this resolver, before returning. If this property is not true after this method is called, the caller should ignore the
     * return value. Just as in java.util.Map.get(Object), just because null is returned doesn't mean there is no mapping for the key; it's also possible that the Map explicitly maps the key to null.
     * 
     * @param context
     *            The context of this evaluation.
     * @param base
     *            The map to analyze. Only bases of type Map are handled by this resolver.
     * @param property
     *            The key to return the acceptable type for. Ignored by this resolver.
     * @return If the propertyResolved property of ELContext was set to true, then the value associated with the given key or null if the key was not found. Otherwise, undefined.
     * @throws ClassCastException
     *             if the key is of an inappropriate type for this map (optionally thrown by the underlying Map).
     * @throws NullPointerException
     *             if context is null, or if the key is null and this map does not permit null keys (the latter is optionally thrown by the underlying Map).
     * @throws ELException
     *             if an exception was thrown while performing the property or variable resolution. The thrown exception must be included as the cause property of this exception, if available.
     */
    @Override
    public Object getValue(ELContext context, Object base, Object property) {
        if (context == null) {
            throw new NullPointerException("context is null");
        }
        Object result = null;
        if (isResolvable(base)) {
            FlowableJsonNode baseNode = JsonUtil.asFlowableJsonNode(base);
            FlowableJsonNode resultNode = getResultNode(baseNode, property, context);
            if (resultNode != null && resultNode.isValueNode()) {
                if (resultNode.isBoolean()) {
                    result = resultNode.booleanValue();
                } else if (resultNode.isShort() || resultNode.isInt()) {
                    result = resultNode.intValue();
                } else if (resultNode.isLong()) {
                    result = resultNode.longValue();
                } else if (resultNode.isBigDecimal() || resultNode.isDouble() || resultNode.isFloat()) {
                    result = resultNode.doubleValue();
                } else if (resultNode.isString()) {
                    result = resultNode.asString();
                } else if (resultNode.isNull()) {
                    result = null;
                } else {
                    result = resultNode.getImplementationValue().toString();
                }

            } else if (resultNode != null) {
                result = resultNode.getImplementationValue();
            }
            context.setPropertyResolved(true);
        }
        return result;
    }

    protected FlowableJsonNode getResultNode(FlowableJsonNode base, Object property, ELContext context) {
        if (property instanceof String) {
            FlowableJsonNode propertyNode = base.get((String) property);
            if (propertyNode != null) {
                return propertyNode;
            }

            if (!readOnly && base instanceof FlowableObjectNode && context.getContext(EvaluationState.class) == EvaluationState.WRITE) {
                // The base does not have the requested property, so add it and return it, only if we are in write evaluation state
                return ((FlowableObjectNode) base).putObject((String) property);
            }
            return null;
        } else if (property instanceof Number) {
            int requestedIndex = ((Number) property).intValue();
            if (requestedIndex >= 0) {
                return base.get(requestedIndex);
            } else {
                // If the requested index is negative then lookup the element from the tail
                // using plus because requestedIndex is negative so size() + (-1) is the last element
                return base.get(base.size() + requestedIndex);
            }
        } else {
            return base.get(property.toString());
        }
    }

    /**
     * If the base object is a map, returns whether a call to {@link #setValue(ELContext, Object, Object, Object)} will always fail. If the base is a Map, the propertyResolved property of the
     * ELContext object must be set to true by this resolver, before returning. If this property is not true after this method is called, the caller should ignore the return value. If this resolver
     * was constructed in read-only mode, this method will always return true. If a Map was created using java.util.Collections.unmodifiableMap(Map), this method must return true. Unfortunately, there
     * is no Collections API method to detect this. However, an implementation can create a prototype unmodifiable Map and query its runtime type to see if it matches the runtime type of the base
     * object as a workaround.
     * 
     * @param context
     *            The context of this evaluation.
     * @param base
     *            The map to analyze. Only bases of type Map are handled by this resolver.
     * @param property
     *            The key to return the acceptable type for. Ignored by this resolver.
     * @return If the propertyResolved property of ELContext was set to true, then true if calling the setValue method will always fail or false if it is possible that such a call may succeed;
     *         otherwise undefined.
     * @throws NullPointerException
     *             if context is null.
     * @throws ELException
     *             if an exception was thrown while performing the property or variable resolution. The thrown exception must be included as the cause property of this exception, if available.
     */
    @Override
    public boolean isReadOnly(ELContext context, Object base, Object property) {
        if (context == null) {
            throw new NullPointerException("context is null");
        }
        if (isResolvable(base)) {
            context.setPropertyResolved(true);
        }
        return readOnly;
    }
    
    @Override
	public Object invoke(ELContext context, Object base, Object method, Class<?>[] paramTypes, Object[] params) {
        if (!isResolvable(base)) {
            return null;
        }
        if (method == null) {
            return null;
        }
        if (params.length == 0) {
            String methodName = method.toString();
            if (methodName.equals("asString") || methodName.equals("asText")) {
                String value = JsonUtil.asFlowableJsonNode(base).asString();
                context.setPropertyResolved(true);
                return value;
            }
            return null;

        } else if (params.length != 1) {
            return null;
        }
        Object param = params[0];
        if (!(param instanceof Long)) {
            return null;
        }

        int index = ((Long) param).intValue();
        String methodName = method.toString();
        if (methodName.equals("path")) {
            FlowableJsonNode valueNode = JsonUtil.asFlowableJsonNode(base).path(index);
            context.setPropertyResolved(true);
            return valueNode.getImplementationValue();
        } else if (methodName.equals("get")) {
            FlowableJsonNode valueNode = JsonUtil.asFlowableJsonNode(base).get(index);
            context.setPropertyResolved(true);
            return valueNode != null ? valueNode.getImplementationValue() : null;
        }

        return null;
	}

    /**
     * If the base object is a map, attempts to set the value associated with the given key, as specified by the property argument. If the base is a Map, the propertyResolved property of the ELContext
     * object must be set to true by this resolver, before returning. If this property is not true after this method is called, the caller can safely assume no value was set. If this resolver was
     * constructed in read-only mode, this method will always throw PropertyNotWritableException. If a Map was created using java.util.Collections.unmodifiableMap(Map), this method must throw
     * PropertyNotWritableException. Unfortunately, there is no Collections API method to detect this. However, an implementation can create a prototype unmodifiable Map and query its runtime type to
     * see if it matches the runtime type of the base object as a workaround.
     * 
     * @param context
     *            The context of this evaluation.
     * @param base
     *            The map to analyze. Only bases of type Map are handled by this resolver.
     * @param property
     *            The key to return the acceptable type for. Ignored by this resolver.
     * @param value
     *            The value to be associated with the specified key.
     * @throws ClassCastException
     *             if the class of the specified key or value prevents it from being stored in this map.
     * @throws NullPointerException
     *             if context is null, or if this map does not permit null keys or values, and the specified key or value is null.
     * @throws IllegalArgumentException
     *             if some aspect of this key or value prevents it from being stored in this map.
     * @throws PropertyNotWritableException
     *             if this resolver was constructed in read-only mode, or if the put operation is not supported by the underlying map.
     * @throws ELException
     *             if an exception was thrown while performing the property or variable resolution. The thrown exception must be included as the cause property of this exception, if available.
     */
    @Override
    public void setValue(ELContext context, Object base, Object property, Object value) {
        if (context == null) {
            throw new NullPointerException("context is null");
        }
        if (isResolvable(base)) {
            FlowableJsonNode baseNode = JsonUtil.asFlowableJsonNode(base);
            if (baseNode instanceof FlowableObjectNode objectNode) {
                setValue(context, objectNode, property, value);
            } else if (baseNode instanceof FlowableArrayNode arrayNode) {
                setValue(context, arrayNode, property, value);
            }
        }
    }

    protected void setValue(ELContext context, FlowableObjectNode node, Object property, Object value) {
        if (readOnly) {
            throw new PropertyNotWritableException("resolver is read-only");
        }

        String propertyName = property.toString();
        if (value instanceof BigDecimal bigDecimal) {
            node.put(propertyName, bigDecimal);

        } else if (value instanceof Boolean booleanValue) {
            node.put(propertyName, booleanValue);

        } else if (value instanceof Integer integerValue) {
            node.put(propertyName, integerValue);
        } else if (value instanceof Long longValue) {
            node.put(propertyName, longValue);

        } else if (value instanceof Double doubleValue) {
            node.put(propertyName, doubleValue);

        } else if (JsonUtil.isJsonNode(value)) {
            node.set(propertyName, JsonUtil.asFlowableJsonNode(value));
        } else if (value instanceof CharSequence charSequence) {
            node.put(propertyName, charSequence.toString());
        } else if (value instanceof Date date) {
            node.put(propertyName, date.toInstant().toString());
        } else if (value != null) {
            node.put(propertyName, value.toString());

        } else {
            node.putNull(propertyName);
        }
        context.setPropertyResolved(true);
    }

    protected void setValue(ELContext context, FlowableArrayNode node, Object property, Object value) {
        if (readOnly) {
            throw new PropertyNotWritableException("resolver is read-only");
        }

        int index = toIndex(property);
        if (value instanceof BigDecimal bigDecimal) {
            node.set(index, bigDecimal);

        } else if (value instanceof Boolean booleanValue) {
            node.set(index, booleanValue);

        } else if (value instanceof Integer integerValue) {
            node.set(index, integerValue);
        } else if (value instanceof Long longValue) {
            node.set(index, longValue);

        } else if (value instanceof Double doubleValue) {
            node.set(index, doubleValue);

        } else if (JsonUtil.isJsonNode(value)) {
            node.set(index, JsonUtil.asFlowableJsonNode(value));
        } else if (value instanceof CharSequence charSequence) {
            node.set(index, charSequence.toString());
        } else if (value instanceof Date date) {
            node.set(index, date.toInstant().toString());
        } else if (value != null) {
            node.set(index, value.toString());

        } else {
            node.setNull(index);
        }

        context.setPropertyResolved(true);
    }

    protected int toIndex(Object property) {
        int index;
        if (property instanceof Number) {
            index = ((Number) property).intValue();
        } else if (property instanceof String) {
            try {
                index = Integer.valueOf((String) property);
            } catch (NumberFormatException e) {
                throw new IllegalArgumentException("Cannot parse array index: " + property, e);
            }
        } else {
            throw new IllegalArgumentException("Cannot coerce property to array index: " + property);
        }

        return index;
    }

    /**
     * Test whether the given base should be resolved by this ELResolver.
     * 
     * @param base
     *            The bean to analyze.
     * @return base != null
     */
    private final boolean isResolvable(Object base) {
        return JsonUtil.isJsonNode(base);
    }
}
