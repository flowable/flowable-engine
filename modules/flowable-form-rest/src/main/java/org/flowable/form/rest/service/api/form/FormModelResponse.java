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
package org.flowable.form.rest.service.api.form;

import java.util.List;

import org.flowable.form.api.FormInfo;
import org.flowable.form.model.FormField;
import org.flowable.form.model.FormOutcome;
import org.flowable.form.model.SimpleFormModel;

/**
 * @author Yvo Swillens
 */
public class FormModelResponse extends FormInfo {

    private static final long serialVersionUID = 1L;

    protected String url;
    protected List<FormField> fields;
    protected List<FormOutcome> outcomes;
    protected String outcomeVariableName;

    public FormModelResponse(FormInfo formInfo) {
        setId(formInfo.getId());
        setName(formInfo.getName());
        setDescription(formInfo.getDescription());
        setKey(formInfo.getKey());
        setVersion(formInfo.getVersion());
        
        SimpleFormModel formModel = (SimpleFormModel) formInfo.getFormModel();
        setFields(formModel.getFields());
        setOutcomes(formModel.getOutcomes());
        setOutcomeVariableName(formModel.getOutcomeVariableName());
    }

    public String getUrl() {
        return url;
    }

    public void setUrl(String url) {
        this.url = url;
    }

    public List<FormField> getFields() {
        return fields;
    }

    public void setFields(List<FormField> fields) {
        this.fields = fields;
    }

    public List<FormOutcome> getOutcomes() {
        return outcomes;
    }

    public void setOutcomes(List<FormOutcome> outcomes) {
        this.outcomes = outcomes;
    }

    public String getOutcomeVariableName() {
        return outcomeVariableName;
    }

    public void setOutcomeVariableName(String outcomeVariableName) {
        this.outcomeVariableName = outcomeVariableName;
    }
}
