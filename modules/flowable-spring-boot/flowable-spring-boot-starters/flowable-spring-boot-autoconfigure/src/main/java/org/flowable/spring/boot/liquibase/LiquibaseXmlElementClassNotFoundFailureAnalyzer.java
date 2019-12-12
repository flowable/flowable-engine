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
package org.flowable.spring.boot.liquibase;

import org.springframework.boot.diagnostics.AbstractFailureAnalyzer;
import org.springframework.boot.diagnostics.FailureAnalysis;

/**
 * @author Filip Hrisafov
 */
public class LiquibaseXmlElementClassNotFoundFailureAnalyzer extends AbstractFailureAnalyzer<ClassNotFoundException> {

    @Override
    protected FailureAnalysis analyze(Throwable rootFailure, ClassNotFoundException cause) {
        String causeMessage = cause.getMessage();

        if (causeMessage != null && causeMessage.contains("javax.xml.bind.annotation.XmlElement")) {
            for (StackTraceElement stackTraceElement : cause.getStackTrace()) {
                if (stackTraceElement.getClassName().startsWith("liquibase.pro.packaged")) {
                    return new FailureAnalysis(
                        "Liquibase failed to initialize due to javax.xml.bin.annotation.XmlElement not being present."
                            + " Liquibase Versions starting from 3.8.1 have problems on Java 11."
                            + " See https://liquibase.jira.com/browse/CORE-3537 for more information.",
                        "Set the liquibase version to 3.8.0",
                        cause
                    );
                }
            }
        }

        return null;
    }
}
