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

import java.util.Arrays;
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
            Object formattedValue = DMNParseUtil.getFormattedValue(value.toString(), targetCollection);
            return targetCollection.contains(formattedValue);
        }
    }

    public static boolean notContains(Object collection, Object value) {

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
            Object formattedValue = DMNParseUtil.getFormattedValue(value.toString(), targetCollection);
            return !targetCollection.contains(formattedValue);
        }
    }

    public static boolean containsAny(Object collection, Object value) {

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
            return valueCollection != null && CollectionUtils.containsAny(targetCollection, valueCollection);
        } else if (DMNParseUtil.isJavaCollection(value)) {
            return CollectionUtils.containsAny(targetCollection, (Collection) value);
        } else if (DMNParseUtil.isArrayNode(value)) {
            Collection valueCollection = DMNParseUtil.getCollectionFromArrayNode((ArrayNode) value);
            return valueCollection != null && CollectionUtils.containsAny(targetCollection, valueCollection);
        } else {
            Object formattedValue = DMNParseUtil.getFormattedValue(value.toString(),targetCollection);
            return targetCollection.contains(formattedValue);
        }
    }

    public static boolean notContainsAny(Object collection, Object value) {

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
            return valueCollection == null || !CollectionUtils.containsAny(targetCollection, valueCollection);
        } else if (DMNParseUtil.isJavaCollection(value)) {
            return !CollectionUtils.containsAny(targetCollection, (Collection) value);
        } else if (DMNParseUtil.isArrayNode(value)) {
            Collection valueCollection = DMNParseUtil.getCollectionFromArrayNode((ArrayNode) value);
            return valueCollection == null || !CollectionUtils.containsAny(targetCollection, valueCollection);
        } else {
            Object formattedValue = DMNParseUtil.getFormattedValue(value.toString(), targetCollection);
            return !targetCollection.contains(formattedValue);
        }
    }
    public static boolean in(Object collection, Object value) {
    	
    		return contains(value, collection);

    } public static boolean notIn(Object collection, Object value) {
    	
    		return notContains(value, collection);

    } public static boolean any(Object collection, Object value) {
    	
    		return containsAny(value, collection);

    }
    public static boolean notAny(Object collection, Object value) {

    		return notContainsAny(value, collection);
    	
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
