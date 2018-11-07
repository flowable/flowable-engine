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
public enum HitPolicy {

    UNIQUE("UNIQUE"),
    FIRST("FIRST"),
    PRIORITY("PRIORITY"),
    ANY("ANY"),
    UNORDERED("UNORDERED"),
    RULE_ORDER("RULE ORDER"),
    OUTPUT_ORDER("OUTPUT ORDER"),
    COLLECT("COLLECT");

    private static final Map<String, HitPolicy> lookup = new HashMap<>();

    static {
        for (HitPolicy hitPolicy : EnumSet.allOf(HitPolicy.class))
            lookup.put(hitPolicy.getValue(), hitPolicy);
    }

    private final String value;

    HitPolicy(String value) {
        this.value = value;
    }

    public String getValue() {
        return value;
    }

    public static HitPolicy get(String value) {
        return lookup.get(value);
    }
}
