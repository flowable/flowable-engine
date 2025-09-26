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

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.node.ArrayNode;

public class CandidateUtil {

    public static Collection<String> extractCandidates(Object value) {
        if (value instanceof Collection) {
            return (Collection<String>) value;
        } else if (value instanceof ArrayNode valueArrayNode) {
            Collection<String> candidates = new ArrayList<>(valueArrayNode.size());
            for (JsonNode node : valueArrayNode) {
                candidates.add(node.asText());
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
