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
package flowable;

import static org.assertj.core.api.Assertions.assertThat;

import org.flowable.cmmn.engine.CmmnEngineConfiguration;
import org.flowable.cmmn.spring.impl.test.FlowableCmmnSpringExtension;
import org.flowable.engine.impl.cfg.ProcessEngineConfigurationImpl;
import org.flowable.spring.impl.test.FlowableSpringExtension;
import org.flowable.variable.api.types.VariableTypes;
import org.flowable.variable.service.impl.types.DefaultVariableTypes;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.context.annotation.Import;

/**
 * @author Joram Barrez
 */
@Import(CustomVariableTypesTestConfiguration.class)
@SpringBootTest
@ExtendWith(FlowableCmmnSpringExtension.class)
@ExtendWith(FlowableSpringExtension.class)
public class PreAndPostVariableTypesTest {

    @Autowired
    private ProcessEngineConfigurationImpl processEngineConfiguration;

    @Autowired
    private CmmnEngineConfiguration cmmnEngineConfiguration;

    @Test
    public void testVariableTypesMerged() {
        assertVariableTypeIndexes(processEngineConfiguration.getVariableTypes());
        assertVariableTypeIndexes(cmmnEngineConfiguration.getVariableTypes());
    }

    protected void assertVariableTypeIndexes(VariableTypes variableTypes) {
        int nrOfVariableTypes = ((DefaultVariableTypes) variableTypes).size();

        assertThat(variableTypes.getTypeIndex("pre-bpmn01")).isEqualTo(1); // cmmn goes first
        assertThat(variableTypes.getTypeIndex("pre-bpmn02")).isEqualTo(2);
        assertThat(variableTypes.getTypeIndex("post-bpmn01")).isEqualTo(nrOfVariableTypes - 5);
        assertThat(variableTypes.getTypeIndex("post-bpmn02")).isEqualTo(nrOfVariableTypes - 4);
        assertThat(variableTypes.getTypeIndex("post-bpmn03")).isEqualTo(nrOfVariableTypes - 3);

        assertThat(variableTypes.getTypeIndex("pre-cmmn01")).isEqualTo(0);
        assertThat(variableTypes.getTypeIndex("post-cmmn01")).isEqualTo(nrOfVariableTypes - 2);
        assertThat(variableTypes.getTypeIndex("post-cmmn02")).isEqualTo(nrOfVariableTypes - 1);
    }

}
