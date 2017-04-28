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
package org.flowable.bpm.model.bpmn.util;

import org.flowable.bpm.model.bpmn.BpmnModelBuilder;
import org.flowable.bpm.model.xml.Model;
import org.flowable.bpm.model.xml.ModelInstance;
import org.flowable.bpm.model.xml.instance.ModelElementInstance;
import org.flowable.bpm.model.xml.test.GetModelElementTypeRule;
import org.flowable.bpm.model.xml.type.ModelElementType;
import org.junit.rules.TestWatcher;
import org.junit.runner.Description;

public class GetBpmnModelElementTypeRule
        extends TestWatcher
        implements GetModelElementTypeRule {

    private ModelInstance modelInstance;
    private Model model;
    private ModelElementType modelElementType;

    @Override
    @SuppressWarnings("unchecked")
    protected void starting(Description description) {
        String className = description.getClassName();
        className = className.replaceAll("Test", "");
        Class<? extends ModelElementInstance> instanceClass;
        try {
            instanceClass = (Class<? extends ModelElementInstance>) Class.forName(className);
        }
        catch (ClassNotFoundException e) {
            throw new RuntimeException(e);
        }
        modelInstance = BpmnModelBuilder.createEmptyModel();
        model = modelInstance.getModel();
        modelElementType = model.getType(instanceClass);
    }

    @Override
    public ModelInstance getModelInstance() {
        return modelInstance;
    }

    @Override
    public Model getModel() {
        return model;
    }

    @Override
    public ModelElementType getModelElementType() {
        return modelElementType;
    }
}
