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
package org.flowable.cmmn.engine.interceptor;

import org.flowable.cmmn.engine.impl.persistence.entity.PlanItemInstanceEntity;
import org.flowable.cmmn.model.ExternalWorkerServiceTask;

/**
 * @author Filip Hrisafov
 */
public class CreateCmmnExternalWorkerJobBeforeContext {

    protected final ExternalWorkerServiceTask externalWorkerServiceTask;
    protected final PlanItemInstanceEntity planItemInstance;

    protected String jobCategory;
    protected String jobTopicExpression;

    public CreateCmmnExternalWorkerJobBeforeContext(ExternalWorkerServiceTask externalWorkerServiceTask, PlanItemInstanceEntity planItemInstance,
            String jobCategory, String jobTopicExpression) {
        this.externalWorkerServiceTask = externalWorkerServiceTask;
        this.planItemInstance = planItemInstance;
        this.jobCategory = jobCategory;
        this.jobTopicExpression = jobTopicExpression;
    }

    public ExternalWorkerServiceTask getExternalWorkerServiceTask() {
        return externalWorkerServiceTask;
    }

    public PlanItemInstanceEntity getPlanItemInstance() {
        return planItemInstance;
    }

    public String getJobCategory() {
        return jobCategory;
    }

    public void setJobCategory(String jobCategory) {
        this.jobCategory = jobCategory;
    }

    public String getJobTopicExpression() {
        return jobTopicExpression;
    }

    public void setJobTopicExpression(String jobTopicExpression) {
        this.jobTopicExpression = jobTopicExpression;
    }
}
