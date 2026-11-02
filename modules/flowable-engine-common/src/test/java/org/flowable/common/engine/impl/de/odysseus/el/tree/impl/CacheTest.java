/*
 * Copyright 2006-2009 Odysseus Software GmbH
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *     http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */ 
package org.flowable.common.engine.impl.de.odysseus.el.tree.impl;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.fail;

import java.util.ArrayList;
import java.util.Collection;
import java.util.Random;
import java.util.concurrent.Callable;
import java.util.concurrent.ExecutionException;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.Future;
import java.util.concurrent.TimeUnit;

import org.flowable.common.engine.impl.de.odysseus.el.TestCase;
import org.flowable.common.engine.impl.de.odysseus.el.tree.Tree;
import org.junit.jupiter.api.Test;

class CacheTest extends TestCase {

	@Test
	void testSingleThread() {
		Cache cache = null;

		// check if caching works, cache size 1
		cache = new Cache(1);
		cache.put("1", parse("1"));
		assertThat(cache.get("1")).isNotNull();
		assertThat(cache.size()).isEqualTo(1);

		// check if removeEldest works, cache size 1
		cache = new Cache(1);
		cache.put("1", parse("1"));
		cache.put("2", parse("2"));
		assertThat(cache.get("1")).isNull();
		assertThat(cache.size()).isEqualTo(1);

		// check if removeEldest works, cache size 9
		cache = new Cache(9);
		for (int i = 1; i < 10; i++) {
			cache.put("" + i, parse("" + i));
			for (int j = 1; j <= i; j++) {
				assertThat(cache.get("" + j)).isNotNull();
			}
			assertThat(cache.size()).isEqualTo(i);
		}
		cache.put("10", parse("10"));
		assertThat(cache.get("1")).isNull();
		assertThat(cache.get("10")).isNotNull();
		assertThat(cache.size()).isEqualTo(9);
	}

	long testMultiThread(
			final int cacheSize,
			final int numberOfThreads,
			final int numberOfLookups) throws InterruptedException, ExecutionException {
		final Cache cache = new Cache(cacheSize, numberOfThreads);
		final Builder builder = new Builder();
		final Random random = new Random(7);
		ExecutorService service = Executors.newFixedThreadPool(numberOfThreads);
		Collection<Callable<Long>> tasks = new ArrayList<Callable<Long>>();
		for (int i = 0; i < numberOfThreads; i++) {
			tasks.add(new Callable<Long>() {
				public Long call() throws Exception {
					long time = System.currentTimeMillis();
					for (int j = 0; j < numberOfLookups; j++) {
						String expression = String.valueOf(Math.abs(random.nextInt()) % numberOfLookups);
						Tree tree = cache.get(expression);
						if (tree == null) {
							cache.put(expression, builder.build(expression));
						}
					}
					return System.currentTimeMillis() - time;
				}
			});
		}

		long result = 0;
		for (Future<Long> future : service.invokeAll(tasks, 10L, TimeUnit.SECONDS)) {
			if (!future.isDone() || future.isCancelled()) {
				fail();
			}
			result += future.get();
		}
		service.shutdown();
		return result;
	}

	@Test
	public void testMultiThread() throws Exception {
		testMultiThread(10000, 1, 100000);
		testMultiThread(1000, 10, 10000);
		testMultiThread(100, 100, 1000);
	}
}
