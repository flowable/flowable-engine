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
package org.flowable.common.engine.impl.assignment;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collection;
import java.util.Collections;

import org.apache.commons.lang3.StringUtils;
import org.flowable.common.engine.impl.json.FlowableArrayNode;
import org.flowable.common.engine.impl.json.FlowableJsonNode;
import org.flowable.common.engine.impl.util.JsonUtil;

public class CandidateUtil {

    public static Collection<String> extractCandidates(Object value) {
        if (value instanceof Collection) {
            return (Collection<String>) value;
        } else if (JsonUtil.isArrayNode(value)) {
            FlowableArrayNode valueArrayNode = JsonUtil.asFlowableArrayNode(value);
            Collection<String> candidates = new ArrayList<>(valueArrayNode.size());
            for (FlowableJsonNode node : valueArrayNode) {
                candidates.add(node.asString());
            }

            return candidates;
        } else if (value != null) {
            String str = value.toString();
            if (StringUtils.isNotEmpty(str)) {
                return Arrays.asList(value.toString().split("[\\s]*,[\\s]*"));
            }
        }

        return Collections.emptyList();
    }

}
