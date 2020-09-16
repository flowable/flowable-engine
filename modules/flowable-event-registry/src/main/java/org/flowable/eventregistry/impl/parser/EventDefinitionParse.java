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
package org.flowable.eventregistry.impl.parser;

import java.io.InputStream;
import java.io.InputStreamReader;
import java.io.UnsupportedEncodingException;
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
import org.flowable.eventregistry.impl.EventRegistryEngineConfiguration;
import org.flowable.eventregistry.impl.io.ResourceStreamSource;
import org.flowable.eventregistry.impl.persistence.entity.EventDefinitionEntity;
import org.flowable.eventregistry.impl.persistence.entity.EventDeploymentEntity;
import org.flowable.eventregistry.json.converter.EventJsonConverter;
import org.flowable.eventregistry.model.EventModel;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

/**
 * Specific parsing of one event json file.
 * 
 * @author Tijs Rademakers
 * @author Joram Barrez
 */
public class EventDefinitionParse {

    protected static final Logger LOGGER = LoggerFactory.getLogger(EventDefinitionParse.class);

    protected String name;

    protected StreamSource streamSource;
    
    protected String sourceSystemId;

    protected EventModel eventModel;

    /** The deployment to which the parsed decision tables will be added. */
    protected EventDeploymentEntity deployment;

    /** The end result of the parsing: a list of event definitions. */
    protected List<EventDefinitionEntity> eventDefinitions = new ArrayList<>();

    public EventDefinitionParse deployment(EventDeploymentEntity deployment) {
        this.deployment = deployment;
        return this;
    }

    public EventDefinitionParse execute(EventRegistryEngineConfiguration eventEngineConfig) {
        String encoding = eventEngineConfig.getXmlEncoding();
        EventJsonConverter converter = eventEngineConfig.getEventJsonConverter();

        try (InputStreamReader in = newInputStreamReaderForSource(encoding)) {
            String eventJson = IOUtils.toString(in);
            eventModel = converter.convertToEventModel(eventJson);

            if (eventModel != null && eventModel.getKey() != null) {
                EventDefinitionEntity eventDefinitionEntity = eventEngineConfig.getEventDefinitionEntityManager().create();
                eventDefinitionEntity.setKey(eventModel.getKey());
                eventDefinitionEntity.setName(eventModel.getName());
                eventDefinitionEntity.setResourceName(name);
                eventDefinitionEntity.setDeploymentId(deployment.getId());
                eventDefinitions.add(eventDefinitionEntity);
            }
        } catch (Exception e) {
            throw new FlowableException("Error parsing event definition JSON", e);
        }
        return this;
    }

    private InputStreamReader newInputStreamReaderForSource(String encoding) throws UnsupportedEncodingException {
        if (encoding != null) {
            return new InputStreamReader(streamSource.getInputStream(), encoding);
        } else {
            return new InputStreamReader(streamSource.getInputStream());
        }
    }

    public EventDefinitionParse name(String name) {
        this.name = name;
        return this;
    }

    public EventDefinitionParse sourceInputStream(InputStream inputStream) {
        if (name == null) {
            name("inputStream");
        }
        setStreamSource(new InputStreamSource(inputStream));
        return this;
    }
    
    public String getSourceSystemId() {
        return sourceSystemId;
    }

    public EventDefinitionParse setSourceSystemId(String sourceSystemId) {
        this.sourceSystemId = sourceSystemId;
        return this;
    }

    public EventDefinitionParse sourceUrl(URL url) {
        if (name == null) {
            name(url.toString());
        }
        setStreamSource(new UrlStreamSource(url));
        return this;
    }

    public EventDefinitionParse sourceUrl(String url) {
        try {
            return sourceUrl(new URL(url));
        } catch (MalformedURLException e) {
            throw new FlowableException("malformed url: " + url, e);
        }
    }

    public EventDefinitionParse sourceResource(String resource) {
        if (name == null) {
            name(resource);
        }
        setStreamSource(new ResourceStreamSource(resource));
        return this;
    }

    public EventDefinitionParse sourceString(String string) {
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

    /*
     * ------------------- GETTERS AND SETTERS -------------------
     */

    public List<EventDefinitionEntity> getEventDefinitions() {
        return eventDefinitions;
    }

    public EventDeploymentEntity getDeployment() {
        return deployment;
    }

    public void setDeployment(EventDeploymentEntity deployment) {
        this.deployment = deployment;
    }

    public EventModel getEventModel() {
        return eventModel;
    }

    public void setEventModel(EventModel eventModel) {
        this.eventModel = eventModel;
    }
}
