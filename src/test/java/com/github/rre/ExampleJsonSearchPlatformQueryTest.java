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

import io.sease.rre.search.api.QueryOrSearchResponse;
import org.apache.commons.io.IOUtils;
import org.junit.Before;
import org.junit.Rule;
import org.junit.Test;
import org.mockserver.client.MockServerClient;
import org.mockserver.junit.MockServerRule;
import org.mockserver.model.HttpRequest;
import org.mockserver.model.HttpResponse;
import org.mockserver.model.MediaType;

import java.nio.charset.Charset;
import java.util.HashMap;
import java.util.Map;

import static org.assertj.core.api.Assertions.assertThat;

/**
 * Unit tests for the search platform's execute query method.
 *
 * @author Matt Pearce <matt@elysiansoftware.co.uk>
 */
public class ExampleJsonSearchPlatformQueryTest {

	private final String BASE_PATH = "/searcher/search";
	private final String INDEX = "index";
	private final String VERSION = "1.0";
	private final String QUERY = "{ \"query\": \"fred\" }";
	private final String[] FIELDS = new String[0];
	private final int MAX_ROWS = 100;

	@Rule
	public MockServerRule mockServerRule = new MockServerRule(this);

	private MockServerClient mockServerClient;

	private ExampleJsonSearchPlatform platform;

	@Before
	public void setupPlatform() {
		platform = new ExampleJsonSearchPlatform();

		final String baseUrl = "http://localhost:" + mockServerRule.getPort() + BASE_PATH;

		platform.getBaseUrls().put(platform.getFullyQualifiedDomainName(INDEX, VERSION), baseUrl);
	}

	@Test
	public void queryReturnsEmptyResponseWhenIndexNull() {
		QueryOrSearchResponse queryResponse = platform.executeQuery(null, null, null, null, 0);

		assertThat(queryResponse.totalHits()).isEqualTo(0);
		assertThat(queryResponse.hits()).isEmpty();
	}

	@Test
	public void queryReturnsEmptyResponseWhenIndexUnrecognized() {
		QueryOrSearchResponse queryResponse = platform.executeQuery(INDEX + "a", VERSION, QUERY, FIELDS, MAX_ROWS);

		assertThat(queryResponse.totalHits()).isEqualTo(0);
		assertThat(queryResponse.hits()).isEmpty();
	}

	@Test
	public void queryReturnsEmptyResponseWhenRequestFails() {
		mockServerClient.when(HttpRequest.request().withPath(BASE_PATH))
				.respond(HttpResponse.response().withStatusCode(500));

		QueryOrSearchResponse queryResponse = platform.executeQuery(INDEX, VERSION, QUERY, FIELDS, MAX_ROWS);

		assertThat(queryResponse.totalHits()).isEqualTo(0);
		assertThat(queryResponse.hits()).isEmpty();
	}

	@Test
	public void queryReturnsResponseWhenRequestSucceeds() throws Exception {
		final String responseJson = IOUtils.toString(ExampleJsonSearchPlatformQueryTest.class.getResourceAsStream("search/search_response.json"), Charset.defaultCharset());
		mockServerClient.when(HttpRequest.request().withPath(BASE_PATH).withQueryStringParameter("query", "fred"))
				.respond(HttpResponse.response().withBody(responseJson, MediaType.JSON_UTF_8));

		QueryOrSearchResponse queryResponse = platform.executeQuery(INDEX, VERSION, QUERY, new String[]{"id", "name"}, MAX_ROWS);

		assertThat(queryResponse.totalHits()).isEqualTo(154);
		assertThat(queryResponse.hits()).isNotEmpty();

		// Verify the request format
		mockServerClient.verify(HttpRequest.request()
				.withQueryStringParameter(ExampleJsonSearchPlatform.FIELDS_PARAM, "id,name")
				.withQueryStringParameter(ExampleJsonSearchPlatform.PAGESIZE_PARAM, "" + MAX_ROWS));
	}
}
