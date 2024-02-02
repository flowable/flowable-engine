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

import static org.assertj.core.api.Assertions.assertThat;

import org.junit.jupiter.api.Test;
import org.springframework.boot.diagnostics.FailureAnalysis;
import org.springframework.boot.diagnostics.FailureAnalyzer;

/**
 * @author Filip Hrisafov
 */
class LiquibaseXmlElementClassNotFoundFailureAnalyzerTest {

    protected FailureAnalyzer underTest = new LiquibaseXmlElementClassNotFoundFailureAnalyzer();

    @Test
    void noAnalysisWhenClassNotFoundExceptionForNonXmlElementClass() {
        assertThat(underTest.analyze(new ClassNotFoundException("com.example.test.TestClass")))
            .isNull();
    }

    @Test
    void noAnalysisWhenStackTraceDoesNotContainLiquibaseProPackage() {
        ClassNotFoundException failure = new ClassNotFoundException("javax.xml.bind.annotation.XmlElement");
        StackTraceElement[] stackTrace = new StackTraceElement[] {
            new StackTraceElement("com.example.test.TestClass", "test()", null, -1),
            new StackTraceElement("com.example.test.TestClass", "testAgain()", null, -1)
        };
        failure.setStackTrace(stackTrace);

        assertThat(underTest.analyze(failure))
            .isNull();
    }

    @Test
    void analysisWhenStackTraceContainsLiquibaseProPackage() {
        ClassNotFoundException failure = new ClassNotFoundException("javax.xml.bind.annotation.XmlElement");
        StackTraceElement[] stackTrace = new StackTraceElement[] {
            new StackTraceElement("com.example.test.TestClass", "test()", null, -1),
            new StackTraceElement("com.example.test.TestClass", "testAgain()", null, -1),
            new StackTraceElement("liquibase.pro.packaged.k1", "k()", null, -1)
        };
        failure.setStackTrace(stackTrace);

        FailureAnalysis analysis = underTest.analyze(failure);
        assertThat(analysis).isNotNull();
        assertThat(analysis.getCause()).isSameAs(failure);
        assertThat(analysis.getAction()).isEqualTo("Set the liquibase version to 3.8.0");
        assertThat(analysis.getDescription()).isEqualTo("Liquibase failed to initialize due to javax.xml.bin.annotation.XmlElement not being present."
            + " Liquibase Versions starting from 3.8.1 have problems on Java 11."
            + " See https://liquibase.jira.com/browse/CORE-3537 for more information.");
    }
}
