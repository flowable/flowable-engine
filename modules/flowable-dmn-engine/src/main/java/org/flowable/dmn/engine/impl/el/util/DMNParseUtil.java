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

import java.util.Collection;
import java.util.List;
import java.util.stream.Collectors;
import java.util.stream.Stream;

/**
 * @author Yvo Swillens
 */
public class DMNParseUtil {

    public static boolean isCollection(Object collection) {
        if (Collection.class.isAssignableFrom(collection.getClass()) == false) {
           return false;
        }
        return true;
    }

    public static boolean isDMNCollection(Object value) {
        if (value instanceof String == false || value.equals("")) {
            return false;
        }

        String stringValue = String.valueOf(value);
        return stringValue.contains(",");
    }

    public static Collection getCollectionFromDMNCollection(Object value) {
        String stringValue = String.valueOf(value);
        List<Object> items = split(stringValue);
        return items;
    }

    private static List<Object> split(String str) {
        return Stream.of(str.split(","))
            .map(elem -> formatElementValue(elem.trim()))
            .collect(Collectors.toList());
    }


    //TODO: DATES
    protected static Object formatElementValue(String value) {
        if (value.isEmpty()) {
            return null;
        }
        if (value.startsWith("\"") && value.endsWith("\"")) {
            String result = value.substring(1, value.length() - 1);

            // Boolean
            if ("true".equalsIgnoreCase(value) || "false".equalsIgnoreCase(value)) {
                return Boolean.valueOf(value);
            } else {
                return result;
            }
        } else {
            return Long.valueOf(value);
        }
    }
}
