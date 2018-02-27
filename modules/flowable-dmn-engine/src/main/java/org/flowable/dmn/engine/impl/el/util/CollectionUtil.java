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

        if (DMNParseUtil.isDMNCollection(value)) {
            Collection valueCollection = DMNParseUtil.getCollectionFromDMNCollection(value);

            if (valueCollection != null) {
                return ((Collection) collection).containsAll(valueCollection);
            } else return false;
        } else {
            return ((Collection) collection).contains(value);
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

        if (DMNParseUtil.isDMNCollection(value)) {
            Collection valueCollection = DMNParseUtil.getCollectionFromDMNCollection(value);

            if (valueCollection != null) {
                return !((Collection) collection).containsAll(valueCollection);
            } else return true;
        } else {
            return !((Collection) collection).contains(value);
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

        if (DMNParseUtil.isDMNCollection(value)) {
            Collection valueCollection = DMNParseUtil.getCollectionFromDMNCollection(value);

            if (valueCollection != null) {
                return CollectionUtils.containsAny((Collection) collection, valueCollection);
            } else return false;
        } else {
            return ((Collection) collection).contains(value);
        }
    }

}
