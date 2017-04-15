/**
 * Copyright 2017 Lending Club, Inc.
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
package org.lendingclub.mercator.docker;

import org.junit.Test;
import org.lendingclub.mercator.core.BasicProjector;
import org.lendingclub.mercator.core.Projector;
import org.macgyver.mercator.docker.DockerScanner;
import org.macgyver.mercator.docker.DockerScannerBuilder;

public class DockerTest {

	@Test
	public void testLocalDockerDaemon() {

		try {
			Projector p = new Projector.Builder().build();
			DockerScanner ds = p.createBuilder(DockerScannerBuilder.class).build();
			
			ds.getSchemaManager().applyConstraints();
			ds.scan();

		} catch (Exception e) {

			e.printStackTrace();
		}

	}
}
