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
package org.flowable.spring.aot;

import java.io.IOException;
import java.io.Serializable;
import java.util.Collection;
import java.util.HashSet;
import java.util.Set;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.context.annotation.ClassPathScanningCandidateComponentProvider;
import org.springframework.core.io.ClassPathResource;
import org.springframework.core.io.Resource;
import org.springframework.core.type.filter.AssignableTypeFilter;

/**
 * @author Josh Long
 */
final class FlowableSpringAotUtils {

	private static final Logger log = LoggerFactory.getLogger(FlowableSpringAotUtils.class);

	private FlowableSpringAotUtils() {
	}

	static Resource newResourceFor(Resource in) {
		try {
			var marker = "jar!";
			var p = in.getURL().toExternalForm();
			var rest = p.substring(p.lastIndexOf(marker) + marker.length());
			return new ClassPathResource(rest);
		}
		catch (IOException e) {
			throw new RuntimeException(e);
		}
	}

	static boolean isSerializable(Class<?> clazz) {
		return Serializable.class.isAssignableFrom(clazz);
	}

	static <T> void debug(String message, Collection<T> tCollection) {
		log.debug(message + System.lineSeparator());
		for (var t : tCollection)
			log.debug('\t' + t.toString());
		log.debug(System.lineSeparator());
	}

	static String packageToPath(String packageName) {
		var sb = new StringBuilder();
		for (var c : packageName.toCharArray())
			sb.append(c == '.' ? '/' : c);
		return sb.toString();
	}


	static Set<String> getSubTypesOf(Class<?> clzzName, String... packages) {
		var set = new HashSet<String>();

		for (var p : packages) {
			var classPathScanningCandidateComponentProvider = new ClassPathScanningCandidateComponentProvider(false);

			classPathScanningCandidateComponentProvider.addIncludeFilter(new AssignableTypeFilter(clzzName));

			var results = classPathScanningCandidateComponentProvider.findCandidateComponents(p);
			for (var r : results)
				set.add(r.getBeanClassName());
		}

		return set;

	}
}
