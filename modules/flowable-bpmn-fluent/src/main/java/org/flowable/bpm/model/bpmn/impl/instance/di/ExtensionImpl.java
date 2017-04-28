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
package org.flowable.bpm.model.bpmn.impl.instance.di;

import static org.flowable.bpm.model.bpmn.impl.BpmnModelConstants.DI_ELEMENT_EXTENSION;
import static org.flowable.bpm.model.bpmn.impl.BpmnModelConstants.DI_NS;
import static org.flowable.bpm.model.xml.type.ModelElementTypeBuilder.ModelTypeInstanceProvider;

import org.flowable.bpm.model.bpmn.impl.instance.BpmnModelElementInstanceImpl;
import org.flowable.bpm.model.bpmn.instance.di.Extension;
import org.flowable.bpm.model.xml.ModelBuilder;
import org.flowable.bpm.model.xml.impl.instance.ModelTypeInstanceContext;
import org.flowable.bpm.model.xml.type.ModelElementTypeBuilder;

/**
 * The DI extension element of the DI DiagramElement type.
 */
public class ExtensionImpl
        extends BpmnModelElementInstanceImpl
        implements Extension {

    public static void registerType(ModelBuilder modelBuilder) {
        ModelElementTypeBuilder typeBuilder = modelBuilder.defineType(Extension.class, DI_ELEMENT_EXTENSION)
                .namespaceUri(DI_NS)
                .instanceProvider(new ModelTypeInstanceProvider<Extension>() {
                    public Extension newInstance(ModelTypeInstanceContext instanceContext) {
                        return new ExtensionImpl(instanceContext);
                    }
                });

        typeBuilder.build();
    }

    public ExtensionImpl(ModelTypeInstanceContext instanceContext) {
        super(instanceContext);
    }
}
