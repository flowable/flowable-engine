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
package org.flowable.task.service.impl;

import org.flowable.task.service.TaskServiceConfiguration;
import org.flowable.task.service.impl.persistence.entity.HistoricTaskInstanceEntityManager;
import org.flowable.task.service.impl.persistence.entity.TaskEntityManager;

/**
 * @author Tom Baeyens
 * @author Joram Barrez
 */
public class ServiceImpl {

    protected TaskServiceConfiguration taskServiceConfiguration;

    public ServiceImpl() {

    }

    public ServiceImpl(TaskServiceConfiguration taskServiceConfiguration) {
        this.taskServiceConfiguration = taskServiceConfiguration;
    }
    
    public TaskEntityManager getTaskEntityManager() {
        return taskServiceConfiguration.getTaskEntityManager();
    }
    
    public HistoricTaskInstanceEntityManager getHistoricTaskInstanceEntityManager() {
        return taskServiceConfiguration.getHistoricTaskInstanceEntityManager();
    }
}
