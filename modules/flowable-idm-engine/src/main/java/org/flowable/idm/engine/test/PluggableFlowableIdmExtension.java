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

import org.flowable.common.engine.api.FlowableException;
import org.flowable.idm.engine.IdmEngine;
import org.flowable.idm.engine.IdmEngines;
import org.junit.jupiter.api.extension.ExtensionContext;

/**
 * An extension which uses the {@link IdmEngines#getDefaultIdmEngine()} and is cached within the entire context
 * (i.e. it would be reused by all users of the extension).
 * <p>
 *
 * @author Filip Hrisafov
 */
public class PluggableFlowableIdmExtension extends InternalFlowableIdmExtension {

    private static final ExtensionContext.Namespace NAMESPACE = ExtensionContext.Namespace.create(PluggableFlowableIdmExtension.class);
    private static final String IDM_ENGINE = "cachedIdmEngine";

    @Override
    protected IdmEngine getIdmEngine(ExtensionContext context) {
        return getStore(context).getOrComputeIfAbsent(IDM_ENGINE, key -> initializeIdmEngine(), IdmEngine.class);
    }

    protected IdmEngine initializeIdmEngine() {
        logger.info("No cached idm engine found for test. Retrieving the default engine.");
        IdmEngines.destroy(); // Just to be sure we're not getting any previously cached version

        IdmEngine idmEngine = IdmEngines.getDefaultIdmEngine();
        if (idmEngine == null) {
            throw new FlowableException("no default idm engine available");
        }
        return idmEngine;
    }

    @Override
    protected ExtensionContext.Store getStore(ExtensionContext context) {
        return context.getRoot().getStore(NAMESPACE);
    }
}
