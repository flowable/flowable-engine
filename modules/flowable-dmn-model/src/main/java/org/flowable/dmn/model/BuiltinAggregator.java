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
package org.flowable.dmn.model;

import java.util.EnumSet;
import java.util.HashMap;
import java.util.Map;

/**
 * @author Yvo Swillens
 */
public enum BuiltinAggregator {

    SUM("SUM"),
    COUNT("COUNT"),
    MIN("MIN"),
    MAX("MAX");

    private static final Map<String, BuiltinAggregator> lookup = new HashMap<>();

    static {
        for (BuiltinAggregator builtinAggregator : EnumSet.allOf(BuiltinAggregator.class))
            lookup.put(builtinAggregator.getValue(), builtinAggregator);
    }

    private final String value;

    BuiltinAggregator(String value) {
        this.value = value;
    }

    public String getValue() {
        return value;
    }

    public static BuiltinAggregator get(String value) {
        return lookup.get(value);
    }
}
