/*
 *    Copyright 2022 the original author or authors.
 *
 *    Licensed under the Apache License, Version 2.0 (the "License");
 *    you may not use this file except in compliance with the License.
 *    You may obtain a copy of the License at
 *
 *       https://www.apache.org/licenses/LICENSE-2.0
 *
 *    Unless required by applicable law or agreed to in writing, software
 *    distributed under the License is distributed on an "AS IS" BASIS,
 *    WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 *    See the License for the specific language governing permissions and
 *    limitations under the License.
 */
package org.flowable.spring.aot;

import java.lang.reflect.Method;
import java.lang.reflect.ParameterizedType;
import java.lang.reflect.Type;
import java.util.Map;
import java.util.Set;
import java.util.stream.Collectors;
import java.util.stream.Stream;

import org.apache.ibatis.reflection.TypeParameterResolver;

/**
 * @author Josh Long
 */
final class MybatisMapperTypeUtils {


	static Class<?> resolveReturnClass(Class<?> mapperInterface, Method method) {
		var resolvedReturnType = TypeParameterResolver.resolveReturnType(method, mapperInterface);
		return typeToClass(resolvedReturnType, method.getReturnType());
	}

	static Set<Class<?>> resolveParameterClasses(Class<?> mapperInterface, Method method) {
		return Stream.of(TypeParameterResolver.resolveParamTypes(method, mapperInterface))
			.map(x -> typeToClass(x, x instanceof Class ? (Class<?>) x : Object.class))
			.collect(Collectors.toSet());
	}

	private static Class<?> typeToClass(Type src, Class<?> fallback) {
		var result = (Class<?>) null;
		if (src instanceof Class<?> c) {
			result = c.isArray() ? c.getComponentType() : c;
		}
		else if (src instanceof ParameterizedType parameterizedType) {
			var index = (parameterizedType.getRawType() instanceof Class
					&& Map.class.isAssignableFrom((Class<?>) parameterizedType.getRawType())
					&& parameterizedType.getActualTypeArguments().length > 1) ? 1 : 0;
			var actualType = parameterizedType.getActualTypeArguments()[index];
			result = typeToClass(actualType, fallback);
		}
		if (result == null) {
			result = fallback;
		}
		return result;
	}

}
