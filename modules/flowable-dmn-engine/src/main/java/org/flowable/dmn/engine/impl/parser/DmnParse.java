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
package org.flowable.dmn.engine.impl.parser;

import java.io.InputStream;
import java.net.MalformedURLException;
import java.net.URL;
import java.util.ArrayList;
import java.util.List;

import org.flowable.common.engine.api.FlowableException;
import org.flowable.common.engine.impl.util.io.InputStreamSource;
import org.flowable.common.engine.impl.util.io.StreamSource;
import org.flowable.common.engine.impl.util.io.StringStreamSource;
import org.flowable.common.engine.impl.util.io.UrlStreamSource;
import org.flowable.dmn.engine.DmnEngineConfiguration;
import org.flowable.dmn.engine.impl.io.ResourceStreamSource;
import org.flowable.dmn.engine.impl.persistence.entity.DecisionTableEntity;
import org.flowable.dmn.engine.impl.persistence.entity.DmnDeploymentEntity;
import org.flowable.dmn.engine.impl.util.CommandContextUtil;
import org.flowable.dmn.model.Decision;
import org.flowable.dmn.model.DmnDefinition;
import org.flowable.dmn.xml.constants.DmnXMLConstants;
import org.flowable.dmn.xml.converter.DmnXMLConverter;
import org.flowable.dmn.xml.exception.DmnXMLException;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

/**
 * Specific parsing of one BPMN 2.0 XML file, created by the {@link DmnParse}.
 * 
 * @author Tijs Rademakers
 * @author Joram Barrez
 */
public class DmnParse implements DmnXMLConstants {

    protected static final Logger LOGGER = LoggerFactory.getLogger(DmnParse.class);

    protected String name;

    protected boolean validateSchema = true;

    protected StreamSource streamSource;
    protected String sourceSystemId;

    protected DmnDefinition dmnDefinition;

    protected String targetNamespace;

    /** The deployment to which the parsed decision tables will be added. */
    protected DmnDeploymentEntity deployment;

    /** The end result of the parsing: a list of decision tables. */
    protected List<DecisionTableEntity> decisionTables = new ArrayList<>();

    public DmnParse deployment(DmnDeploymentEntity deployment) {
        this.deployment = deployment;
        return this;
    }

    public DmnParse execute(DmnEngineConfiguration dmnEngineConfig) {
        try {

            DmnXMLConverter converter = new DmnXMLConverter();

            boolean enableSafeDmnXml = dmnEngineConfig.isEnableSafeDmnXml();
            String encoding = dmnEngineConfig.getXmlEncoding();

            if (encoding != null) {
                dmnDefinition = converter.convertToDmnModel(streamSource, validateSchema, enableSafeDmnXml, encoding);
            } else {
                dmnDefinition = converter.convertToDmnModel(streamSource, validateSchema, enableSafeDmnXml);
            }

            if (dmnDefinition != null && dmnDefinition.getDecisions() != null) {
                for (Decision decision : dmnDefinition.getDecisions()) {
                    DecisionTableEntity decisionTableEntity = CommandContextUtil.getDmnEngineConfiguration().getDecisionTableEntityManager().create();
                    decisionTableEntity.setKey(decision.getId());
                    decisionTableEntity.setName(decision.getName());
                    decisionTableEntity.setResourceName(name);
                    decisionTableEntity.setDeploymentId(deployment.getId());
                    decisionTableEntity.setParentDeploymentId(deployment.getParentDeploymentId());
                    decisionTableEntity.setDescription(decision.getDescription());
                    decisionTables.add(decisionTableEntity);
                }
            }

        } catch (Exception e) {
            if (e instanceof FlowableException) {
                throw (FlowableException) e;
            } else if (e instanceof DmnXMLException) {
                throw (DmnXMLException) e;
            } else {
                throw new FlowableException("Error parsing XML", e);
            }
        }

        return this;
    }

    public DmnParse name(String name) {
        this.name = name;
        return this;
    }

    public DmnParse sourceInputStream(InputStream inputStream) {
        if (name == null) {
            name("inputStream");
        }
        setStreamSource(new InputStreamSource(inputStream));
        return this;
    }

    public DmnParse sourceUrl(URL url) {
        if (name == null) {
            name(url.toString());
        }
        setStreamSource(new UrlStreamSource(url));
        return this;
    }

    public DmnParse sourceUrl(String url) {
        try {
            return sourceUrl(new URL(url));
        } catch (MalformedURLException e) {
            throw new FlowableException("malformed url: " + url, e);
        }
    }

    public DmnParse sourceResource(String resource) {
        if (name == null) {
            name(resource);
        }
        setStreamSource(new ResourceStreamSource(resource));
        return this;
    }

    public DmnParse sourceString(String string) {
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

    public DmnParse setSourceSystemId(String sourceSystemId) {
        this.sourceSystemId = sourceSystemId;
        return this;
    }

    /*
     * ------------------- GETTERS AND SETTERS -------------------
     */

    public boolean isValidateSchema() {
        return validateSchema;
    }

    public void setValidateSchema(boolean validateSchema) {
        this.validateSchema = validateSchema;
    }

    public List<DecisionTableEntity> getDecisionTables() {
        return decisionTables;
    }

    public String getTargetNamespace() {
        return targetNamespace;
    }

    public DmnDeploymentEntity getDeployment() {
        return deployment;
    }

    public void setDeployment(DmnDeploymentEntity deployment) {
        this.deployment = deployment;
    }

    public DmnDefinition getDmnDefinition() {
        return dmnDefinition;
    }

    public void setDmnDefinition(DmnDefinition dmnDefinition) {
        this.dmnDefinition = dmnDefinition;
    }
}
