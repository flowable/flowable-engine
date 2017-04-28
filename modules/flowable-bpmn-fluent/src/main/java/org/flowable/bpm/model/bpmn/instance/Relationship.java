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
package org.flowable.bpm.model.bpmn.instance;

import org.flowable.bpm.model.bpmn.RelationshipDirection;
import org.flowable.bpm.model.bpmn.impl.instance.Source;
import org.flowable.bpm.model.bpmn.impl.instance.Target;

import java.util.Collection;

/**
 * The BPMN relationship element.
 */
public interface Relationship
        extends BaseElement {

    String getType();

    void setType(String type);

    RelationshipDirection getDirection();

    void setDirection(RelationshipDirection direction);

    Collection<Source> getSources();

    Collection<Target> getTargets();
}
