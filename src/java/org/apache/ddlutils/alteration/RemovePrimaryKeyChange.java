package org.apache.ddlutils.alteration;

/*
 * Copyright 2006 The Apache Software Foundation.
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

import org.apache.ddlutils.model.Column;
import org.apache.ddlutils.model.Database;
import org.apache.ddlutils.model.Table;

/**
 * Represents the removal of the primary key from a table.
 * 
 * @version $Revision: $
 */
public class RemovePrimaryKeyChange extends TableChangeImplBase
{
    /** The columns making up the primary key. */
    private Column[] _primaryKeyColumns;

    /**
     * Creates a new change object.
     * 
     * @param table             The table to remove the primary key from
     * @param primaryKeyColumns The columns making up the primary key
     */
    public RemovePrimaryKeyChange(Table table, Column[] primaryKeyColumns)
    {
        super(table);
        _primaryKeyColumns = primaryKeyColumns;
    }

    /**
     * Returns the primary key columns making up the primary key.
     *
     * @return The primary key columns
     */
    public Column[] getPrimaryKeyColumns()
    {
        return _primaryKeyColumns;
    }

    /**
     * {@inheritDoc}
     */
    public void apply(Database database)
    {
        Table table = database.findTable(getChangedTable().getName());

        for (int idx = 0; idx < _primaryKeyColumns.length; idx++)
        {
            Column column = table.findColumn(_primaryKeyColumns[idx].getName());

            column.setPrimaryKey(false);
        }
    }
}