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
package org.flowable.cmmn.engine.impl;

import org.flowable.cmmn.api.CmmnHistoryService;
import org.flowable.cmmn.api.CmmnManagementService;
import org.flowable.cmmn.api.CmmnMigrationService;
import org.flowable.cmmn.api.CmmnRepositoryService;
import org.flowable.cmmn.api.CmmnRuntimeService;
import org.flowable.cmmn.api.CmmnTaskService;
import org.flowable.cmmn.api.DynamicCmmnService;
import org.flowable.cmmn.engine.CmmnEngine;
import org.flowable.cmmn.engine.CmmnEngineConfiguration;
import org.flowable.cmmn.engine.CmmnEngines;
import org.flowable.cmmn.engine.impl.cmd.ClearCaseInstanceLockTimesCmd;
import org.flowable.common.engine.api.FlowableOptimisticLockingException;
import org.flowable.common.engine.api.engine.EngineLifecycleListener;
import org.flowable.common.engine.impl.interceptor.CommandExecutor;
import org.flowable.job.service.impl.asyncexecutor.AsyncExecutor;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

/**
 * @author Joram Barrez
 */
public class CmmnEngineImpl implements CmmnEngine {
    
    private static final Logger LOGGER = LoggerFactory.getLogger(CmmnEngineImpl.class);

    protected String name;
    protected CmmnEngineConfiguration cmmnEngineConfiguration;
    protected CmmnRuntimeService cmmnRuntimeService;
    protected DynamicCmmnService dynamicCmmnService;
    protected CmmnTaskService cmmnTaskService;
    protected CmmnManagementService cmmnManagementService;
    protected CmmnRepositoryService cmmnRepositoryService;
    protected CmmnHistoryService cmmnHistoryService;
    protected CmmnMigrationService cmmnMigrationService;
    
    protected AsyncExecutor asyncExecutor;
    protected AsyncExecutor asyncHistoryExecutor;
    
    public CmmnEngineImpl(CmmnEngineConfiguration cmmnEngineConfiguration) {
        this.cmmnEngineConfiguration = cmmnEngineConfiguration;
        this.name = cmmnEngineConfiguration.getEngineName();
        this.cmmnRuntimeService = cmmnEngineConfiguration.getCmmnRuntimeService();
        this.dynamicCmmnService = cmmnEngineConfiguration.getDynamicCmmnService();
        this.cmmnTaskService = cmmnEngineConfiguration.getCmmnTaskService();
        this.cmmnManagementService = cmmnEngineConfiguration.getCmmnManagementService();
        this.cmmnRepositoryService = cmmnEngineConfiguration.getCmmnRepositoryService();
        this.cmmnHistoryService = cmmnEngineConfiguration.getCmmnHistoryService();
        this.cmmnMigrationService = cmmnEngineConfiguration.getCmmnMigrationService();
        
        this.asyncExecutor = cmmnEngineConfiguration.getAsyncExecutor();
        this.asyncHistoryExecutor = cmmnEngineConfiguration.getAsyncHistoryExecutor();
        
        if (cmmnEngineConfiguration.getSchemaManagementCmd() != null) {
            CommandExecutor commandExecutor = cmmnEngineConfiguration.getCommandExecutor();
            commandExecutor.execute(cmmnEngineConfiguration.getSchemaCommandConfig(), cmmnEngineConfiguration.getSchemaManagementCmd());
        }

        LOGGER.info("CmmnEngine {} created", name);
        
        CmmnEngines.registerCmmnEngine(this);

        if (cmmnEngineConfiguration.getEngineLifecycleListeners() != null) {
            for (EngineLifecycleListener engineLifecycleListener : cmmnEngineConfiguration.getEngineLifecycleListeners()) {
                engineLifecycleListener.onEngineBuilt(this);
            }
        }
    }
    
    @Override
    public void startExecutors() {
        if (asyncExecutor != null && asyncExecutor.isAutoActivate()) {
            asyncExecutor.start();
        }

        // When running together with the bpmn engine, the asyncHistoryExecutor is shared by default.
        // However, calling multiple times .start() won't do anything (the method returns if already running),
        // so no need to check this case specifically here.
        if (asyncHistoryExecutor != null && asyncHistoryExecutor.isAutoActivate()) {
            asyncHistoryExecutor.start();
        }
        
        if (cmmnEngineConfiguration.isEnableHistoryCleaning()) {
            try {
                cmmnManagementService.handleHistoryCleanupTimerJob();
            } catch (FlowableOptimisticLockingException ex) {
                LOGGER.warn(
                        "Optimistic locking exception when creating timer history clean jobs. Cleanup timer job was created / updated by another instance.");
            }
        }
    }
    
    @Override
    public String getName() {
        return name;
    }
    
    public void setName(String name) {
        this.name = name;
    }
    
    @Override
    public void close() {
        CmmnEngines.unregister(this);
        
        if (asyncExecutor != null && asyncExecutor.isActive()) {
            asyncExecutor.shutdown();

            // Async executor will have cleared the jobs lock owner/times, but not yet the case instance lock time/owner
            cmmnEngineConfiguration.getCommandExecutor().execute(new ClearCaseInstanceLockTimesCmd(asyncExecutor.getLockOwner(), cmmnEngineConfiguration));
        }
        if (asyncHistoryExecutor != null && asyncHistoryExecutor.isActive()) {
            asyncHistoryExecutor.shutdown();
        }

        cmmnEngineConfiguration.close();

        if (cmmnEngineConfiguration.getEngineLifecycleListeners() != null) {
            for (EngineLifecycleListener engineLifecycleListener : cmmnEngineConfiguration.getEngineLifecycleListeners()) {
                engineLifecycleListener.onEngineClosed(this);
            }
        }
    }
    
    @Override
    public CmmnEngineConfiguration getCmmnEngineConfiguration() {
        return cmmnEngineConfiguration;
    }

    public void setCmmnEngineConfiguration(CmmnEngineConfiguration cmmnEngineConfiguration) {
        this.cmmnEngineConfiguration = cmmnEngineConfiguration;
    }
    
    @Override
    public CmmnRuntimeService getCmmnRuntimeService() {
        return cmmnRuntimeService;
    }

    public void setCmmnRuntimeService(CmmnRuntimeService cmmnRuntimeService) {
        this.cmmnRuntimeService = cmmnRuntimeService;
    }

    @Override
    public DynamicCmmnService getDynamicCmmnService() {
        return dynamicCmmnService;
    }

    public void setDynamicCmmnService(DynamicCmmnService dynamicCmmnService) {
        this.dynamicCmmnService = dynamicCmmnService;
    }

    @Override
    public CmmnTaskService getCmmnTaskService() {
        return cmmnTaskService;
    }

    public void setCmmnTaskService(CmmnTaskService cmmnTaskService) {
        this.cmmnTaskService = cmmnTaskService;
    }

    @Override
    public CmmnManagementService getCmmnManagementService() {
        return cmmnManagementService;
    }

    public void setCmmnManagementService(CmmnManagementService cmmnManagementService) {
        this.cmmnManagementService = cmmnManagementService;
    }

    @Override
    public CmmnRepositoryService getCmmnRepositoryService() {
        return cmmnRepositoryService;
    }
    
    public void setCmmnRepositoryService(CmmnRepositoryService cmmnRepositoryService) {
        this.cmmnRepositoryService = cmmnRepositoryService;
    }

    @Override
    public CmmnHistoryService getCmmnHistoryService() {
        return cmmnHistoryService;
    }

    public void setCmmnHistoryService(CmmnHistoryService cmmnHistoryService) {
        this.cmmnHistoryService = cmmnHistoryService;
    }

    @Override
    public CmmnMigrationService getCmmnMigrationService() {
        return cmmnMigrationService;
    }

    public void setCmmnMigrationService(CmmnMigrationService cmmnMigrationService) {
        this.cmmnMigrationService = cmmnMigrationService;
    }
}
