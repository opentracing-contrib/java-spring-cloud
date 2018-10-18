/**
 * Copyright 2017-2018 The OpenTracing Authors
 *
 * Licensed under the Apache License, Version 2.0 (the "License"); you may not use this file except
 * in compliance with the License. You may obtain a copy of the License at
 *
 * http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software distributed under the License
 * is distributed on an "AS IS" BASIS, WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express
 * or implied. See the License for the specific language governing permissions and limitations under
 * the License.
 */

package io.opentracing.contrib.spring.cloud.jdbc.jdbc;

import io.opentracing.contrib.spring.cloud.jdbc.MockTracingConfiguration;
import org.junit.runner.RunWith;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.test.context.TestPropertySource;
import org.springframework.test.context.junit4.SpringJUnit4ClassRunner;

/**
 * Test behaviour when withActiveSpanOnly is set
 * @author Will Penington
 */
@SpringBootTest(classes = {MockTracingConfiguration.class})
@RunWith(SpringJUnit4ClassRunner.class)
@TestPropertySource(properties = {
        "opentracing.spring.cloud.jdbc.withActiveSpanOnly=true"
})
public class JdbcOnlyWithActiveDefaultTest {}
