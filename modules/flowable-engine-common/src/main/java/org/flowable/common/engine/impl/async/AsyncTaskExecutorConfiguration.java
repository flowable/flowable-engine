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
package org.flowable.common.engine.impl.async;

import java.time.Duration;

/**
 * @author Filip Hrisafov
 */
public class AsyncTaskExecutorConfiguration {

    /**
     * The minimal number of threads that are kept alive in the thread pool for
     * job execution
     */
    protected int corePoolSize = 8;

    /**
     * The maximum number of threads that are kept alive in the thread pool for
     * job execution
     */
    protected int maxPoolSize = 8;

    /**
     * The time a thread used for job execution must be kept
     * alive before it is destroyed. Default setting is 5 seconds. Having a non-default
     * setting of 0 takes resources, but in the case of many job executions it
     * avoids creating new threads all the time.
     */
    protected Duration keepAlive = Duration.ofSeconds(5);

    /**
     * The size of the queue on which jobs to be executed are placed
     */
    protected int queueSize = 2048;

    /**
     * Whether core threads can time out (which is needed to scale down the threads)
     */
    protected boolean allowCoreThreadTimeout = true;

    /**
     * The time that is waited to gracefully shut down the
     * thread pool used for job execution
     */
    protected Duration awaitTerminationPeriod = Duration.ofSeconds(60);

    /**
     * The naming pattern for the thread pool threads.
     */
    protected String threadPoolNamingPattern;

    public int getCorePoolSize() {
        return corePoolSize;
    }

    public void setCorePoolSize(int corePoolSize) {
        this.corePoolSize = corePoolSize;
    }

    public int getMaxPoolSize() {
        return maxPoolSize;
    }

    public void setMaxPoolSize(int maxPoolSize) {
        this.maxPoolSize = maxPoolSize;
    }

    public Duration getKeepAlive() {
        return keepAlive;
    }

    public void setKeepAlive(Duration keepAlive) {
        this.keepAlive = keepAlive;
    }

    public int getQueueSize() {
        return queueSize;
    }

    public void setQueueSize(int queueSize) {
        this.queueSize = queueSize;
    }

    public boolean isAllowCoreThreadTimeout() {
        return allowCoreThreadTimeout;
    }

    public void setAllowCoreThreadTimeout(boolean allowCoreThreadTimeout) {
        this.allowCoreThreadTimeout = allowCoreThreadTimeout;
    }

    public Duration getAwaitTerminationPeriod() {
        return awaitTerminationPeriod;
    }

    public void setAwaitTerminationPeriod(Duration awaitTerminationPeriod) {
        this.awaitTerminationPeriod = awaitTerminationPeriod;
    }

    public String getThreadPoolNamingPattern() {
        return threadPoolNamingPattern;
    }

    public void setThreadPoolNamingPattern(String threadPoolNamingPattern) {
        this.threadPoolNamingPattern = threadPoolNamingPattern;
    }

    public void setThreadNamePrefix(String prefix) {
        if (prefix == null) {
            this.threadPoolNamingPattern = "%d";
        } else {
            this.threadPoolNamingPattern = prefix + "%d";
        }
    }
}
