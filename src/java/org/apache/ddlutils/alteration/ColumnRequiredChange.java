package org.apache.ddlutils.alteration;

/*
 * Licensed to the Apache Software Foundation (ASF) under one
 * or more contributor license agreements.  See the NOTICE file
 * distributed with this work for additional information
 * regarding copyright ownership.  The ASF licenses this file
 * to you under the Apache License, Version 2.0 (the
 * "License"); you may not use this file except in compliance
 * with the License.  You may obtain a copy of the License at
 *
 *   http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing,
 * software distributed under the License is distributed on an
 * "AS IS" BASIS, WITHOUT WARRANTIES OR CONDITIONS OF ANY
 * KIND, either express or implied.  See the License for the
 * specific language governing permissions and limitations
 * under the License.
 */

import org.apache.ddlutils.model.Column;
import org.apache.ddlutils.model.Database;
import org.apache.ddlutils.model.Table;

/**
 * Represents the change of the required constraint of a column. Since it is a boolean value,
 * this means the required constraint will simply be toggled.
 * 
 * @version $Revision: $
 */
public class ColumnRequiredChange extends ColumnChangeImplBase
{
    /**
     * Creates a new change object.
     * 
     * @param table  The table of the column
     * @param column The column
     */
    public ColumnRequiredChange(Table table, Column column)
    {
        super(table, column);
    }

    /**
     * {@inheritDoc}
     */
    public void apply(Database model, boolean caseSensitive)
    {
        findChangedColumn(model, caseSensitive).setRequired(!getChangedColumn().isRequired());
    }
}
