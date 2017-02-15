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
package org.flowable.form.engine.impl.persistence.deploy;

import java.io.Serializable;

import org.flowable.form.engine.impl.persistence.entity.FormDefinitionEntity;

/**
 * @author Tijs Rademakers
 */
public class FormDefinitionCacheEntry implements Serializable {

    private static final long serialVersionUID = 1L;

    protected FormDefinitionEntity formDefinitionEntity;
    protected String formDefinitionJson;

    public FormDefinitionCacheEntry(FormDefinitionEntity formDefinitionEntity, String formDefinitionJson) {
        this.formDefinitionEntity = formDefinitionEntity;
        this.formDefinitionJson = formDefinitionJson;
    }

    public FormDefinitionEntity getFormDefinitionEntity() {
        return formDefinitionEntity;
    }

    public void setFormDefinitionEntity(FormDefinitionEntity formDefinitionEntity) {
        this.formDefinitionEntity = formDefinitionEntity;
    }

    public String getFormDefinitionJson() {
        return formDefinitionJson;
    }

    public void setFormDefinitionJson(String formDefinitionJson) {
        this.formDefinitionJson = formDefinitionJson;
    }
}
