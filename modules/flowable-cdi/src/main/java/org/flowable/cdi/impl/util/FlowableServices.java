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

package org.flowable.cdi.impl.util;

import javax.enterprise.context.ApplicationScoped;
import javax.enterprise.inject.Produces;
import javax.inject.Named;

import org.flowable.engine.FormService;
import org.flowable.engine.HistoryService;
import org.flowable.engine.IdentityService;
import org.flowable.engine.ManagementService;
import org.flowable.engine.ProcessEngine;
import org.flowable.engine.RepositoryService;
import org.flowable.engine.RuntimeService;
import org.flowable.engine.TaskService;

/**
 * Makes the managed process engine and the provided services available for injection
 * 
 * @author Daniel Meyer
 * @author Falko Menge
 */
@ApplicationScoped
public class FlowableServices {

    private ProcessEngine processEngine;

    public void setProcessEngine(ProcessEngine processEngine) {
        this.processEngine = processEngine;
    }

    @Produces
    @Named
    @ApplicationScoped
    public ProcessEngine processEngine() {
        return processEngine;
    }

    @Produces
    @Named
    @ApplicationScoped
    public RuntimeService runtimeService() {
        return processEngine().getRuntimeService();
    }

    @Produces
    @Named
    @ApplicationScoped
    public TaskService taskService() {
        return processEngine().getTaskService();
    }

    @Produces
    @Named
    @ApplicationScoped
    public RepositoryService repositoryService() {
        return processEngine().getRepositoryService();
    }

    @Produces
    @Named
    @ApplicationScoped
    public FormService formService() {
        return processEngine().getFormService();
    }

    @Produces
    @Named
    @ApplicationScoped
    public HistoryService historyService() {
        return processEngine().getHistoryService();
    }

    @Produces
    @Named
    @ApplicationScoped
    public IdentityService identityService() {
        return processEngine().getIdentityService();
    }

    @Produces
    @Named
    @ApplicationScoped
    public ManagementService managementService() {
        return processEngine().getManagementService();
    }

}
