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
package org.flowable.app.engine.impl.persistence.entity.deploy;

import org.flowable.app.api.repository.AppDefinition;
import org.flowable.app.api.repository.AppModel;

public class AppDefinitionCacheEntry {
    
    protected AppDefinition appDefinition;
    protected AppModel appModel;

    public AppDefinitionCacheEntry(AppDefinition appDefinition, AppModel appModel) {
        this.appDefinition = appDefinition;
        this.appModel = appModel;
    }

    public AppDefinition getAppDefinition() {
        return appDefinition;
    }

    public void setAppDefinition(AppDefinition appDefinition) {
        this.appDefinition = appDefinition;
    }

    public AppModel getAppModel() {
        return appModel;
    }

    public void setAppModel(AppModel appModel) {
        this.appModel = appModel;
    }

}
