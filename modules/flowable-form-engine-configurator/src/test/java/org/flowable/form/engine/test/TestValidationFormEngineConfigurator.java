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

import java.util.Map;

import org.flowable.common.engine.api.FlowableException;
import org.flowable.form.api.FormInfo;
import org.flowable.form.engine.FormEngineConfiguration;
import org.flowable.form.engine.configurator.FormEngineConfigurator;
import org.flowable.form.engine.impl.FormServiceImpl;
import org.flowable.form.engine.impl.cfg.StandaloneFormEngineConfiguration;

/**
 * @author martin.grofcik
 */
public class TestValidationFormEngineConfigurator extends FormEngineConfigurator {

    public TestValidationFormEngineConfigurator() {
        this.formEngineConfiguration = new StandaloneFormEngineConfiguration();
        this.formEngineConfiguration.setFormService(new ThrowExceptionOnValidationFormService(formEngineConfiguration));
    }

    protected static class ThrowExceptionOnValidationFormService extends FormServiceImpl {

        public ThrowExceptionOnValidationFormService(FormEngineConfiguration engineConfiguration) {
            super(engineConfiguration);
        }

        @Override
        public void validateFormFields(FormInfo formInfo, Map<String, Object> values) {
            commandExecutor.execute(commandContext -> { throw new FlowableException("validation failed");});
        }
    }
}
