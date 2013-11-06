package org.apache.ddlutils.io;

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

import java.io.FileOutputStream;
import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;
import java.io.Reader;
import java.io.Writer;
import java.sql.Connection;
import java.sql.SQLException;
import java.util.ArrayList;
import java.util.HashSet;
import java.util.Iterator;
import java.util.List;
import java.util.Map;
import java.util.Set;

import org.apache.commons.beanutils.DynaBean;

import org.apache.commons.collections.map.ListOrderedMap;
import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;
import org.apache.ddlutils.DdlUtilsException;
import org.apache.ddlutils.Platform;
import org.apache.ddlutils.model.Column;
import org.apache.ddlutils.model.Database;
import org.apache.ddlutils.model.Table;
import org.apache.ddlutils.dynabean.SqlDynaBean;

/**
 * Provides basic live database data <-> XML functionality.
 * 
 * @version $Revision: $
 */
public class DatabaseDataDiffIO extends DatabaseDataIO
{
	private final Log _log = LogFactory.getLog(DatabaseDataDiffIO.class);
	
	/**
     * Compares the keys for data in ths database to whats in the readers xml files.
	 * if the keys don't exist in the xml files then it writes those records out into 1 file for each table
     *  
     * @param platform The platform; needs to be connected to a live database
     * @param model    The model for which to retrieve and write the data
     * @param writer   The data writer
	 * @param reader   The data reader
     */
    public void writeDiffDataToXML(Platform platform, Database model, String outPath, InputStream[] inputs)
    {

		//read in the keys from a reader
		DataReaderKeys keyreader = new DataReaderKeys();
		keyreader.setModel(model);
		Map keymap = getKeyMap(keyreader,inputs);

		Table[] tables = model.getTables();
		for (int idx = 0; idx < tables.length; idx++)
	    {
	        Table table = tables[idx];
            Iterator beans = getDataForTable(platform, model, table);
			Set keys = (Set)keymap.get(table.getName());
			writeNewRows(beans,table,keys,outPath);
        }
        //writer.writeDocumentEnd();
    }

	/**
     * writes all data out. one file for each table.
     *  
     * @param platform The platform; needs to be connected to a live database
     * @param model    The model for which to retrieve and write the data
     * @param writer   The data writer
	 * @param reader   The data reader
     */
    public void writeDataToXML(Platform platform, Database model, List tableList, String outPath)
    {
		
		Table[] tables = model.getTables();
		for (int idx = 0; idx < tables.length; idx++)
	    {
	        Table table = tables[idx];
			if(tableList.contains("*") || tableList.contains(table.getName())){
	            Iterator beans = getDataForTable(platform, model, table);
				writeNewRows(beans,table,null,outPath);
			}
        }
    }
	

	public Map getKeyMap(DataReaderKeys keyreader, InputStream[] inputs) throws DdlUtilsException{
		for (int idx = 0; (inputs != null) && (idx < inputs.length); idx++)
        {
            keyreader.read(inputs[idx]);
        }
		return keyreader.keymap;
	}
	
	/**
     * Writes the beans contained in the given iterator.
     * 
     * @param beans The beans iterator
     */
    public void writeNewRows(Iterator beans, Table table, Set keys, String outPath) throws DataWriterException
    {
		DataWriter writer = null;
		
        while (beans.hasNext())
        {
			
            DynaBean bean = (DynaBean)beans.next();

            if (bean instanceof SqlDynaBean)
            {
				Identity id = buildIdentityFromPKs(table, bean);
				if(keys==null || !keys.contains(id)){
					if (writer==null){
						writer = getConfiguredDataWriter(outPath+table.getName()+".xml", null);
						writer.writeDocumentStart();
					}
                	writer.write((SqlDynaBean)bean);
					
				}
            }
            else
            {
                _log.warn("Cannot write normal dyna beans (type: "+bean.getDynaClass().getName()+")");
            }
        }
		if (writer!=null) writer.writeDocumentEnd();
    }
	
	/**
     * gets the iterator for the tablerows
     * 
     * @param platform The platform
     * @param model    The database model
     * @param table    The table 
     */
    private Iterator getDataForTable(Platform platform, Database model, Table table)
    {
        Table[]      tables = { table };
        StringBuffer query  = new StringBuffer();

        query.append("SELECT ");

        Connection connection = null;
        String     schema     = null;

        Column[] columns = tables[0].getColumns();

        for (int columnIdx = 0; columnIdx < columns.length; columnIdx++)
        {
            if (columnIdx > 0)
            {
                query.append(",");
            }
            if (platform.isDelimitedIdentifierModeOn())
            {
                query.append(platform.getPlatformInfo().getDelimiterToken());
            }
            query.append(columns[columnIdx].getName());
            if (platform.isDelimitedIdentifierModeOn())
            {
                query.append(platform.getPlatformInfo().getDelimiterToken());
            }
        }
        query.append(" FROM ");
        if (platform.isDelimitedIdentifierModeOn())
        {
            query.append(platform.getPlatformInfo().getDelimiterToken());
        }
        if (schema != null)
        {
            query.append(schema);
            query.append(".");
        }
        query.append(tables[0].getName());
        if (platform.isDelimitedIdentifierModeOn())
        {
            query.append(platform.getPlatformInfo().getDelimiterToken());
        }

        return platform.query(model, query.toString(), tables);
    }

	private Identity buildIdentityFromPKs(Table table, DynaBean bean)
    {
        Identity identity  = new Identity(table);
        Column[] pkColumns = table.getPrimaryKeyColumns();

        for (int idx = 0; idx < pkColumns.length; idx++)
        {
            identity.setColumnValue(pkColumns[idx].getName(), bean.get(pkColumns[idx].getName()));
        }
        return identity;
    }
}	