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
package org.flowable.app.engine.test;

import java.io.IOException;
import java.io.InputStream;
import java.util.Date;

import org.flowable.app.api.AppManagementService;
import org.flowable.app.api.AppRepositoryService;
import org.flowable.app.engine.AppEngine;
import org.flowable.app.engine.AppEngineConfiguration;
import org.flowable.app.engine.impl.cfg.StandaloneInMemAppEngineConfiguration;
import org.flowable.app.engine.test.impl.AppTestRunner;
import org.junit.After;
import org.junit.Before;
import org.junit.BeforeClass;
import org.junit.runner.RunWith;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

/**
 * @author Tijs Rademakers
 */
@RunWith(AppTestRunner.class)
public abstract class FlowableAppTestCase {

    private static final Logger LOGGER = LoggerFactory.getLogger(FlowableAppTestCase.class);

    public static String FLOWABLE_APP_CFG_XML = "flowable.app.cfg.xml";

    protected AppEngineConfiguration appEngineConfiguration;
    protected AppManagementService appManagementService;
    protected AppRepositoryService appRepositoryService;

    protected String deploymentId;

    @BeforeClass
    public static void setupEngine() {
        if (AppTestRunner.getAppEngineConfiguration() == null) {
            initAppEngine();
        }
    }

    protected static void initAppEngine() {
        try (InputStream inputStream = FlowableAppTestCase.class.getClassLoader().getResourceAsStream(FLOWABLE_APP_CFG_XML)) {
            AppEngine cmmnEngine = null;
            if (inputStream != null) {
                cmmnEngine = AppEngineConfiguration.createAppEngineConfigurationFromInputStream(inputStream).buildAppEngine();
            } else {
                LOGGER.info("No {} configuration found. Using default in-memory standalone configuration.", FLOWABLE_APP_CFG_XML);
                cmmnEngine = new StandaloneInMemAppEngineConfiguration().buildAppEngine();
            }
            AppTestRunner.setAppEngineConfiguration(cmmnEngine.getAppEngineConfiguration());
        } catch (IOException e) {
            LOGGER.error("Could not create App engine", e);
        }
    }

    @Before
    public void setupServices() {
        AppEngineConfiguration appEngineConfiguration = AppTestRunner.getAppEngineConfiguration();
        this.appEngineConfiguration = appEngineConfiguration;
        this.appRepositoryService = appEngineConfiguration.getAppRepositoryService();
        this.appManagementService = appEngineConfiguration.getAppManagementService();
    }

    @After
    public void cleanupDeployment() {
        if (deploymentId != null) {
           appRepositoryService.deleteDeployment(deploymentId, true);
        }
    }

    protected Date setClockFixedToCurrentTime() {
        Date date = new Date();
        appEngineConfiguration.getClock().setCurrentTime(date);
        return date;
    }
    
    protected void setClockTo(Date date) {
        appEngineConfiguration.getClock().setCurrentTime(date);
    }

}
