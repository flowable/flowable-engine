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

public class DynamicUserTaskBuilder {

    protected String id;
    protected String name;
    protected String assignee;
    protected boolean joinParallelActivitiesOnComplete = true;
    protected DynamicUserTaskCallback dynamicUserTaskCallback;

    public DynamicUserTaskBuilder() {

    }

    public DynamicUserTaskBuilder(String id) {
        this.id = id;
    }

    public String getId() {
        return id;
    }

    public void setId(String id) {
        this.id = id;
    }

    public DynamicUserTaskBuilder id(String id) {
        this.id = id;
        return this;
    }

    public String getName() {
        return name;
    }

    public void setName(String name) {
        this.name = name;
    }

    public DynamicUserTaskBuilder name(String name) {
        this.name = name;
        return this;
    }

    public String getAssignee() {
        return assignee;
    }

    public void setAssignee(String assignee) {
        this.assignee = assignee;
    }

    public DynamicUserTaskBuilder assignee(String assignee) {
        this.assignee = assignee;
        return this;
    }

    public boolean isJoinParallelActivitiesOnComplete() {
        return joinParallelActivitiesOnComplete;
    }

    public void setJoinParallelActivitiesOnComplete(boolean joinParallelActivitiesOnComplete) {
        this.joinParallelActivitiesOnComplete = joinParallelActivitiesOnComplete;
    }

    public DynamicUserTaskBuilder joinParallelActivitiesOnComplete(boolean joinParallelActivitiesOnComplete) {
        this.joinParallelActivitiesOnComplete = joinParallelActivitiesOnComplete;
        return this;
    }
    
    public DynamicUserTaskCallback getDynamicUserTaskCallback() {
        return dynamicUserTaskCallback;
    }

    public void setDynamicUserTaskCallback(DynamicUserTaskCallback dynamicUserTaskCallback) {
        this.dynamicUserTaskCallback = dynamicUserTaskCallback;
    }

    public DynamicUserTaskBuilder dynamicUserTaskCallback(DynamicUserTaskCallback dynamicUserTaskCallback) {
        this.dynamicUserTaskCallback = dynamicUserTaskCallback;
        return this;
    }

}