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
package org.flowable.common.engine.impl.interceptor;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.times;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import java.sql.SQLException;
import java.util.Collection;
import java.util.concurrent.atomic.AtomicInteger;

import org.flowable.common.engine.impl.AbstractEngineConfiguration;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

/**
 * Tests for {@link SpannerRetryInterceptor} and Spanner interceptor chain integration.
 */
class SpannerRetryInterceptorTest {

    private SpannerRetryInterceptor retryInterceptor;
    private CommandInterceptor nextInterceptor;
    private CommandConfig commandConfig;
    private Command<String> command;
    private CommandExecutor commandExecutor;

    @BeforeEach
    @SuppressWarnings("unchecked")
    void setUp() {
        retryInterceptor = new SpannerRetryInterceptor();
        // Use minimal wait time for fast test execution
        retryInterceptor.setWaitTime(1);
        retryInterceptor.setWaitTimeIncrease(2);
        retryInterceptor.setNrRetries(3);

        nextInterceptor = mock(CommandInterceptor.class);
        retryInterceptor.setNext(nextInterceptor);

        commandConfig = new CommandConfig();
        command = mock(Command.class);
        commandExecutor = mock(CommandExecutor.class);
    }

    @Test
    void testSuccessfulExecutionWithoutRetry() {
        when(nextInterceptor.execute(commandConfig, command, commandExecutor)).thenReturn("success");

        String result = retryInterceptor.execute(commandConfig, command, commandExecutor);

        assertThat(result).isEqualTo("success");
        verify(nextInterceptor, times(1)).execute(commandConfig, command, commandExecutor);
    }

    @Test
    void testRetryOnSpannerAbortedExceptionAndEventualSuccess() {
        SQLException spannerAborted = new SQLException("Transaction was aborted", "00000", 10);
        when(nextInterceptor.execute(commandConfig, command, commandExecutor))
                .thenThrow(new RuntimeException(spannerAborted))
                .thenThrow(new RuntimeException(spannerAborted))
                .thenReturn("eventual-success");

        String result = retryInterceptor.execute(commandConfig, command, commandExecutor);

        assertThat(result).isEqualTo("eventual-success");
        verify(nextInterceptor, times(3)).execute(commandConfig, command, commandExecutor);
    }

    @Test
    void testRetryExhaustionThrowsException() {
        SQLException spannerAborted = new SQLException("ABORTED: Transaction was aborted by Spanner", null, 10);
        RuntimeException failure = new RuntimeException(spannerAborted);
        when(nextInterceptor.execute(commandConfig, command, commandExecutor)).thenThrow(failure);

        assertThatThrownBy(() -> retryInterceptor.execute(commandConfig, command, commandExecutor))
                .isSameAs(failure);

        // 1 initial attempt + 3 retries = 4 attempts total
        verify(nextInterceptor, times(4)).execute(commandConfig, command, commandExecutor);
    }

    @Test
    void testImmediateRethrowOnNonRetryableException() {
        SQLException fatalException = new SQLException("Unique constraint violation", "23505", 23505);
        RuntimeException failure = new RuntimeException(fatalException);
        when(nextInterceptor.execute(commandConfig, command, commandExecutor)).thenThrow(failure);

        assertThatThrownBy(() -> retryInterceptor.execute(commandConfig, command, commandExecutor))
                .isSameAs(failure);

        verify(nextInterceptor, times(1)).execute(commandConfig, command, commandExecutor);
    }

    @Test
    void testIsTransactionRetryExceptionWithGrpcCode10() {
        SQLException sqlException = new SQLException("Spanner transaction error", null, 10);
        assertThat(retryInterceptor.isTransactionRetryException(sqlException)).isTrue();
    }

    @Test
    void testIsTransactionRetryExceptionWithAbortedMessage() {
        Exception exception = new RuntimeException("ABORTED: commit conflict detected");
        assertThat(retryInterceptor.isTransactionRetryException(exception)).isTrue();
    }

    @Test
    void testIsTransactionRetryExceptionWithAbortedExceptionClassName() {
        Exception exception = new AbortedException("Aborted by Spanner server");
        assertThat(retryInterceptor.isTransactionRetryException(exception)).isTrue();
    }

    @Test
    void testIsTransactionRetryExceptionWithAbortedDueToConcurrentModificationExceptionClassName() {
        Exception exception = new AbortedDueToConcurrentModificationException("Concurrent modification detected");
        assertThat(retryInterceptor.isTransactionRetryException(exception)).isTrue();
    }

    @Test
    void testIsTransactionRetryExceptionWithNestedCauseChain() {
        SQLException rootSqlException = new SQLException("Aborted", null, 10);
        Exception middle = new IllegalStateException("Middle wrapper", rootSqlException);
        Exception outer = new RuntimeException("Outer wrapper", middle);

        assertThat(retryInterceptor.isTransactionRetryException(outer)).isTrue();
    }

    @Test
    void testIsTransactionRetryExceptionWithSqlNextException() {
        SQLException rootSqlException = new SQLException("Primary error", "00000", 0);
        SQLException nextSqlException = new SQLException("Next error", null, 10);
        rootSqlException.setNextException(nextSqlException);

        assertThat(retryInterceptor.isTransactionRetryException(rootSqlException)).isTrue();
    }

    @Test
    void testIsTransactionRetryExceptionReturnsFalseForNonRetryable() {
        assertThat(retryInterceptor.isTransactionRetryException(null)).isFalse();
        assertThat(retryInterceptor.isTransactionRetryException(new IllegalArgumentException("Invalid argument"))).isFalse();
        assertThat(retryInterceptor.isTransactionRetryException(new SQLException("Generic error", "HY000", 999))).isFalse();
    }

    @Test
    void testGettersAndSetters() {
        retryInterceptor.setNrRetries(5);
        assertThat(retryInterceptor.getNrRetries()).isEqualTo(5);

        retryInterceptor.setWaitTime(100);
        assertThat(retryInterceptor.getWaitTime()).isEqualTo(100);

        retryInterceptor.setWaitTimeIncrease(3);
        assertThat(retryInterceptor.getWaitTimeIncrease()).isEqualTo(3);
    }

    @Test
    void testDefaultValues() {
        SpannerRetryInterceptor defaultInterceptor = new SpannerRetryInterceptor();
        assertThat(defaultInterceptor.getNrRetries()).isEqualTo(3);
        assertThat(defaultInterceptor.getWaitTime()).isEqualTo(50);
        assertThat(defaultInterceptor.getWaitTimeIncrease()).isEqualTo(2);
    }

    @Test
    void testSpannerRetryInterceptorInEngineConfigurationChain() {
        TestEngineConfiguration spannerConfig = new TestEngineConfiguration();
        spannerConfig.setDatabaseType(AbstractEngineConfiguration.DATABASE_TYPE_SPANNER);

        Collection<? extends CommandInterceptor> interceptors = spannerConfig.getDefaultCommandInterceptors();
        boolean hasSpannerRetry = interceptors.stream().anyMatch(SpannerRetryInterceptor.class::isInstance);
        assertThat(hasSpannerRetry).as("SpannerRetryInterceptor must be present when databaseType is spanner").isTrue();

        TestEngineConfiguration h2Config = new TestEngineConfiguration();
        h2Config.setDatabaseType(AbstractEngineConfiguration.DATABASE_TYPE_H2);

        Collection<? extends CommandInterceptor> h2Interceptors = h2Config.getDefaultCommandInterceptors();
        boolean hasSpannerRetryInH2 = h2Interceptors.stream().anyMatch(SpannerRetryInterceptor.class::isInstance);
        assertThat(hasSpannerRetryInH2).as("SpannerRetryInterceptor must not be present when databaseType is h2").isFalse();
    }

    // Dummy exception classes to simulate Spanner client library exception class names
    private static class AbortedException extends RuntimeException {
        public AbortedException(String message) {
            super(message);
        }
    }

    private static class AbortedDueToConcurrentModificationException extends RuntimeException {
        public AbortedDueToConcurrentModificationException(String message) {
            super(message);
        }
    }

    private static class TestEngineConfiguration extends AbstractEngineConfiguration {

        @Override
        public String getEngineName() {
            return "testEngine";
        }

        @Override
        public String getEngineCfgKey() {
            return "testEngine";
        }

        @Override
        public String getEngineScopeType() {
            return "test";
        }

        @Override
        public CommandInterceptor createTransactionInterceptor() {
            return null;
        }

        @Override
        protected org.flowable.common.engine.impl.db.SchemaManager createEngineSchemaManager() {
            return null;
        }

        @Override
        protected void initDbSqlSessionFactoryEntitySettings() {
        }

        @Override
        public java.io.InputStream getMyBatisXmlConfigurationStream() {
            return null;
        }
    }
}
