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
package org.flowable.form.engine.impl.deployer;

import java.io.ByteArrayInputStream;
import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

import org.flowable.form.engine.impl.parser.FormDefinitionParse;
import org.flowable.form.engine.impl.parser.FormDefinitionParseFactory;
import org.flowable.form.engine.impl.persistence.entity.FormDefinitionEntity;
import org.flowable.form.engine.impl.persistence.entity.FormDeploymentEntity;
import org.flowable.form.engine.impl.persistence.entity.FormResourceEntity;
import org.flowable.form.engine.impl.util.CommandContextUtil;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

public class ParsedDeploymentBuilder {

    private static final Logger LOGGER = LoggerFactory.getLogger(ParsedDeploymentBuilder.class);

    public static final String[] FORM_RESOURCE_SUFFIXES = new String[] { "form" };

    protected FormDeploymentEntity deployment;
    protected FormDefinitionParseFactory formDefinitionParseFactory;

    public ParsedDeploymentBuilder(FormDeploymentEntity deployment, FormDefinitionParseFactory formDefinitionParseFactory) {
        this.deployment = deployment;
        this.formDefinitionParseFactory = formDefinitionParseFactory;
    }

    public ParsedDeployment build() {
        List<FormDefinitionEntity> formDefinitions = new ArrayList<>();
        Map<FormDefinitionEntity, FormDefinitionParse> formDefinitionToParseMap = new LinkedHashMap<>();
        Map<FormDefinitionEntity, FormResourceEntity> formDefinitionToResourceMap = new LinkedHashMap<>();

        for (FormResourceEntity resource : deployment.getResources().values()) {
            if (isFormResource(resource.getName())) {
                LOGGER.debug("Processing Form definition resource {}", resource.getName());
                FormDefinitionParse parse = createFormParseFromResource(resource);
                for (FormDefinitionEntity formDefinition : parse.getFormDefinitions()) {
                    formDefinitions.add(formDefinition);
                    formDefinitionToParseMap.put(formDefinition, parse);
                    formDefinitionToResourceMap.put(formDefinition, resource);
                }
            }
        }

        return new ParsedDeployment(deployment, formDefinitions, formDefinitionToParseMap, formDefinitionToResourceMap);
    }

    protected FormDefinitionParse createFormParseFromResource(FormResourceEntity resource) {
        String resourceName = resource.getName();
        ByteArrayInputStream inputStream = new ByteArrayInputStream(resource.getBytes());

        FormDefinitionParse formParse = formDefinitionParseFactory.createParse()
                .sourceInputStream(inputStream)
                .setSourceSystemId(resourceName)
                .deployment(deployment)
                .name(resourceName);

        formParse.execute(CommandContextUtil.getFormEngineConfiguration());
        return formParse;
    }

    protected boolean isFormResource(String resourceName) {
        for (String suffix : FORM_RESOURCE_SUFFIXES) {
            if (resourceName.endsWith(suffix)) {
                return true;
            }
        }

        return false;
    }

}
