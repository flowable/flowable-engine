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
package org.flowable.common.engine.impl;

import java.util.function.Consumer;

import org.flowable.common.engine.api.Engine;

/**
 * @author Filip Hrisafov
 */
public abstract class AbstractBuildableEngineConfiguration<E extends Engine> extends AbstractEngineConfiguration {

    protected boolean runPostEngineBuildConsumer = true;
    protected Consumer<E> postEngineBuildConsumer;

    public E buildEngine() {
        init();
        initPostEngineBuildConsumer();
        E engine = createEngine();
        if (runPostEngineBuildConsumer) {
            postEngineBuildConsumer.accept(engine);
        }
        return engine;
    }

    protected abstract E createEngine();

    protected abstract void init();

    protected void initPostEngineBuildConsumer() {
        if (this.postEngineBuildConsumer == null) {
            this.postEngineBuildConsumer = createPostEngineBuildConsumer();
        }
    }

    protected abstract Consumer<E> createPostEngineBuildConsumer();

    public boolean isRunPostEngineBuildConsumer() {
        return runPostEngineBuildConsumer;
    }

    public void setRunPostEngineBuildConsumer(boolean runPostEngineBuildConsumer) {
        this.runPostEngineBuildConsumer = runPostEngineBuildConsumer;
    }

    public Consumer<E> getPostEngineBuildConsumer() {
        return postEngineBuildConsumer;
    }

    public void setPostEngineBuildConsumer(Consumer<E> postEngineBuildConsumer) {
        this.postEngineBuildConsumer = postEngineBuildConsumer;
    }
}
