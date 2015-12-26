/*
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 * http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */
package org.jdbi.v3;

import java.math.BigDecimal;
import java.sql.PreparedStatement;
import java.sql.Types;
import java.util.HashMap;
import java.util.Map;

import org.easymock.EasyMockSupport;
import org.junit.After;
import org.junit.Test;

public class TestMapArguments extends EasyMockSupport
{
    private final PreparedStatement stmt = createMock(PreparedStatement.class);

    @After
    public void checkMock() {
        verifyAll();
    }

    @Test
    public void testBind() throws Exception
    {
        stmt.setBigDecimal(5, BigDecimal.ONE);
        replayAll();

        Map<String, Object> args = new HashMap<String, Object>();
        args.put("foo", BigDecimal.ONE);
        Foreman foreman = new Foreman();
        StatementContext ctx = new ConcreteStatementContext(new HashMap<String, Object>(), new MappingRegistry());
        MapArguments mapArguments = new MapArguments(foreman, ctx, args);
        mapArguments.find("foo").apply(5, stmt, null);
    }

    @Test
    public void testNullBinding() throws Exception
    {
        stmt.setNull(3, Types.NULL);
        replayAll();

        Map<String, Object> args = new HashMap<String, Object>();
        args.put("foo", null);
        Foreman foreman = new Foreman();
        StatementContext ctx = new ConcreteStatementContext(new HashMap<String, Object>(), new MappingRegistry());
        MapArguments mapArguments = new MapArguments(foreman, ctx, args);
        mapArguments.find("foo").apply(3, stmt, null);
    }
}
