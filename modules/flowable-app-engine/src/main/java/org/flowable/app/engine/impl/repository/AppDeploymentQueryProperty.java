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

package org.flowable.app.engine.impl.repository;

import java.util.HashMap;
import java.util.Map;

import org.flowable.common.engine.api.query.QueryProperty;

/**
 * @author Joram Barrez
 */
public class AppDeploymentQueryProperty implements QueryProperty {

    private static final long serialVersionUID = 1L;

    private static final Map<String, AppDeploymentQueryProperty> properties = new HashMap<>();

    public static final AppDeploymentQueryProperty DEPLOYMENT_ID = new AppDeploymentQueryProperty("RES.ID_");
    public static final AppDeploymentQueryProperty DEPLOYMENT_NAME = new AppDeploymentQueryProperty("RES.NAME_");
    public static final AppDeploymentQueryProperty DEPLOYMENT_TENANT_ID = new AppDeploymentQueryProperty("RES.TENANT_ID_");
    public static final AppDeploymentQueryProperty DEPLOY_TIME = new AppDeploymentQueryProperty("RES.DEPLOY_TIME_");

    private String name;

    public AppDeploymentQueryProperty(String name) {
        this.name = name;
        properties.put(name, this);
    }

    @Override
    public String getName() {
        return name;
    }

    public static AppDeploymentQueryProperty findByName(String propertyName) {
        return properties.get(propertyName);
    }

}
