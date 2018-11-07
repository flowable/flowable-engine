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
package org.flowable.form.engine.test;

import java.time.Instant;
import java.util.Date;

import org.flowable.form.engine.FormEngine;

/**
 * A Helper for the Flowable {@link FlowableFormExtension} that can be used within the JUnit Jupiter context store
 * and users can use it in the tests for easy modifying of the {@link FormEngine} time and easy access for waiting on the job executor.
 *
 * @author Filip Hrisafov
 */
public class FlowableFormTestHelper {

    protected final FormEngine formEngine;
    protected String deploymentIdFromDeploymentAnnotation;

    public FlowableFormTestHelper(FormEngine formEngine) {
        this.formEngine = formEngine;
    }

    public FormEngine getFormEngine() {
        return formEngine;
    }

    public String getDeploymentIdFromDeploymentAnnotation() {
        return deploymentIdFromDeploymentAnnotation;
    }

    public void setDeploymentIdFromDeploymentAnnotation(String deploymentIdFromDeploymentAnnotation) {
        this.deploymentIdFromDeploymentAnnotation = deploymentIdFromDeploymentAnnotation;
    }

    public void setCurrentTime(Date date) {
        formEngine.getFormEngineConfiguration().getClock().setCurrentTime(date);
    }

    public void setCurrentTime(Instant instant) {
        formEngine.getFormEngineConfiguration().getClock().setCurrentTime(instant == null ? null : Date.from(instant));
    }

}
