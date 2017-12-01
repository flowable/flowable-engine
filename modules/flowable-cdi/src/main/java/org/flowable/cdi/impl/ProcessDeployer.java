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
package org.flowable.cdi.impl;

import java.net.URL;
import java.util.HashSet;
import java.util.Set;

import javax.xml.parsers.DocumentBuilderFactory;

import org.flowable.engine.ProcessEngine;
import org.flowable.engine.repository.Deployment;
import org.flowable.engine.repository.DeploymentBuilder;
import org.flowable.engine.repository.ProcessDefinition;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.w3c.dom.Document;
import org.w3c.dom.Element;
import org.w3c.dom.Node;
import org.w3c.dom.NodeList;

/**
 * Used to deploy processes. When the flowable-cdi extension is initialized, the classpath is scanned for a file named {@value #PROCESSES_FILE_NAME}. All processes listed in that file are
 * automatically deployed to the engine.
 *
 * @author Daniel Meyer
 */
public class ProcessDeployer {

    public static final String PROCESSES_FILE_NAME = "processes.xml";
    public static final String PROCESS_ELEMENT_NAME = "process";
    public static final String PROCESS_ATTR_RESOURCE = "resource";

    private static final Logger LOGGER = LoggerFactory.getLogger(ProcessDeployer.class);

    protected final ProcessEngine processEngine;

    public ProcessDeployer(ProcessEngine processEngine) {
        this.processEngine = processEngine;
    }

    /**
     * Deploys a single process
     *
     * @return the processDefinitionId of the deployed process as returned by {@link ProcessDefinition#getId()}
     */
    public String deployProcess(String resourceName) {
        LOGGER.debug("Start deploying single process.");
        // deploy processes as one deployment
        DeploymentBuilder deploymentBuilder = processEngine.getRepositoryService().createDeployment();
        deploymentBuilder.addClasspathResource(resourceName);
        // deploy the processes
        Deployment deployment = deploymentBuilder.deploy();
        LOGGER.debug("Process deployed");
        // retrieve the processDefinitionId for this process
        return processEngine.getRepositoryService().createProcessDefinitionQuery().deploymentId(deployment.getId()).singleResult().getId();
    }

    /**
     * Deploys all processes listed in the {@link #PROCESSES_FILE_NAME}-file.
     */
    public void deployProcesses() {
        // build a single deployment containing all discovered processes
        Set<String> resourceNames = getResourceNames();
        if (resourceNames.isEmpty()) {
            LOGGER.debug("Not creating a deployment");
            return;
        }
        LOGGER.debug("Start deploying processes.");
        DeploymentBuilder deploymentBuilder = processEngine.getRepositoryService().createDeployment();
        for (String string : resourceNames) {
            LOGGER.info("Adding '{}' to deployment.", string);
            deploymentBuilder.addClasspathResource(string);
        }
        // deploy the processes
        deploymentBuilder.deploy();
        LOGGER.debug("Done deploying processes.");
    }

    public Set<String> getResourceNames() {
        Set<String> result = new HashSet<>();
        URL processFileUrl = getClass().getClassLoader().getResource(PROCESSES_FILE_NAME);
        if (processFileUrl == null) {
            LOGGER.debug("No '{}'-file provided.", PROCESSES_FILE_NAME);
            // return empty set
            return result;
        }
        try {
            Document document = DocumentBuilderFactory.newInstance().newDocumentBuilder().parse(processFileUrl.openStream());
            NodeList nodeList = document.getElementsByTagName(PROCESS_ELEMENT_NAME);
            for (int i = 0; i < nodeList.getLength(); i++) {
                Node cn = nodeList.item(i);
                if (!(cn instanceof Element)) {
                    continue;
                }
                Element ce = (Element) cn;
                String resourceName = ce.getAttribute(PROCESS_ATTR_RESOURCE);
                if (resourceName == null || resourceName.length() == 0) {
                    continue;
                }
                result.add(resourceName);
            }
        } catch (Exception e) {
            LOGGER.error("could not parse file '{}'. {}", PROCESSES_FILE_NAME, e.getMessage(), e);
        }
        return result;
    }
}
