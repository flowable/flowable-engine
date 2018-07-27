package com.dj.flowable.EventExtractors;

import java.util.Map;

public class AbstractExtractor {

	public AbstractExtractor() {
		super();
	}

	public void putSafe(Map<String, Object> map, String key, Object value) {
		if (value != null) {
			map.put(key, value);
		}
	}

}