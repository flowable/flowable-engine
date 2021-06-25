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

import java.util.Arrays;

import org.flowable.cmmn.spring.SpringCmmnEngineConfiguration;
import org.flowable.spring.SpringProcessEngineConfiguration;
import org.flowable.spring.boot.EngineConfigurationConfigurer;
import org.flowable.variable.api.types.ValueFields;
import org.flowable.variable.api.types.VariableType;
import org.flowable.variable.service.impl.types.IntegerType;
import org.springframework.boot.test.context.TestConfiguration;
import org.springframework.context.annotation.Bean;

@TestConfiguration
public class CustomVariableTypesTestConfiguration {

    @Bean
    public EngineConfigurationConfigurer<SpringProcessEngineConfiguration> custombpmnEngineConfigurer() {
        return engineConfiguration -> {
            engineConfiguration.setCustomPreVariableTypes(Arrays.asList(new TestVariableType("pre-bpmn01"),
                new TestVariableType("pre-bpmn02"),
                new TestVariableType(IntegerType.TYPE_NAME)));
            engineConfiguration.setCustomPostVariableTypes(Arrays.asList(new TestVariableType("post-bpmn01"),
                new TestVariableType("post-bpmn02"),
                new TestVariableType("post-bpmn03")));
        };
    }

    @Bean
    public EngineConfigurationConfigurer<SpringCmmnEngineConfiguration> customCmmnEngineConfigurer() {
        return engineConfiguration -> {
            engineConfiguration.setCustomPreVariableTypes(Arrays.asList(new TestVariableType("pre-cmmn01")));
            engineConfiguration.setCustomPostVariableTypes(Arrays.asList(new TestVariableType("post-cmmn01"),
                new TestVariableType("post-cmmn02")));
        };
    }

    public static class TestVariableType implements VariableType {

        private String type;

        public TestVariableType(String type) {
            this.type = type;
        }
        @Override
        public String getTypeName() {
            return type;
        }
        @Override
        public boolean isCachable() {
            return false;
        }
        @Override
        public boolean isAbleToStore(Object value) {
            return false;
        }
        @Override
        public void setValue(Object value, ValueFields valueFields) {

        }
        @Override
        public Object getValue(ValueFields valueFields) {
            return null;
        }
    }

}
