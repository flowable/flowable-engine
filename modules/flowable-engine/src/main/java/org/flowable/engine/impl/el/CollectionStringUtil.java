package org.flowable.engine.impl.el;

import java.util.ArrayList;
import java.util.Collection;

import org.apache.commons.lang3.StringEscapeUtils;
import org.flowable.engine.impl.util.json.JSONArray;
import org.flowable.engine.impl.util.json.JSONObject;

public class CollectionStringUtil {

    public static Collection<String> parseJson(Object collectionString) {
    	JSONArray jsonArray = new JSONArray(StringEscapeUtils.unescapeJson((String) collectionString));
    	ArrayList<String> collection = new ArrayList<String>();
    	JSONObject jsonObj = null;

    	for (int i=0; i < jsonArray.length(); i++) {
    		jsonObj = jsonArray.getJSONObject(i);
    		collection.add(jsonObj.getString("principal"));
    	}

		return collection;
	}
}
