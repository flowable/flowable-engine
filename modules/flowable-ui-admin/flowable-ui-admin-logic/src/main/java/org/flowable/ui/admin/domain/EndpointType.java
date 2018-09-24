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
package org.flowable.ui.admin.domain;

import java.util.HashMap;
import java.util.Map;

/**
 * @author Yvo Swillens
 */
public enum EndpointType {

    PROCESS(1), DMN(2), FORM(3), CONTENT(4), CMMN(5), APP(6);

    private static Map<Integer, EndpointType> map = new HashMap<>();

    static {
        for (EndpointType endpointType : EndpointType.values()) {
            map.put(endpointType.endpointCode, endpointType);
        }
    }

    private final int endpointCode;

    EndpointType(int endpointCode) {
        this.endpointCode = endpointCode;
    }

    public int getEndpointCode() {
        return this.endpointCode;
    }

    public static EndpointType valueOf(int endpointCode) {
        return map.get(endpointCode);
    }

}
