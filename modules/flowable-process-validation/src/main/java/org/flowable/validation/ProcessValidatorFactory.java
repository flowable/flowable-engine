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
package org.flowable.validation;

import org.flowable.validation.validator.ValidatorSet;
import org.flowable.validation.validator.ValidatorSetFactory;
import org.flowable.validation.validator.impl.ServiceTaskValidator;

/**
 * @author jbarrez
 */
public class ProcessValidatorFactory {

    protected ServiceTaskValidator customServiceTaskValidator;

    public ProcessValidator createDefaultProcessValidator() {
        ProcessValidatorImpl processValidator = new ProcessValidatorImpl();

        ValidatorSet executableProcessValidatorSet = new ValidatorSetFactory().createFlowableExecutableProcessValidatorSet();

        if (customServiceTaskValidator != null) {
            executableProcessValidatorSet.removeValidator(ServiceTaskValidator.class);
            executableProcessValidatorSet.addValidator(customServiceTaskValidator);
        }

        processValidator.addValidatorSet(executableProcessValidatorSet);

        return processValidator;
    }

    public ServiceTaskValidator getCustomServiceTaskValidator() {
        return customServiceTaskValidator;
    }

    public void setCustomServiceTaskValidator(ServiceTaskValidator customServiceTaskValidator) {
        this.customServiceTaskValidator = customServiceTaskValidator;
    }
}
