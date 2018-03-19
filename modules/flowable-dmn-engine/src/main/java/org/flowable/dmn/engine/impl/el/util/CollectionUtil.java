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

import com.fasterxml.jackson.databind.node.ArrayNode;
import org.springframework.util.CollectionUtils;

import java.util.Collection;

/**
 * @author Yvo Swillens
 */
public class CollectionUtil {

    public static boolean contains(Object collection, Object value) {

        if (collection == null) {
            throw new IllegalArgumentException("collection cannot be null");
        }

        if (value == null) {
            throw new IllegalArgumentException("value cannot be null");
        }

        DMNParseUtil.isCollection(collection);

        if (DMNParseUtil.isArrayNode(collection)) {
            collection = DMNParseUtil.getCollectionFromArrayNode((ArrayNode) collection);
        }

        if (DMNParseUtil.isDMNCollection(value)) {
            Collection valueCollection = DMNParseUtil.getCollectionFromDMNCollection(value, (Collection) collection);
            return valueCollection != null && ((Collection) collection).containsAll(valueCollection);
        } else if (DMNParseUtil.isJavaCollection(value)) {
            return ((Collection) collection).containsAll((Collection) value);
        } else if (DMNParseUtil.isArrayNode(value)) {
            Collection valueCollection = DMNParseUtil.getCollectionFromArrayNode((ArrayNode) value);
            return valueCollection != null && ((Collection) collection).containsAll(valueCollection);
        } else {
            Object formattedValue = DMNParseUtil.getFormattedValue(value.toString(), (Collection) collection);
            return ((Collection) collection).contains(formattedValue);
        }
    }

    public static boolean notContains(Object collection, Object value) {

        if (collection == null) {
            throw new IllegalArgumentException("collection cannot be null");
        }

        if (value == null) {
            throw new IllegalArgumentException("value cannot be null");
        }

        DMNParseUtil.isCollection(collection);

        if (DMNParseUtil.isArrayNode(collection)) {
            collection = DMNParseUtil.getCollectionFromArrayNode((ArrayNode) collection);
        }

        if (DMNParseUtil.isDMNCollection(value)) {
            Collection valueCollection = DMNParseUtil.getCollectionFromDMNCollection(value, (Collection) collection);
            return valueCollection == null || !((Collection) collection).containsAll(valueCollection);
        } else if (DMNParseUtil.isJavaCollection(value)) {
            return !((Collection) collection).containsAll((Collection) value);
        } else if (DMNParseUtil.isArrayNode(value)) {
            Collection valueCollection = DMNParseUtil.getCollectionFromArrayNode((ArrayNode) value);
            return valueCollection == null || !((Collection) collection).containsAll(valueCollection);
        } else {
            Object formattedValue = DMNParseUtil.getFormattedValue(value.toString(), (Collection) collection);
            return !((Collection) collection).contains(formattedValue);
        }
    }

    public static boolean containsAny(Object collection, Object value) {

        if (collection == null) {
            throw new IllegalArgumentException("collection cannot be null");
        }

        if (value == null) {
            throw new IllegalArgumentException("value cannot be null");
        }

        DMNParseUtil.isCollection(collection);

        if (DMNParseUtil.isArrayNode(collection)) {
            collection = DMNParseUtil.getCollectionFromArrayNode((ArrayNode) collection);
        }

        if (DMNParseUtil.isDMNCollection(value)) {
            Collection valueCollection = DMNParseUtil.getCollectionFromDMNCollection(value, (Collection) collection);
            return valueCollection != null && CollectionUtils.containsAny((Collection) collection, valueCollection);
        } else if (DMNParseUtil.isJavaCollection(value)) {
            return CollectionUtils.containsAny((Collection) collection, (Collection) value);
        } else if (DMNParseUtil.isArrayNode(value)) {
            Collection valueCollection = DMNParseUtil.getCollectionFromArrayNode((ArrayNode) value);
            return valueCollection != null && CollectionUtils.containsAny((Collection) collection, valueCollection);
        } else {
            Object formattedValue = DMNParseUtil.getFormattedValue(value.toString(), (Collection) collection);
            return ((Collection) collection).contains(formattedValue);
        }
    }

    public static boolean notContainsAny(Object collection, Object value) {

        if (collection == null) {
            throw new IllegalArgumentException("collection cannot be null");
        }

        if (value == null) {
            throw new IllegalArgumentException("value cannot be null");
        }

        DMNParseUtil.isCollection(collection);

        if (DMNParseUtil.isArrayNode(collection)) {
            collection = DMNParseUtil.getCollectionFromArrayNode((ArrayNode) collection);
        }

        if (DMNParseUtil.isDMNCollection(value)) {
            Collection valueCollection = DMNParseUtil.getCollectionFromDMNCollection(value, (Collection) collection);
            return valueCollection == null || !CollectionUtils.containsAny((Collection) collection, valueCollection);
        } else if (DMNParseUtil.isJavaCollection(value)) {
            return !CollectionUtils.containsAny((Collection) collection, (Collection) value);
        } else if (DMNParseUtil.isArrayNode(value)) {
            Collection valueCollection = DMNParseUtil.getCollectionFromArrayNode((ArrayNode) value);
            return valueCollection == null || !CollectionUtils.containsAny((Collection) collection, valueCollection);
        } else {
            Object formattedValue = DMNParseUtil.getFormattedValue(value.toString(), (Collection) collection);
            return !((Collection) collection).contains(formattedValue);
        }
    }

}
