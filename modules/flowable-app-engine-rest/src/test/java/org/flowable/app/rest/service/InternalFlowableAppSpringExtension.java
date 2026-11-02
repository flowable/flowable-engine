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
package org.flowable.app.rest.service;

import org.flowable.app.engine.AppEngine;
import org.flowable.app.engine.test.FlowableAppExtension;
import org.flowable.app.engine.test.FlowableAppTestHelper;
import org.flowable.common.engine.impl.test.EnsureCleanDb;
import org.flowable.common.engine.impl.test.EnsureCleanDbUtils;
import org.junit.jupiter.api.extension.ExtensionContext;
import org.junit.platform.commons.support.AnnotationSupport;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.test.context.junit.jupiter.SpringExtension;

/**
 * @author Filip Hrisafov
 */
public class InternalFlowableAppSpringExtension extends FlowableAppExtension {

    private static final ExtensionContext.Namespace NAMESPACE = ExtensionContext.Namespace.create(InternalFlowableAppSpringExtension.class);
    private static final Logger LOGGER = LoggerFactory.getLogger(InternalFlowableAppSpringExtension.class);

    @Override
    public void afterEach(ExtensionContext context) {
        super.afterEach(context);
        FlowableAppTestHelper testHelper = getTestHelper(context);
        AnnotationSupport.findAnnotation(context.getRequiredTestClass(), EnsureCleanDb.class)
                .ifPresent(ensureCleanDb -> {
                    EnsureCleanDbUtils.assertAndEnsureCleanDb(
                            context.getDisplayName(),
                            LOGGER,
                            testHelper.getAppEngine().getAppEngineConfiguration(),
                            ensureCleanDb,
                            context.getExecutionException().isEmpty(),
                            testHelper.getAppEngine().getAppEngineConfiguration().getSchemaManagementCmd()
                    );
                });
    }

    @Override
    protected AppEngine createAppEngine(ExtensionContext context) {
        return SpringExtension.getApplicationContext(context).getBean(AppEngine.class);
    }

    @Override
    protected ExtensionContext.Store getStore(ExtensionContext context) {
        return context.getRoot().getStore(NAMESPACE);
    }
}
