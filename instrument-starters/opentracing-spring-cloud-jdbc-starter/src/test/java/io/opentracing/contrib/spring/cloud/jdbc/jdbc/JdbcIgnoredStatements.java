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

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertTrue;

import io.opentracing.contrib.spring.cloud.jdbc.MockTracingConfiguration;
import io.opentracing.mock.MockTracer;
import java.sql.PreparedStatement;
import java.sql.SQLException;
import javax.sql.DataSource;
import org.junit.Before;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.test.context.TestPropertySource;
import org.springframework.test.context.junit4.SpringJUnit4ClassRunner;

/**
 * Test behaviour when ignored statements are configured
 * @author Will Penington
 */
@SpringBootTest(classes = {MockTracingConfiguration.class})
@RunWith(SpringJUnit4ClassRunner.class)
@TestPropertySource(properties = {
        "opentracing.spring.cloud.jdbc.ignoreStatements=SELECT 1"
})
public class JdbcIgnoredStatements {

  @Autowired
  MockTracer tracer;

  @Autowired
  DataSource dataSource;

  @Autowired
  JdbcTemplate jdbcTemplate;

  @Before
  public void before() {
    tracer.reset();
  }

  /**
   * Make sure ignored statements aren't traced
   */
  @Test
  public void spanIsNotCreatedForIgnoredStatement() throws SQLException {
    PreparedStatement pstmt = dataSource.getConnection().prepareStatement("SELECT 1");
    assertTrue(pstmt.execute());
    assertEquals(0, tracer.finishedSpans().size());
  }

  /**
   * Make sure statements that aren't ignored aren't affected
   */
  @Test
  public void spanIsCreatedNonIgnoredStatement() throws SQLException {
    PreparedStatement pstmt = dataSource.getConnection().prepareStatement("SELECT 2");
    assertTrue(pstmt.execute());
    assertEquals(1, tracer.finishedSpans().size());
  }
}
