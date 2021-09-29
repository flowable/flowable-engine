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
package org.flowable.common.engine.impl.util;

import java.util.ArrayList;
import java.util.Collection;
import java.util.Collections;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.function.Consumer;

import org.flowable.common.engine.api.FlowableIllegalArgumentException;

/**
 * helper/convenience methods for working with collections.
 * 
 * @author Joram Barrez
 */
public class CollectionUtil {

    // No need to instantiate
    private CollectionUtil() {
    }

    /**
     * Helper method that creates a singleton map.
     * 
     * Alternative for Collections.singletonMap(), since that method returns a generic typed map depending on the input type, but we often need a String, Object map.
     */
    public static Map<String, Object> singletonMap(String key, Object value) {
        Map<String, Object> map = new HashMap<>(1);
        map.put(key, value);
        return map;
    }

    /**
     * Helper method to easily create a map.
     * 
     * Takes as input a varargs containing the key1, value1, key2, value2, etc. Note: although an Object, we will cast the key to String internally.
     */
    public static Map<String, Object> map(Object... objects) {

        if (objects.length % 2 != 0) {
            throw new FlowableIllegalArgumentException("The input should always be even since we expect a list of key-value pairs!");
        }

        Map<String, Object> map = new HashMap<>();
        for (int i = 0; i < objects.length; i += 2) {
            map.put((String) objects[i], objects[i + 1]);
        }

        return map;
    }

    public static boolean isEmpty(@SuppressWarnings("rawtypes") Collection collection) {
        return (collection == null || collection.isEmpty());
    }

    public static boolean isNotEmpty(@SuppressWarnings("rawtypes") Collection collection) {
        return !isEmpty(collection);
    }

    public static <T> List<List<T>> partition(Collection<T> values, int partitionSize) {
        if (values == null) {
            return null;
        } else if (values.isEmpty()) {
            return Collections.emptyList();
        }

        List<T> valuesList;
        if (values instanceof List) {
            valuesList = (List<T>) values;
        } else {
            valuesList = new ArrayList<>(values);
        }

        int valuesSize = values.size();

        if (valuesSize <= partitionSize) {
            return Collections.singletonList(valuesList);
        }

        List<List<T>> safeValuesList = new ArrayList<>();

        consumePartitions(values, partitionSize, safeValuesList::add);

        return safeValuesList;
    }

    public static <T> void consumePartitions(Collection<T> values, int partitionSize, Consumer<List<T>> partitionConsumer) {
        int valuesSize = values.size();
        List<T> valuesList;
        if (values instanceof List) {
            valuesList = (List<T>) values;
        } else {
            valuesList = new ArrayList<>(values);
        }

        if (valuesSize <= partitionSize) {
            partitionConsumer.accept(valuesList);

        } else {

            for (int startIndex = 0; startIndex < valuesSize; startIndex += partitionSize) {

                int endIndex = startIndex + partitionSize;
                if (endIndex > valuesSize) {
                    endIndex = valuesSize; // endIndex in #subList is exclusive
                }

                List<T> subList = valuesList.subList(startIndex, endIndex);
                partitionConsumer.accept(subList);
            }

        }
    }

}
