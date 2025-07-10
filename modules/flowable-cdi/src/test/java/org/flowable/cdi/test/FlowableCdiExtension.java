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
package org.flowable.cdi.test;

import org.flowable.cdi.impl.util.ProgrammaticBeanLookup;
import org.flowable.engine.ProcessEngine;
import org.flowable.engine.test.FlowableExtension;
import org.junit.jupiter.api.extension.ExtensionContext;

/**
 * @author Filip Hrisafov
 */
public class FlowableCdiExtension extends FlowableExtension {

    private static final ExtensionContext.Namespace NAMESPACE = ExtensionContext.Namespace.create(FlowableCdiExtension.class);

    @Override
    protected ProcessEngine createProcessEngine(ExtensionContext context) {
        return ProgrammaticBeanLookup.lookup(ProcessEngine.class);
    }

    @Override
    protected ExtensionContext.Store getStore(ExtensionContext context) {
        return context.getRoot().getStore(NAMESPACE);
    }

}
