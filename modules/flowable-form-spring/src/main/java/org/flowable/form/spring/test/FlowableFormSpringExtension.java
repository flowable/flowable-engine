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
package org.flowable.form.spring.test;

import org.flowable.form.engine.FormEngine;
import org.flowable.form.engine.test.FlowableFormExtension;
import org.junit.jupiter.api.extension.ExtensionContext;
import org.springframework.test.context.junit.jupiter.SpringExtension;

/**
 * An extension that uses {@link SpringExtension} to get the {@link FormEngine} from the {@link org.springframework.context.ApplicationContext}
 * and make it available for the {@link FlowableFormExtension}.
 *
 * <b>NB:</b> The {@link org.flowable.form.engine.test.FormConfigurationResource} is ignored
 * as the {@link FormEngine} is taken from the Spring application context
 *
 * @author Filip Hrisafov
 */
public class FlowableFormSpringExtension extends FlowableFormExtension {

    private static final ExtensionContext.Namespace NAMESPACE = ExtensionContext.Namespace.create(FlowableFormSpringExtension.class);

    @Override
    protected FormEngine createFormEngine(ExtensionContext context) {
        return SpringExtension.getApplicationContext(context).getBean(FormEngine.class);
    }

    @Override
    protected ExtensionContext.Store getStore(ExtensionContext context) {
        return context.getRoot().getStore(NAMESPACE);
    }
}
