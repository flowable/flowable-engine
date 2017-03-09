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
package org.flowable.engine.impl.dynamic;

public class DynamicEmbeddedSubProcessBuilder {

    protected String id;
    protected String processDefinitionId;
    protected boolean joinParallelActivitiesOnComplete = true;

    public DynamicEmbeddedSubProcessBuilder() {

    }

    public DynamicEmbeddedSubProcessBuilder(String id) {
        this.id = id;
    }

    public String getId() {
        return id;
    }

    public void setId(String id) {
        this.id = id;
    }

    public DynamicEmbeddedSubProcessBuilder id(String id) {
        this.id = id;
        return this;
    }

    public String getProcessDefinitionId() {
        return processDefinitionId;
    }

    public void setProcessDefinitionId(String processDefinitionId) {
        this.processDefinitionId = processDefinitionId;
    }

    public DynamicEmbeddedSubProcessBuilder processDefinitionId(String processDefinitionId) {
        this.processDefinitionId = processDefinitionId;
        return this;
    }

    public boolean isJoinParallelActivitiesOnComplete() {
        return joinParallelActivitiesOnComplete;
    }

    public void setJoinParallelActivitiesOnComplete(boolean joinParallelActivitiesOnComplete) {
        this.joinParallelActivitiesOnComplete = joinParallelActivitiesOnComplete;
    }

    public DynamicEmbeddedSubProcessBuilder joinParallelActivitiesOnComplete(boolean joinParallelActivitiesOnComplete) {
        this.joinParallelActivitiesOnComplete = joinParallelActivitiesOnComplete;
        return this;
    }

}