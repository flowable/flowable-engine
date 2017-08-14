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
package org.flowable.cmmn.engine.test;

import java.io.IOException;
import java.io.InputStream;

import org.flowable.cmmn.engine.CmmnEngine;
import org.flowable.cmmn.engine.CmmnEngineConfiguration;
import org.flowable.cmmn.engine.CmmnHistoryService;
import org.flowable.cmmn.engine.CmmnManagementService;
import org.flowable.cmmn.engine.CmmnRepositoryService;
import org.flowable.cmmn.engine.CmmnRuntimeService;
import org.flowable.cmmn.engine.test.impl.CmmnTestRunner;
import org.junit.Before;
import org.junit.BeforeClass;
import org.junit.runner.RunWith;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

/**
 * @author Joram Barrez
 */
@RunWith(CmmnTestRunner.class)
public class FlowableCmmnTestCase {
    
    private static final Logger logger = LoggerFactory.getLogger(FlowableCmmnTestCase.class);
    
    protected CmmnEngine cmmnEngine;
    protected CmmnManagementService cmmnManagementService;
    protected CmmnRepositoryService cmmnRepositoryService;
    protected CmmnRuntimeService cmmnRuntimeService;
    protected CmmnHistoryService cmmnHistoryService;
    
    @BeforeClass
    public static void setupEngine() {
        if (CmmnTestRunner.getCmmnEngine() == null) {
            initCmmnEngine();
        }
    }

    protected static void initCmmnEngine() {
        try (InputStream inputStream = FlowableCmmnTestCase.class.getClassLoader().getResourceAsStream("flowable.cmmn.cfg.xml")) {
            CmmnEngine cmmnEngine = CmmnEngineConfiguration.createCmmnEngineConfigurationFromInputStream(inputStream).buildCmmnEngine();
            CmmnTestRunner.setCmmnEngine(cmmnEngine);
        } catch (IOException e) {
            logger.error("Could not create CMMN engine", e);
        }
    }
    
    @Before
    public void setupServices() {
        CmmnEngine cmmnEngine = CmmnTestRunner.getCmmnEngine();
        this.cmmnEngine = cmmnEngine;
        this.cmmnRepositoryService = cmmnEngine.getCmmnRepositoryService();
        this.cmmnManagementService = cmmnEngine.getCmmnManagementService();
        this.cmmnRuntimeService = cmmnEngine.getCmmnRuntimeService();
        this.cmmnHistoryService = cmmnEngine.getCmmnHistoryService();
    }
    
}
