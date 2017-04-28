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
package org.flowable.bpm.model.xml.testmodel.instance;

import static org.flowable.bpm.model.xml.testmodel.TestModelConstants.ELEMENT_NAME_GUARD_EGG;
import static org.flowable.bpm.model.xml.testmodel.TestModelConstants.MODEL_NAMESPACE;

import org.flowable.bpm.model.xml.ModelBuilder;
import org.flowable.bpm.model.xml.impl.instance.ModelElementInstanceImpl;
import org.flowable.bpm.model.xml.impl.instance.ModelTypeInstanceContext;
import org.flowable.bpm.model.xml.type.ModelElementTypeBuilder;
import org.flowable.bpm.model.xml.type.ModelElementTypeBuilder.ModelTypeInstanceProvider;

public class GuardEgg
        extends ModelElementInstanceImpl {

    public GuardEgg(ModelTypeInstanceContext instanceContext) {
        super(instanceContext);
    }

    public static void registerType(ModelBuilder modelBuilder) {
        ModelElementTypeBuilder typeBuilder = modelBuilder.defineType(GuardEgg.class, ELEMENT_NAME_GUARD_EGG)
                .namespaceUri(MODEL_NAMESPACE)
                .instanceProvider(new ModelTypeInstanceProvider<GuardEgg>() {
                    @Override
                    public GuardEgg newInstance(ModelTypeInstanceContext instanceContext) {
                        return new GuardEgg(instanceContext);
                    }
                });

        typeBuilder.build();
    }

}
