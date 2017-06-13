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
package org.flowable.bpm.model.bpmn.builder;

import org.flowable.bpm.model.bpmn.instance.StartEvent;

public class EmbeddedSubProcessBuilder
        extends AbstractEmbeddedSubProcessBuilder<EmbeddedSubProcessBuilder, AbstractSubProcessBuilder<?>> {

    @SuppressWarnings("rawtypes")
    protected EmbeddedSubProcessBuilder(AbstractSubProcessBuilder subProcessBuilder) {
        super(subProcessBuilder, EmbeddedSubProcessBuilder.class);
    }

    public StartEventBuilder startEvent() {
        return startEvent(null);
    }

    public StartEventBuilder startEvent(String id) {
        StartEvent start = subProcessBuilder.createChild(StartEvent.class, id);
        return start.builder();
    }
}
