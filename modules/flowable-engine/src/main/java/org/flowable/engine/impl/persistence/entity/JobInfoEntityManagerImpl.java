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

package org.flowable.engine.impl.persistence.entity;

import java.util.List;

import org.flowable.engine.common.impl.Page;
import org.flowable.engine.common.impl.persistence.entity.data.DataManager;
import org.flowable.engine.impl.cfg.ProcessEngineConfigurationImpl;
import org.flowable.engine.impl.persistence.entity.data.JobInfoDataManager;

/**
 * @author Tom Baeyens
 * @author Daniel Meyer
 * @author Joram Barrez
 */
public abstract class JobInfoEntityManagerImpl<T extends JobInfoEntity> extends AbstractEntityManager<T> implements JobInfoEntityManager<T> {

    protected JobInfoDataManager<T> jobDataManager;

    public JobInfoEntityManagerImpl(ProcessEngineConfigurationImpl processEngineConfiguration, JobInfoDataManager<T> jobDataManager) {
        super(processEngineConfiguration);
        this.jobDataManager = jobDataManager;
    }

    @Override
    protected DataManager<T> getDataManager() {
        return jobDataManager;
    }

    public List<T> findJobsToExecute(Page page) {
        return jobDataManager.findJobsToExecute(page);
    }

    @Override
    public List<T> findJobsByExecutionId(String executionId) {
        return jobDataManager.findJobsByExecutionId(executionId);
    }

    @Override
    public List<T> findJobsByProcessInstanceId(String processInstanceId) {
        return jobDataManager.findJobsByProcessInstanceId(processInstanceId);
    }

    @Override
    public List<T> findExpiredJobs(Page page) {
        return jobDataManager.findExpiredJobs(page);
    }

    @Override
    public void resetExpiredJob(String jobId) {
        jobDataManager.resetExpiredJob(jobId);
    }

    @Override
    public void updateJobTenantIdForDeployment(String deploymentId, String newTenantId) {
        jobDataManager.updateJobTenantIdForDeployment(deploymentId, newTenantId);
    }

    public JobInfoDataManager<T> getJobDataManager() {
        return jobDataManager;
    }

    public void setJobDataManager(JobInfoDataManager<T> jobDataManager) {
        this.jobDataManager = jobDataManager;
    }

}
