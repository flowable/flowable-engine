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
package org.flowable.bpm.model.bpmn.instance.di;

import org.flowable.bpm.model.bpmn.instance.BpmnModelElementInstance;

/**
 * The DI Diagram element.
 */
public interface Diagram
        extends BpmnModelElementInstance {

    String getName();

    void setName(String name);

    String getDocumentation();

    void setDocumentation(String documentation);

    double getResolution();

    void setResolution(double resolution);

    String getId();

    void setId(String id);

}
