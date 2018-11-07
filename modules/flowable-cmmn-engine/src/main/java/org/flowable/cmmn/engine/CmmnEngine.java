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
package org.flowable.cmmn.engine;

import org.flowable.cmmn.api.CmmnHistoryService;
import org.flowable.cmmn.api.CmmnManagementService;
import org.flowable.cmmn.api.CmmnRepositoryService;
import org.flowable.cmmn.api.CmmnRuntimeService;
import org.flowable.cmmn.api.CmmnTaskService;
import org.flowable.common.engine.impl.FlowableVersions;

/**
 * Provides access to all services that expose CMMN and case management operations.
 * 
 * @author Joram Barrez
 */
public interface CmmnEngine {
    
    /** the version of the flowable CMMN library */
    public static String VERSION = FlowableVersions.CURRENT_VERSION;

    String getName();

    void close();
    
    CmmnRuntimeService getCmmnRuntimeService();
    
    CmmnTaskService getCmmnTaskService();
    
    CmmnManagementService getCmmnManagementService();
    
    CmmnRepositoryService getCmmnRepositoryService();
    
    CmmnHistoryService getCmmnHistoryService();
    
    CmmnEngineConfiguration getCmmnEngineConfiguration();
}
