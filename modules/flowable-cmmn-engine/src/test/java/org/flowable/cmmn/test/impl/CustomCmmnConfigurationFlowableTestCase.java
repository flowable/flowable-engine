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
package org.flowable.cmmn.test.impl;

import org.flowable.cmmn.engine.CmmnEngine;
import org.flowable.cmmn.test.AbstractFlowableCmmnTestCase;
import org.flowable.cmmn.test.ResourceFlowableCmmnExtension;
import org.junit.jupiter.api.Tag;
import org.junit.jupiter.api.extension.ExtendWith;

/**
 * Reads the default flowable.cmmn.cfg.xml from the test classpath, but allows to change the settings before the engine is built.
 * This allows to run with the same settings as the regular test runs, but tweak the config slightly.
 *
 * Note that a {@link CmmnEngine} is booted up and shut down for every test class,
 * so use with caution to avoid that total test times go up. 
 * 
 * @author Joram Barrez
 */
@Tag("resource")
@ExtendWith(ResourceFlowableCmmnExtension.class)
public abstract class CustomCmmnConfigurationFlowableTestCase extends AbstractFlowableCmmnTestCase {

}
