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
package org.flowable.idm.engine;

import org.flowable.common.engine.impl.FlowableVersions;
import org.flowable.idm.api.IdmIdentityService;
import org.flowable.idm.api.IdmManagementService;

public interface IdmEngine {

    /**
     * the version of the flowable idm library
     */
    public static String VERSION = FlowableVersions.CURRENT_VERSION;

    /**
     * The name as specified in 'idm-engine-name' in the flowable.idm.cfg.xml configuration file. The default name for a idm engine is 'default
     */
    String getName();

    void close();

    IdmIdentityService getIdmIdentityService();

    IdmManagementService getIdmManagementService();

    IdmEngineConfiguration getIdmEngineConfiguration();
}
