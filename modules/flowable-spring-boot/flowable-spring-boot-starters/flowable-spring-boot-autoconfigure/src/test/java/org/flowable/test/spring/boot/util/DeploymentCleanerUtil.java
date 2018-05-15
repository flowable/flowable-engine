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
package org.flowable.test.spring.boot.util;

import java.util.List;

import org.flowable.app.api.repository.AppDeployment;
import org.flowable.app.engine.AppEngine;
import org.flowable.cmmn.api.repository.CmmnDeployment;
import org.flowable.cmmn.engine.CmmnEngine;
import org.flowable.dmn.api.DmnDeployment;
import org.flowable.dmn.engine.DmnEngine;
import org.flowable.engine.ProcessEngine;
import org.flowable.engine.repository.Deployment;
import org.flowable.form.api.FormDeployment;
import org.flowable.form.engine.FormEngine;

/**
 * @author Filip Hrisafov
 */
public class DeploymentCleanerUtil {

    public static void deleteDeployments(AppEngine appEngine) {
        List<AppDeployment> appDeployments = appEngine.getAppRepositoryService().createDeploymentQuery().list();
        for (AppDeployment appDeployment : appDeployments) {
            appEngine.getAppRepositoryService().deleteDeployment(appDeployment.getId(), true);
        }
    }

    public static void deleteDeployments(ProcessEngine processEngine) {
        List<Deployment> deployments = processEngine.getRepositoryService().createDeploymentQuery().list();
        for (Deployment deployment : deployments) {
            processEngine.getRepositoryService().deleteDeployment(deployment.getId(), true);
        }
    }

    public static void deleteDeployments(CmmnEngine cmmnEngine) {
        List<CmmnDeployment> cmmnDeployments = cmmnEngine.getCmmnRepositoryService().createDeploymentQuery().list();
        for (CmmnDeployment cmmnDeployment : cmmnDeployments) {
            cmmnEngine.getCmmnRepositoryService().deleteDeployment(cmmnDeployment.getId(), true);
        }
    }

    public static void deleteDeployments(DmnEngine dmnEngine) {
        List<DmnDeployment> dmnDeployments = dmnEngine.getDmnRepositoryService().createDeploymentQuery().list();
        for (DmnDeployment dmnDeployment : dmnDeployments) {
            dmnEngine.getDmnRepositoryService().deleteDeployment(dmnDeployment.getId());
        }
    }

    public static void deleteDeployments(FormEngine formEngine) {
        List<FormDeployment> formDeployments = formEngine.getFormRepositoryService().createDeploymentQuery().list();
        for (FormDeployment formDeployment : formDeployments) {
            formEngine.getFormRepositoryService().deleteDeployment(formDeployment.getId());
        }
    }

}
