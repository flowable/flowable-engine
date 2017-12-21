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

import java.util.HashMap;
import java.util.Map;

import org.flowable.idm.engine.IdmEngine;
import org.flowable.idm.engine.IdmEngineConfiguration;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

/**
 * @author Tijs Rademakers
 * @author Joram Barrez
 */
public abstract class IdmTestHelper {

    private static final Logger LOGGER = LoggerFactory.getLogger(IdmTestHelper.class);

    public static final String EMPTY_LINE = "\n";

    static Map<String, IdmEngine> idmEngines = new HashMap<>();

    public static IdmEngine getIdmEngine(String configurationResource) {
        IdmEngine idmEngine = idmEngines.get(configurationResource);
        if (idmEngine == null) {
            LOGGER.debug("==== BUILDING IDM ENGINE ========================================================================");
            idmEngine = IdmEngineConfiguration.createIdmEngineConfigurationFromResource(configurationResource)
                    .setDatabaseSchemaUpdate(IdmEngineConfiguration.DB_SCHEMA_UPDATE_DROP_CREATE)
                    .buildIdmEngine();
            LOGGER.debug("==== IDM ENGINE CREATED =========================================================================");
            idmEngines.put(configurationResource, idmEngine);
        }
        return idmEngine;
    }

    public static void closeIdmEngines() {
        for (IdmEngine idmEngine : idmEngines.values()) {
            idmEngine.close();
        }
        idmEngines.clear();
    }

}
