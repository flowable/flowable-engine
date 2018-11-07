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
package org.flowable.form.engine.impl.parser;

import java.io.InputStream;
import java.io.InputStreamReader;
import java.net.MalformedURLException;
import java.net.URL;
import java.util.ArrayList;
import java.util.List;

import org.apache.commons.io.IOUtils;
import org.flowable.common.engine.api.FlowableException;
import org.flowable.common.engine.impl.util.io.InputStreamSource;
import org.flowable.common.engine.impl.util.io.StreamSource;
import org.flowable.common.engine.impl.util.io.StringStreamSource;
import org.flowable.common.engine.impl.util.io.UrlStreamSource;
import org.flowable.editor.form.converter.FormJsonConverter;
import org.flowable.form.engine.FormEngineConfiguration;
import org.flowable.form.engine.impl.io.ResourceStreamSource;
import org.flowable.form.engine.impl.persistence.entity.FormDefinitionEntity;
import org.flowable.form.engine.impl.persistence.entity.FormDeploymentEntity;
import org.flowable.form.engine.impl.util.CommandContextUtil;
import org.flowable.form.model.SimpleFormModel;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

/**
 * Specific parsing of one form json file.
 * 
 * @author Tijs Rademakers
 * @author Joram Barrez
 */
public class FormDefinitionParse {

    protected static final Logger LOGGER = LoggerFactory.getLogger(FormDefinitionParse.class);

    protected String name;

    protected boolean validateSchema = true;

    protected StreamSource streamSource;
    protected String sourceSystemId;

    protected SimpleFormModel formModel;

    protected String targetNamespace;

    /** The deployment to which the parsed decision tables will be added. */
    protected FormDeploymentEntity deployment;

    /** The end result of the parsing: a list of decision tables. */
    protected List<FormDefinitionEntity> formDefinitions = new ArrayList<>();

    public FormDefinitionParse deployment(FormDeploymentEntity deployment) {
        this.deployment = deployment;
        return this;
    }

    public FormDefinitionParse execute(FormEngineConfiguration formEngineConfig) {
        String encoding = formEngineConfig.getXmlEncoding();
        FormJsonConverter converter = new FormJsonConverter();

        try {
            InputStreamReader in = null;
            if (encoding != null) {
                in = new InputStreamReader(streamSource.getInputStream(), encoding);
            } else {
                in = new InputStreamReader(streamSource.getInputStream());
            }

            String formJson = IOUtils.toString(in);
            formModel = converter.convertToFormModel(formJson);

            if (formModel != null && formModel.getFields() != null) {
                FormDefinitionEntity formDefinitionEntity = CommandContextUtil.getFormEngineConfiguration().getFormDefinitionEntityManager().create();
                formDefinitionEntity.setKey(formModel.getKey());
                formDefinitionEntity.setName(formModel.getName());
                formDefinitionEntity.setResourceName(name);
                formDefinitionEntity.setDeploymentId(deployment.getId());
                formDefinitionEntity.setDescription(formModel.getDescription());
                formDefinitions.add(formDefinitionEntity);
            }
        } catch (Exception e) {
            throw new FlowableException("Error parsing form definition JSON", e);
        }
        return this;
    }

    public FormDefinitionParse name(String name) {
        this.name = name;
        return this;
    }

    public FormDefinitionParse sourceInputStream(InputStream inputStream) {
        if (name == null) {
            name("inputStream");
        }
        setStreamSource(new InputStreamSource(inputStream));
        return this;
    }

    public FormDefinitionParse sourceUrl(URL url) {
        if (name == null) {
            name(url.toString());
        }
        setStreamSource(new UrlStreamSource(url));
        return this;
    }

    public FormDefinitionParse sourceUrl(String url) {
        try {
            return sourceUrl(new URL(url));
        } catch (MalformedURLException e) {
            throw new FlowableException("malformed url: " + url, e);
        }
    }

    public FormDefinitionParse sourceResource(String resource) {
        if (name == null) {
            name(resource);
        }
        setStreamSource(new ResourceStreamSource(resource));
        return this;
    }

    public FormDefinitionParse sourceString(String string) {
        if (name == null) {
            name("string");
        }
        setStreamSource(new StringStreamSource(string));
        return this;
    }

    protected void setStreamSource(StreamSource streamSource) {
        if (this.streamSource != null) {
            throw new FlowableException("invalid: multiple sources " + this.streamSource + " and " + streamSource);
        }
        this.streamSource = streamSource;
    }

    public String getSourceSystemId() {
        return sourceSystemId;
    }

    public FormDefinitionParse setSourceSystemId(String sourceSystemId) {
        this.sourceSystemId = sourceSystemId;
        return this;
    }

    /*
     * ------------------- GETTERS AND SETTERS -------------------
     */

    public List<FormDefinitionEntity> getFormDefinitions() {
        return formDefinitions;
    }

    public String getTargetNamespace() {
        return targetNamespace;
    }

    public FormDeploymentEntity getDeployment() {
        return deployment;
    }

    public void setDeployment(FormDeploymentEntity deployment) {
        this.deployment = deployment;
    }

    public SimpleFormModel getFormModel() {
        return formModel;
    }

    public void setFormModel(SimpleFormModel formModel) {
        this.formModel = formModel;
    }
}
