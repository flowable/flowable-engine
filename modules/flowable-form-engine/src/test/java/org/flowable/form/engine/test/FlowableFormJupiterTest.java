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
package org.flowable.form.engine.test;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.tuple;

import org.flowable.form.api.FormDefinition;
import org.flowable.form.api.FormDeployment;
import org.flowable.form.engine.FormEngine;
import org.flowable.form.engine.FormEngines;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;

/**
 * @author Filip Hrisafov
 */
@ExtendWith(FlowableFormExtension.class)
public class FlowableFormJupiterTest {

    private FormEngine formEngine;

    @BeforeEach
    public void setUp(FormEngine formEngine, @FormDeploymentId String formDeploymentId) {
        this.formEngine = formEngine;

        assertThat(formEngine).isNotNull();
        FormDeployment formDeployment = formEngine.getFormRepositoryService().createDeploymentQuery().singleResult();
        assertThat(formDeployment.getName()).startsWith("FlowableFormJupiterTest.");
        assertThat(formDeployment.getId()).isEqualTo(formDeploymentId);
    }

    @Test
    @FormDeploymentAnnotation
    public void ruleUsageExample() {
        assertThat(formEngine.getFormRepositoryService().createFormDefinitionQuery().list())
            .extracting(FormDefinition::getKey, FormDefinition::getName)
            .containsExactlyInAnyOrder(
                tuple("ruleUsageExample", "Form for rule usage example")
            );

        FormDeployment formDeployment = formEngine.getFormRepositoryService().createDeploymentQuery().singleResult();
        assertThat(formDeployment.getName()).isEqualTo("FlowableFormJupiterTest.ruleUsageExample");

        assertThat(formEngine.getName()).as("form engine name").isEqualTo(FormEngines.NAME_DEFAULT);
    }

    @Test
    @FormDeploymentAnnotation(resources = {
        "org/flowable/form/engine/test/FlowableFormJupiterTest.ruleUsageExample.form",
        "org/flowable/form/engine/test/example.form"
    })
    public void ruleUsageExampleWithDefinedResources() {
        assertThat(formEngine.getFormRepositoryService().createFormDefinitionQuery().list())
            .extracting(FormDefinition::getKey, FormDefinition::getName)
            .containsExactlyInAnyOrder(
                tuple("ruleUsageExample", "Form for rule usage example"),
                tuple("simpleExample", "Form for example")
            );

        FormDeployment formDeployment = formEngine.getFormRepositoryService().createDeploymentQuery().singleResult();
        assertThat(formDeployment.getName()).isEqualTo("FlowableFormJupiterTest.ruleUsageExampleWithDefinedResources");
    }
}
