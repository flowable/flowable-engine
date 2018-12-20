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
package org.activiti.engine.test.logging.mdc;

import java.io.StringWriter;

import org.apache.log4j.ConsoleAppender;

/**
 * @author Saeid Mirzaei
 */

public class MemoryLogAppender extends ConsoleAppender {

    StringWriter stringWriter = new StringWriter();

    public void activateOptions() {
        setWriter(stringWriter);
    }

    public String toString() {
        return stringWriter.toString();
    }

    public void clear() {
        stringWriter = new StringWriter();
    }

}
