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
package org.flowable.dmn.engine.impl.hitpolicy;

import org.apache.commons.lang3.builder.CompareToBuilder;
import org.flowable.dmn.engine.impl.context.Context;
import org.flowable.dmn.engine.impl.mvel.MvelExecutionContext;
import org.flowable.dmn.model.HitPolicy;
import org.flowable.engine.common.api.FlowableException;

import java.util.ArrayList;
import java.util.Collections;
import java.util.Comparator;
import java.util.List;
import java.util.Map;

/**
 * @author Yvo Swillens
 */
public class HitPolicyPriority extends AbstractHitPolicy implements ComposeDecisionResultBehavior {

    @Override
    public String getHitPolicyName() {
        return HitPolicy.PRIORITY.getValue();
    }

    public void composeDecisionResults(final MvelExecutionContext executionContext) {

        List<Map<String, Object>> ruleResults = new ArrayList<>(executionContext.getRuleResults().values());

        // sort on predefined list(s) of output values
        Collections.sort(ruleResults, new Comparator() {
            boolean noOutputValuesPresent = true;

            public int compare(Object o1, Object o2) {
                CompareToBuilder compareToBuilder = new CompareToBuilder();
                for (Map.Entry<String, List<Object>> entry : executionContext.getOutputValues().entrySet()) {
                    List<Object> outputValues = entry.getValue();
                    if (outputValues != null || !outputValues.isEmpty()) {
                        noOutputValuesPresent = false;
                        compareToBuilder.append(((Map) o1).get(entry.getKey()), ((Map) o2).get(entry.getKey()), new OutputOrderComparator<>(outputValues.toArray(new Comparable[outputValues.size()])));
                    }
                }

                if (!noOutputValuesPresent) {
                    return compareToBuilder.toComparison();
                } else {
                    if (Context.getDmnEngineConfiguration().isStrictMode()) {
                        throw new FlowableException(String.format("HitPolicy: %s; no output values present", getHitPolicyName()));
                    }
                    return 0;
                }
            }
        });

        executionContext.setDecisionResults(ruleResults.subList(0,1));
    }
}
