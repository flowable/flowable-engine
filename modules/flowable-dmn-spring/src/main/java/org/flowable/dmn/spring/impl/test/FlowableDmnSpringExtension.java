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
package org.flowable.dmn.spring.impl.test;

import org.flowable.dmn.engine.DmnEngine;
import org.flowable.dmn.engine.test.FlowableDmnExtension;
import org.junit.jupiter.api.extension.ExtensionContext;
import org.springframework.test.context.junit.jupiter.SpringExtension;

/**
 * An extension that uses {@link SpringExtension} to get the {@link DmnEngine} from the {@link org.springframework.context.ApplicationContext}
 * and make it available for the {@link FlowableDmnExtension}.
 * <p>
 * <b>NB:</b> The {@link org.flowable.dmn.engine.test.DmnConfigurationResource} is ignored
 * as the {@link DmnEngine} is taken from the Spring application context
 *
 * @author Filip Hrisafov
 */
public class FlowableDmnSpringExtension extends FlowableDmnExtension {

    private static final ExtensionContext.Namespace NAMESPACE = ExtensionContext.Namespace.create(FlowableDmnSpringExtension.class);

    @Override
    protected DmnEngine createDmnEngine(ExtensionContext context) {
        return SpringExtension.getApplicationContext(context).getBean(DmnEngine.class);
    }

    @Override
    protected ExtensionContext.Store getStore(ExtensionContext context) {
        return context.getRoot().getStore(NAMESPACE);
    }
}
