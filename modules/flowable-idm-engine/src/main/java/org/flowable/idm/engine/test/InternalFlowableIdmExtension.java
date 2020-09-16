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
package org.flowable.idm.engine.test;

import org.flowable.common.engine.impl.db.SchemaManager;
import org.flowable.common.engine.impl.interceptor.Command;
import org.flowable.common.engine.impl.interceptor.CommandContext;
import org.flowable.common.engine.impl.test.EnsureCleanDb;
import org.flowable.common.engine.impl.test.EnsureCleanDbUtils;
import org.flowable.idm.engine.IdmEngine;
import org.flowable.idm.engine.IdmEngineConfiguration;
import org.flowable.idm.engine.impl.util.CommandContextUtil;
import org.junit.jupiter.api.TestInstance;
import org.junit.jupiter.api.extension.AfterAllCallback;
import org.junit.jupiter.api.extension.AfterEachCallback;
import org.junit.jupiter.api.extension.BeforeEachCallback;
import org.junit.jupiter.api.extension.ExtensionContext;
import org.junit.jupiter.api.extension.ParameterContext;
import org.junit.jupiter.api.extension.ParameterResolver;
import org.junit.platform.commons.support.AnnotationSupport;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

/**
 * Base internal extension for JUnit Jupiter. This is a basis for other internal extensions. It allows:
 * <ul>
 * <li>
 * Assert and ensure a clean db after each test or after all tests (depending on the {@link TestInstance.Lifecycle}.
 * </li>
 * <li>
 * Support for injecting the {@link IdmEngine}.
 * </li>
 * </ul>
 *
 * @author Filip Hrisafov
 */
public abstract class InternalFlowableIdmExtension implements AfterEachCallback, BeforeEachCallback, AfterAllCallback, ParameterResolver {

    protected static final String EMPTY_LINE = "\n";

    protected final Logger logger = LoggerFactory.getLogger(getClass());

    @Override
    public void beforeEach(ExtensionContext context) {
    }

    @Override
    public void afterEach(ExtensionContext context) throws Exception {
        doFinally(context, TestInstance.Lifecycle.PER_METHOD);
    }

    @Override
    public void afterAll(ExtensionContext context) throws Exception {
        doFinally(context, TestInstance.Lifecycle.PER_CLASS);
    }

    protected void doFinally(ExtensionContext context, TestInstance.Lifecycle lifecycleForClean) {
        IdmEngine idmEngine = getIdmEngine(context);
        IdmEngineConfiguration idmEngineConfiguration = idmEngine.getIdmEngineConfiguration();
        try {
            if (context.getTestInstanceLifecycle().orElse(TestInstance.Lifecycle.PER_METHOD) == lifecycleForClean) {
                cleanTestAndAssertAndEnsureCleanDb(context, idmEngine);
            }
        } finally {
            idmEngineConfiguration.getClock().reset();
        }
    }

    protected void cleanTestAndAssertAndEnsureCleanDb(ExtensionContext context, IdmEngine idmEngine) {
        AnnotationSupport.findAnnotation(context.getRequiredTestClass(), EnsureCleanDb.class)
            .ifPresent(ensureCleanDb -> assertAndEnsureCleanDb(idmEngine, context, ensureCleanDb));
    }

    /**
     * Each test is assumed to clean up all DB content it entered. After a test method executed, this method scans all tables to see if the DB is completely clean. It throws AssertionFailed in case
     * the DB is not clean. If the DB is not clean, it is cleaned by performing a create a drop.
     */
    protected void assertAndEnsureCleanDb(IdmEngine idmEngine, ExtensionContext context, EnsureCleanDb ensureCleanDb) {
        EnsureCleanDbUtils.assertAndEnsureCleanDb(
                context.getDisplayName(),
                logger,
                idmEngine.getIdmEngineConfiguration(),
                ensureCleanDb,
                !context.getExecutionException().isPresent(),
                new Command<Void>() {

                    @Override
                    public Void execute(CommandContext commandContext) {
                        SchemaManager schemaManager = CommandContextUtil.getIdmEngineConfiguration(commandContext).getSchemaManager();
                        schemaManager.schemaDrop();
                        schemaManager.schemaCreate();
                        return null;
                    }
                }
        );
    }

    @Override
    public boolean supportsParameter(ParameterContext parameterContext, ExtensionContext context) {
        Class<?> parameterType = parameterContext.getParameter().getType();
        return IdmEngine.class.equals(parameterType);
    }

    @Override
    public Object resolveParameter(ParameterContext parameterContext, ExtensionContext context) {
        return getIdmEngine(context);
    }

    protected abstract IdmEngine getIdmEngine(ExtensionContext context);

    protected abstract ExtensionContext.Store getStore(ExtensionContext context);
}
