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
package org.flowable.engine.data.inmemory.impl.job;

import java.util.List;
import java.util.stream.Collectors;

import org.flowable.common.engine.impl.query.AbstractQuery;
import org.flowable.engine.ProcessEngineConfiguration;
import org.flowable.engine.data.inmemory.AbstractMemoryDataManager;
import org.flowable.engine.data.inmemory.util.MapProvider;
import org.flowable.engine.repository.ProcessDefinition;
import org.flowable.job.api.Job;
import org.flowable.job.service.JobServiceConfiguration;
import org.flowable.job.service.impl.persistence.entity.AbstractRuntimeJobEntity;
import org.slf4j.Logger;

/**
 * Abstract base class for {@link org.flowable.job.api.AbstractJobEntity}
 * in-memory data manager implementations.
 * 
 * @author ikaakkola (Qvantel Finland Oy)
 */
public class AbstractJobMemoryDataManager<T extends AbstractRuntimeJobEntity> extends AbstractMemoryDataManager<T> {

    protected static final String SCOPE_ALL = "all";

    protected static final String SCOPE_BPMN = "bpmn";

    private final ProcessEngineConfiguration processEngineConfiguration;

    private final JobServiceConfiguration jobServiceConfiguration;

    public AbstractJobMemoryDataManager(Logger logger, MapProvider mapProvider, ProcessEngineConfiguration processEngineConfiguration,
                    JobServiceConfiguration jobServiceConfiguration) {
        super(logger, mapProvider, jobServiceConfiguration.getIdGenerator());
        this.processEngineConfiguration = processEngineConfiguration;
        this.jobServiceConfiguration = jobServiceConfiguration;
    }

    protected void internalUpdateJobTenantIdForDeployment(String deploymentId, String newTenantId) {
        List<ProcessDefinition> definitions = processEngineConfiguration.getRepositoryService().createProcessDefinitionQuery().deploymentId(deploymentId)
                        .list();
        getData().values().stream().filter(item -> definitions.stream().anyMatch(def -> def.getId().equals(item.getProcessDefinitionId()))).forEach(item -> {
            item.setTenantId(newTenantId);
        });
    }

    protected ProcessEngineConfiguration getProcessEngineConfiguration() {
        return processEngineConfiguration;
    }

    protected JobServiceConfiguration getJobServiceConfiguration() {
        return jobServiceConfiguration;
    }

    protected void doDeleteJobsByExecutionId(String executionId) {
        getData().entrySet().removeIf(item -> executionId != null && executionId.equals(item.getValue().getExecutionId()));
    }

    protected List<T> doFindJobsByScopeIdAndSubScopeId(String scopeId, String subScopeId) {
        if (logger.isTraceEnabled()) {
            logger.trace("findJobsByScopeIdAndSubScopeId {} {}", scopeId, subScopeId);
        }
        return getData().values().stream().filter(item -> item.getScopeId() != null && item.getScopeId().equals(scopeId) && item.getSubScopeId() != null
                        && item.getSubScopeId().equals(subScopeId)).collect(Collectors.toList());
    }

    protected List<Job> sortAndLimitJobs(List<Job> collect, AbstractQuery< ? , Job> query) {
        if (collect == null || collect.isEmpty()) {
            return collect;
        }
        return sortAndPaginate(collect, JobComparator.resolve(query.getOrderBy()), query.getFirstResult(), query.getMaxResults());
    }

}
