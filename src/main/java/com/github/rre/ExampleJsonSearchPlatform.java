/*
 * Copyright (c) 2020 Elysian Software Limited.
 * <p/>
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 * <p/>
 * http://www.apache.org/licenses/LICENSE-2.0
 * <p/>
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */
package com.github.rre;

import com.fasterxml.jackson.annotation.JsonIgnoreProperties;
import com.fasterxml.jackson.annotation.JsonProperty;
import com.fasterxml.jackson.core.type.TypeReference;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.databind.type.TypeFactory;
import io.sease.rre.search.api.QueryOrSearchResponse;
import io.sease.rre.search.api.SearchPlatform;
import org.apache.commons.lang3.StringUtils;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.io.File;
import java.io.IOException;
import java.net.URL;
import java.net.URLEncoder;
import java.util.ArrayList;
import java.util.Collection;
import java.util.Collections;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

/**
 * @author Matt Pearce <matt@elysiansoftware.co.uk>
 */
public class ExampleJsonSearchPlatform implements SearchPlatform {

	private static final Logger LOGGER = LoggerFactory.getLogger(ExampleJsonSearchPlatform.class);

	private static final String NAME = "Example JSON API";
	private static final String BASEURL_KEY = "baseUrl";
	private static final String SETTINGS_FILE = "settings.json";

	static final String FIELDS_PARAM = "fields";
	static final String PAGESIZE_PARAM = "pageSize";

	private final Map<String, String> baseUrls = new HashMap<>();

	public void beforeStart(Map<String, Object> configuration) {

	}

	public void start() {

	}

	public void afterStart() {

	}

	public void load(File corpusFile, File settingsFile, String collection, String version) {
		// Corpus file is not used for this implementation
		try {
			Map<String, String> settingsMap = new ObjectMapper().readValue(settingsFile,
					TypeFactory.defaultInstance().constructMapType(HashMap.class, String.class, String.class));
			if (!settingsMap.containsKey(BASEURL_KEY)) {
				LOGGER.warn("Could not get base URL for search engine from {} - skipping configuration", settingsFile.getName());
			} else {
				baseUrls.put(getFullyQualifiedDomainName(collection, version), settingsMap.get(BASEURL_KEY));
			}
		} catch (IOException e) {
			LOGGER.error("Could not read settings from " + settingsFile.getName() + " :: " + e.getMessage());
		}
	}

	public void beforeStop() {

	}

	@Override
	public QueryOrSearchResponse executeQuery(String collection, String version, String query, String[] fields, int maxRows) {
		QueryOrSearchResponse searchResponse;

		// Look up the search settings
		final String baseUrl = baseUrls.get(getFullyQualifiedDomainName(collection, version));

		if (baseUrl == null) {
			LOGGER.error("No base URL found for index {} {}", collection, version);
			searchResponse = new QueryOrSearchResponse(0, Collections.emptyList());
		} else {
			try {
				// Build the URL query parameters
				Collection<String> urlQuery = new ArrayList<>();
				for (Map.Entry<String, String> qp : convertQueryToMap(query).entrySet()) {
					urlQuery.add(qp.getKey() + "=" + URLEncoder.encode(qp.getValue(), "UTF-8"));
				}
				// Add the fields to the query parameters
				urlQuery.add(FIELDS_PARAM + "=" + StringUtils.join(fields, ','));
				// Add the page size to the query parameters
				urlQuery.add(PAGESIZE_PARAM + "=" + maxRows);

				// Build the URL
				final URL queryUrl = new URL(baseUrl + "?" + StringUtils.join(urlQuery, "&"));

				// Make the request
				JsonSearchResponse jsonSearchResponse = new ObjectMapper().readValue(queryUrl, JsonSearchResponse.class);
				// Convert the response
				searchResponse = new QueryOrSearchResponse(jsonSearchResponse.getTotalResults(), jsonSearchResponse.getDocuments());
			} catch (IOException e) {
				LOGGER.error("Caught IOException making query :: {}", e.getMessage());
				searchResponse = new QueryOrSearchResponse(0, Collections.emptyList());
			}
		}

		return searchResponse;
	}

	private Map<String, String> convertQueryToMap(String query) {
		try {
			LOGGER.debug("Converting query:: {}", query);
			final ObjectMapper mapper = new ObjectMapper();
			return mapper.readValue(query, new TypeReference<HashMap<String, Object>>() {
			});
		} catch (IOException e) {
			LOGGER.error("Cannot convert incoming query string to Map! {}", e.getMessage());
			return Collections.emptyMap();
		}
	}

	public String getName() {
		return NAME;
	}

	public boolean isRefreshRequired() {
		return false;
	}

	public boolean isSearchPlatformConfiguration(String indexName, File searchEngineStartupSettings) {
		return searchEngineStartupSettings.isFile() && searchEngineStartupSettings.getName().equals(SETTINGS_FILE);
	}

	public boolean isCorporaRequired() {
		return false;
	}

	public void close() {

	}

	@JsonIgnoreProperties(ignoreUnknown = true)
	public static class JsonSearchResponse {
		private final long totalResults;
		private final List<Map<String, Object>> documents;

		public JsonSearchResponse(@JsonProperty("totalResults") long totalResults,
								  @JsonProperty("documents") List<Map<String, Object>> documents) {
			this.totalResults = totalResults;
			this.documents = documents;
		}

		public long getTotalResults() {
			return totalResults;
		}

		public List<Map<String, Object>> getDocuments() {
			return documents;
		}
	}

	// Test method
	Map<String, String> getBaseUrls() {
		return baseUrls;
	}
}
