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
package org.flowable.bpmn.model;

/**
 * @author Filip Hrisafov
 */
public class ExternalWorkerServiceTask extends ServiceTask {

    protected String topic;

    public String getTopic() {
        return topic;
    }

    public void setTopic(String topic) {
        this.topic = topic;
    }

    @Override
    public ExternalWorkerServiceTask clone() {
        ExternalWorkerServiceTask clone = new ExternalWorkerServiceTask();
        clone.setValues(this);
        return clone;
    }

    public void setValues(ExternalWorkerServiceTask otherElement) {
        super.setValues(otherElement);
        setTopic(otherElement.getTopic());
    }
}
