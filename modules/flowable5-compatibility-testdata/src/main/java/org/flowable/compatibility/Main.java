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
package org.flowable.compatibility;

import java.util.ArrayList;
import java.util.List;
import java.util.Set;

import org.activiti.engine.ProcessEngine;
import org.activiti.engine.ProcessEngineConfiguration;
import org.flowable.compatibility.testdata.Flowable5TestDataGenerator;
import org.reflections.Reflections;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

public class Main {

    private static final Logger LOGGER = LoggerFactory.getLogger(Main.class);

    public static void main(String[] args) throws Exception {
        LOGGER.info("Booting up v5 Process Engine");
        ProcessEngine processEngine = ProcessEngineConfiguration.createProcessEngineConfigurationFromResource("v5.cfg.xml").buildProcessEngine();
        LOGGER.info("Starting test data generation.");
        List<String> executedGenerators = new ArrayList<String>();
        Reflections reflections = new Reflections("org.flowable.compatibility.testdata.generator");
        Set<Class<? extends Flowable5TestDataGenerator>> generatorClasses = reflections.getSubTypesOf(Flowable5TestDataGenerator.class);
        for (Class<? extends Flowable5TestDataGenerator> generatorClass : generatorClasses) {
            Flowable5TestDataGenerator testDataGenerator = generatorClass.newInstance();
            testDataGenerator.generateTestData(processEngine);
            executedGenerators.add(testDataGenerator.getClass().getCanonicalName());
        }

        LOGGER.info("Test data generation completed.");
        for (String generatorClass : executedGenerators) {
            LOGGER.info("Executed test data generator {}", generatorClass);
        }
    }

}
