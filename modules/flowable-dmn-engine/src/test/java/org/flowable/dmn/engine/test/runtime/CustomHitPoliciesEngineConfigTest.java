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
package org.flowable.dmn.engine.test.runtime;

import static org.assertj.core.api.Assertions.assertThat;

import org.flowable.dmn.engine.DmnEngine;
import org.flowable.dmn.engine.DmnEngineConfiguration;
import org.flowable.dmn.engine.impl.hitpolicy.AbstractHitPolicy;
import org.flowable.dmn.engine.test.FlowableDmnRule;
import org.junit.Rule;
import org.junit.Test;

/**
 * @author Yvo Swillens
 */
public class CustomHitPoliciesEngineConfigTest {

    protected static final String ENGINE_CONFIG_1 = "custom3.flowable.dmn.cfg.xml";
    protected static final String ENGINE_CONFIG_2 = "custom4.flowable.dmn.cfg.xml";
    protected static final String ENGINE_CONFIG_3 = "custom5.flowable.dmn.cfg.xml";

    @Rule
    public FlowableDmnRule flowableDmnRule1 = new FlowableDmnRule(ENGINE_CONFIG_1);

    @Rule
    public FlowableDmnRule flowableDmnRule2 = new FlowableDmnRule(ENGINE_CONFIG_2);

    @Rule
    public FlowableDmnRule flowableDmnRule3 = new FlowableDmnRule(ENGINE_CONFIG_3);

    @Test
    public void overwriteHitPolicyBehaviors() {
        DmnEngine dmnEngine = flowableDmnRule1.getDmnEngine();
        DmnEngineConfiguration dmnEngineConfiguration = dmnEngine.getDmnEngineConfiguration();

        assertThat(dmnEngineConfiguration.getHitPolicyBehaviors()).hasSize(1);
    }

    @Test
    public void overwriteSpecificHitPolicyBehavior() {
        DmnEngine dmnEngine = flowableDmnRule2.getDmnEngine();
        DmnEngineConfiguration dmnEngineConfiguration = dmnEngine.getDmnEngineConfiguration();

        assertThat(dmnEngineConfiguration.getHitPolicyBehaviors()).hasSize(7);
        AbstractHitPolicy overwrittenHitPolicyBehavior = dmnEngineConfiguration.getHitPolicyBehaviors().get("FIRST");
        assertThat(overwrittenHitPolicyBehavior.getHitPolicyName()).isEqualTo("CUSTOM_HIT_POLICY");
    }

    @Test
    public void addHitPolicyBehavior() {
        DmnEngine dmnEngine = flowableDmnRule3.getDmnEngine();
        DmnEngineConfiguration dmnEngineConfiguration = dmnEngine.getDmnEngineConfiguration();

        assertThat(dmnEngineConfiguration.getHitPolicyBehaviors())
                .hasSize(8)
                .containsKey("CUSTOM_HIT_POLICY");
    }

}
