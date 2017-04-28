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
package org.flowable.bpm.model.bpmn.impl.instance.flowable;

import static org.flowable.bpm.model.bpmn.impl.BpmnModelConstants.FLOWABLE_ELEMENT_FAILED_JOB_RETRY_TIME_CYCLE;
import static org.flowable.bpm.model.bpmn.impl.BpmnModelConstants.FLOWABLE_NS;
import static org.flowable.bpm.model.xml.type.ModelElementTypeBuilder.ModelTypeInstanceProvider;

import org.flowable.bpm.model.bpmn.impl.instance.BpmnModelElementInstanceImpl;
import org.flowable.bpm.model.bpmn.instance.flowable.FlowableFailedJobRetryTimeCycle;
import org.flowable.bpm.model.xml.ModelBuilder;
import org.flowable.bpm.model.xml.impl.instance.ModelTypeInstanceContext;
import org.flowable.bpm.model.xml.type.ModelElementTypeBuilder;

/**
 * The BPMN failedJobRetryTimeCycle Flowable extension element.
 */
public class FlowableFailedJobRetryTimeCycleImpl
        extends BpmnModelElementInstanceImpl
        implements FlowableFailedJobRetryTimeCycle {

    public static void registerType(ModelBuilder modelBuilder) {
        ModelElementTypeBuilder typeBuilder = modelBuilder.defineType(FlowableFailedJobRetryTimeCycle.class, FLOWABLE_ELEMENT_FAILED_JOB_RETRY_TIME_CYCLE)
                .namespaceUri(FLOWABLE_NS)
                .instanceProvider(new ModelTypeInstanceProvider<FlowableFailedJobRetryTimeCycle>() {
                    @Override
                    public FlowableFailedJobRetryTimeCycle newInstance(ModelTypeInstanceContext instanceContext) {
                        return new FlowableFailedJobRetryTimeCycleImpl(instanceContext);
                    }
                });

        typeBuilder.build();
    }

    public FlowableFailedJobRetryTimeCycleImpl(ModelTypeInstanceContext instanceContext) {
        super(instanceContext);
    }
}
