package org.apache.ddlutils.platform.mckoi;

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

import java.io.IOException;

import org.apache.ddlutils.PlatformInfo;
import org.apache.ddlutils.model.Column;
import org.apache.ddlutils.model.Table;
import org.apache.ddlutils.platform.SqlBuilder;

/**
 * The SQL Builder for the Mckoi database.
 * 
 * @author James Strachan
 * @author Thomas Dudziak
 * @version $Revision$
 */
public class MckoiBuilder extends SqlBuilder
{
    /**
     * Creates a new builder instance.
     * 
     * @param info The platform info
     */
    public MckoiBuilder(PlatformInfo info)
    {
        super(info);
    }

    /**
     * {@inheritDoc}
     */
    public void dropTable(Table table) throws IOException
    { 
        print("DROP TABLE IF EXISTS ");
        printIdentifier(getTableName(table));
        printEndOfStatement();
    }

    /**
     * {@inheritDoc}
     */
    protected void writeColumnDefaultValue(Table table, Column column) throws IOException
    {
        if (column.isAutoIncrement())
        {
            // we start at value 1 to avoid issues with jdbc
            print("UNIQUEKEY('");
            print(getTableName(table));
            print("') + 1");
        }
        else
        {
            super.writeColumnDefaultValue(table, column);
        }
    }
}
