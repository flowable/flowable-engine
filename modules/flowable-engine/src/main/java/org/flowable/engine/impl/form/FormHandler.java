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

package org.flowable.engine.impl.form;

import java.io.Serializable;
import java.util.List;
import java.util.Map;

import org.flowable.bpmn.model.FormProperty;
import org.flowable.engine.impl.persistence.entity.DeploymentEntity;
import org.flowable.engine.impl.persistence.entity.ExecutionEntity;
import org.flowable.engine.repository.ProcessDefinition;

/**
 * @author Tom Baeyens
 */
public interface FormHandler extends Serializable {

    ThreadLocal<FormHandler> current = new ThreadLocal<>();

    void parseConfiguration(List<FormProperty> formProperties, String formKey, DeploymentEntity deployment, ProcessDefinition processDefinition);

    void submitFormProperties(Map<String, String> properties, ExecutionEntity execution);
}
