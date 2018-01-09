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
package org.flowable.cmmn.model;

/**
 * @author Joram Barrez
 */
public class Task extends PlanItemDefinition {
    
    protected boolean blocking = true;
    protected String blockingExpression;
    
    protected boolean async;
    protected boolean exclusive;

    public boolean isBlocking() {
        return blocking;
    }

    public void setBlocking(boolean blocking) {
        this.blocking = blocking;
    }
    
    public String getBlockingExpression() {
        return blockingExpression;
    }

    public void setBlockingExpression(String blockingExpression) {
        this.blockingExpression = blockingExpression;
    }
    
    public boolean isAsync() {
        return async;
    }

    public void setAsync(boolean async) {
        this.async = async;
    }

    public boolean isExclusive() {
        return exclusive;
    }

    public void setExclusive(boolean exclusive) {
        this.exclusive = exclusive;
    }

    public void setValues(Task otherElement) {
        super.setValues(otherElement);
        setBlocking(otherElement.isBlocking());
        setBlockingExpression(otherElement.getBlockingExpression());
        setAsync(otherElement.isAsync());
        setExclusive(otherElement.isExclusive());
    }
}
