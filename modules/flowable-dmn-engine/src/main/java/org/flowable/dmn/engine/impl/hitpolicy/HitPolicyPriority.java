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

import java.util.ArrayList;
import java.util.Collections;
import java.util.Comparator;
import java.util.List;
import java.util.Map;

import org.apache.commons.lang3.builder.CompareToBuilder;
import org.flowable.common.engine.api.FlowableException;
import org.flowable.dmn.engine.impl.el.ELExecutionContext;
import org.flowable.dmn.engine.impl.util.CommandContextUtil;
import org.flowable.dmn.model.HitPolicy;

/**
 * @author Yvo Swillens
 */
public class HitPolicyPriority extends AbstractHitPolicy implements ComposeDecisionResultBehavior {

    @Override
    public String getHitPolicyName() {
        return HitPolicy.PRIORITY.getValue();
    }

    @Override
    public void composeDecisionResults(final ELExecutionContext executionContext) {

        List<Map<String, Object>> ruleResults = new ArrayList<>(executionContext.getRuleResults().values());

        // sort on predefined list(s) of output values
        Collections.sort(ruleResults, new Comparator<Object>() {
            boolean noOutputValuesPresent = true;

            @SuppressWarnings("unchecked")
            @Override
            public int compare(Object o1, Object o2) {
                CompareToBuilder compareToBuilder = new CompareToBuilder();
                for (Map.Entry<String, List<Object>> entry : executionContext.getOutputValues().entrySet()) {
                    List<Object> outputValues = entry.getValue();
                    if (outputValues != null || !outputValues.isEmpty()) {
                        noOutputValuesPresent = false;
                        compareToBuilder.append(((Map<String, Object>) o1).get(entry.getKey()), 
                                        ((Map<String, Object>) o2).get(entry.getKey()), 
                                        new OutputOrderComparator<>(outputValues.toArray(new Comparable[outputValues.size()])));
                    }
                }

                if (!noOutputValuesPresent) {
                    return compareToBuilder.toComparison();
                } else {
                    if (CommandContextUtil.getDmnEngineConfiguration().isStrictMode()) {
                        throw new FlowableException(String.format("HitPolicy %s violated; no output values present.", getHitPolicyName()));
                    } else {
                        executionContext.getAuditContainer().setValidationMessage(String.format("HitPolicy %s violated; no output values present. Setting first valid result as final result.", getHitPolicyName()));
                    }
                    
                    return 0;
                }
            }
        });

        executionContext.getAuditContainer().addDecisionResultObject(ruleResults.get(0));
    }
}
