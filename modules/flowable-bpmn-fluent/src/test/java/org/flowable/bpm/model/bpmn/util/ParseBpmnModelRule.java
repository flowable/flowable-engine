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
import org.flowable.bpm.model.bpmn.BpmnModelInstance;
import org.flowable.bpm.model.xml.impl.util.IoUtil;
import org.junit.rules.TestWatcher;
import org.junit.runner.Description;

import java.io.InputStream;

public class ParseBpmnModelRule
        extends TestWatcher {

    protected BpmnModelInstance bpmnModelInstance;

    @Override
    protected void starting(Description description) {

        if (description.getAnnotation(BpmnModelResource.class) != null) {

            Class<?> testClass = description.getTestClass();
            String methodName = description.getMethodName();

            String resourceFolderName = testClass.getName().replaceAll("\\.", "/");
            String bpmnResourceName = resourceFolderName + '.' + methodName + ".bpmn";

            InputStream resourceAsStream = getClass().getClassLoader().getResourceAsStream(bpmnResourceName);
            try {
                bpmnModelInstance = BpmnModelBuilder.readModelFromStream(resourceAsStream);
            }
            finally {
                IoUtil.closeSilently(resourceAsStream);
            }

        }

    }

    public BpmnModelInstance getBpmnModel() {
        return bpmnModelInstance;
    }

}
