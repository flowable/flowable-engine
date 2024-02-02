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
package org.flowable.dmn.engine.impl.el.util;

import java.util.Arrays;
import java.util.Collection;

import org.springframework.util.CollectionUtils;

import com.fasterxml.jackson.databind.node.ArrayNode;

/**
 * @author Yvo Swillens
 */
public class CollectionUtil {

    /**
     * all values of value must be in collection
     *
     * @return {@code true} if all elements of value are within the collection,
     * {@code false} if at least one element of value is not within the collection
     */
    public static boolean allOf(Object collection, Object value) {

        if (collection == null) {
            throw new IllegalArgumentException("collection cannot be null");
        }

        if (value == null) {
            throw new IllegalArgumentException("value cannot be null");
        }

        // collection to check against
        Collection targetCollection = getTargetCollection(collection, value);

        // elements to check
        if (DMNParseUtil.isParseableCollection(value)) {
            Collection valueCollection = DMNParseUtil.parseCollection(value, targetCollection);
            return valueCollection != null && targetCollection.containsAll(valueCollection);
        } else if (DMNParseUtil.isJavaCollection(value)) {
            return targetCollection.containsAll((Collection) value);
        } else if (DMNParseUtil.isArrayNode(value)) {
            Collection valueCollection = DMNParseUtil.getCollectionFromArrayNode((ArrayNode) value);
            return valueCollection != null && targetCollection.containsAll(valueCollection);
        } else {
            Object formattedValue = DMNParseUtil.getFormattedValue(value, targetCollection);
            return targetCollection.contains(formattedValue);
        }
    }

    /**
     * @deprecated use @{link #allOf(Object, Object)} instead
     */
    @Deprecated
    public static boolean contains(Object collection, Object value) {
        return allOf(collection, value);
    }

    /**
     * none of the values of value must be in collection
     *
     * @return {@code true} if all elements of value are not within the collection,
     * {@code false} if at least one element of value is within the collection
     */
    public static boolean noneOf(Object collection, Object value) {

        if (collection == null) {
            throw new IllegalArgumentException("collection cannot be null");
        }

        if (value == null) {
            throw new IllegalArgumentException("value cannot be null");
        }

        // collection to check against
        Collection targetCollection = getTargetCollection(collection, value);

        // elements to check
        if (DMNParseUtil.isParseableCollection(value)) {
            Collection valueCollection = DMNParseUtil.parseCollection(value, targetCollection);
            return !CollectionUtils.containsAny(targetCollection, valueCollection);
        } else if (DMNParseUtil.isJavaCollection(value)) {
            return !CollectionUtils.containsAny(targetCollection, (Collection) value);
        } else if (DMNParseUtil.isArrayNode(value)) {
            Collection valueCollection = DMNParseUtil.getCollectionFromArrayNode((ArrayNode) value);
            return !CollectionUtils.containsAny(targetCollection, valueCollection);
        } else {
            Object formattedValue = DMNParseUtil.getFormattedValue(value, targetCollection);
            return !targetCollection.contains(formattedValue);
        }
    }

    /**
     * @deprecated use @{link #noneOf(Object, Object)} instead
     */
    @Deprecated
    public static boolean notContains(Object collection, Object value) {
        return noneOf(collection, value);
    }

    /**
     * one of the values of value must be in collection
     *
     * @return {@code true} if at least one element of value is within the collection,
     * {@code false} if all elements of value are not within the collection
     */
    public static boolean anyOf(Object collection, Object value) {

        if (collection == null) {
            throw new IllegalArgumentException("collection cannot be null");
        }

        if (value == null) {
            throw new IllegalArgumentException("value cannot be null");
        }

        // collection to check against
        Collection targetCollection = getTargetCollection(collection, value);

        // elements to check
        if (DMNParseUtil.isParseableCollection(value)) {
            Collection valueCollection = DMNParseUtil.parseCollection(value, targetCollection);
            return CollectionUtils.containsAny(targetCollection, valueCollection);
        } else if (DMNParseUtil.isJavaCollection(value)) {
            return CollectionUtils.containsAny(targetCollection, (Collection) value);
        } else if (DMNParseUtil.isArrayNode(value)) {
            Collection valueCollection = DMNParseUtil.getCollectionFromArrayNode((ArrayNode) value);
            return CollectionUtils.containsAny(targetCollection, valueCollection);
        } else {
            Object formattedValue = DMNParseUtil.getFormattedValue(value, targetCollection);
            return targetCollection.contains(formattedValue);
        }
    }

    /**
     * @deprecated use @{link #anyof(Object, Object)} instead
     */
    @Deprecated
    public static boolean containsAny(Object collection, Object value) {
        return anyOf(collection, value);
    }

    /**
     * one of the values of value must not be in collection
     *
     * @return {@code true} if a least one element of value is not within the collection,
     * {@code false} if all elements of value are within the collection
     */
    public static boolean notAllOf(Object collection, Object value) {

        if (collection == null) {
            throw new IllegalArgumentException("collection cannot be null");
        }

        if (value == null) {
            throw new IllegalArgumentException("value cannot be null");
        }

        // collection to check against
        Collection targetCollection = getTargetCollection(collection, value);

        // elements to check
        if (DMNParseUtil.isParseableCollection(value)) {
            Collection valueCollection = DMNParseUtil.parseCollection(value, targetCollection);
            return valueCollection == null || !targetCollection.containsAll(valueCollection);
        } else if (DMNParseUtil.isJavaCollection(value)) {
            return !targetCollection.containsAll((Collection) value);
        } else if (DMNParseUtil.isArrayNode(value)) {
            Collection valueCollection = DMNParseUtil.getCollectionFromArrayNode((ArrayNode) value);
            return valueCollection == null || !targetCollection.containsAll(valueCollection);
        } else {
            Object formattedValue = DMNParseUtil.getFormattedValue(value, targetCollection);
            return !targetCollection.contains(formattedValue);
        }
    }

    /**
     * @deprecated use {@link #notAllOf(Object, Object)} instead
     */
    @Deprecated
    public static boolean notContainsAny(Object collection, Object value) {
        return notAllOf(collection, value);
    }

    protected static Collection getTargetCollection(Object collection, Object value) {
        Collection targetCollection;
        if (!DMNParseUtil.isCollection(collection)) {
            if (DMNParseUtil.isParseableCollection(collection)) {
                targetCollection = DMNParseUtil.parseCollection(collection, value);
            } else {
                targetCollection = Arrays.asList(collection);
            }
        } else if (DMNParseUtil.isArrayNode(collection)) {
            targetCollection = DMNParseUtil.getCollectionFromArrayNode((ArrayNode) collection);
        } else {
            targetCollection = (Collection) collection;
        }

        return targetCollection;
    }

}
