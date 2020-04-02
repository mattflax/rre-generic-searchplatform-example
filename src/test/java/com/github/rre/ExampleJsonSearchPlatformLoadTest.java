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

import org.junit.Before;
import org.junit.Test;

import java.io.File;

import static org.assertj.core.api.Assertions.assertThat;

/**
 * Unit tests for the search platform's load() method.
 *
 * @author Matt Pearce <matt@elysiansoftware.co.uk>
 */
public class ExampleJsonSearchPlatformLoadTest {

	private ExampleJsonSearchPlatform platform;

	@Before
	public void setupPlatform() {
		this.platform = new ExampleJsonSearchPlatform();
	}

	@Test
	public void loadDoesNotCrashWithBadlyFormattedSettings() throws Exception {
		final File settingsFile = new File(ExampleJsonSearchPlatformLoadTest.class.getResource("./bad_format.json").toURI());
		final String index = "test";
		final String version = "1.0";

		platform.load(null, settingsFile, index, version);

		assertThat(platform.getBaseUrls()).isEmpty();
	}

	@Test
	public void loadReadsWellFormattedSettings() throws Exception {
		final File settingsFile = new File(ExampleJsonSearchPlatformLoadTest.class.getResource("./search_settings.json").toURI());
		final String index = "test";
		final String version = "1.0";

		platform.load(null, settingsFile, index, version);

		assertThat(platform.getBaseUrls()).containsOnlyKeys(platform.getFullyQualifiedDomainName(index, version));
	}

}
