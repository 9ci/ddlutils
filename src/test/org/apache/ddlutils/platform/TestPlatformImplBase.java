package org.apache.ddlutils.platform;

/*
 * Copyright 1999-2005 The Apache Software Foundation.
 * 
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 * 
 *      http://www.apache.org/licenses/LICENSE-2.0
 * 
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */

import java.util.Map;

import org.apache.commons.beanutils.DynaBean;
import org.apache.ddlutils.TestPlatformBase;
import org.apache.ddlutils.dynabean.SqlDynaBean;
import org.apache.ddlutils.dynabean.SqlDynaClass;
import org.apache.ddlutils.model.Database;
import org.apache.ddlutils.model.Table;

/**
 * Tests the {@link org.apache.ddlutils.PlatformImplBase} (abstract) class.
 * 
 * @author Martin van den Bemt
 * @version $Revision: 279421 $
 */
public class TestPlatformImplBase extends TestPlatformBase 
{

    String xml = "<?xml version='1.0' encoding='ISO-8859-1'?>\n"+
                 "<database name='ddlutils'>\n"+
                 "  <table name='TestTable'>\n"+
                 "    <column name='id' autoIncrement='true' type='INTEGER' primaryKey='true'/>\n"+
                 "    <column name='name' type='VARCHAR' size='15'/>\n"+
                 "  </table>\n"+
                 "</database>";

    public void setUp()
    {
    }

    /**
     * Test the toColumnValues method
     */
    public void testToColumnValues()
    {
        Database database = parseDatabaseFromString(xml);
        PlatformImplBase platform = new PlatformBase();
        Table table = database.getTable(0);
        SqlDynaClass clz = SqlDynaClass.newInstance(table);
        DynaBean db = new SqlDynaBean(SqlDynaClass.newInstance(table));
        db.set("name", "name");
        Map map = platform.toColumnValues(clz.getSqlDynaProperties(), db);

        assertEquals("name", map.get("name"));
        assertEquals(true, map.containsKey("id"));
    }
    
 
    public class PlatformBase extends PlatformImplBase 
    {
        public String getName() 
        {
            return null;
        }
    }


    protected String getDatabaseName() {
        return null;
    }

    
}
