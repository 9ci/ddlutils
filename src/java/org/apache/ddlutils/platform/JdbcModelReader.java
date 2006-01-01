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

import java.sql.Connection;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.sql.Types;
import java.util.ArrayList;
import java.util.Collection;
import java.util.HashMap;
import java.util.Iterator;
import java.util.List;
import java.util.Map;

import org.apache.commons.collections.map.ListOrderedMap;
import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;
import org.apache.ddlutils.PlatformInfo;
import org.apache.ddlutils.model.Column;
import org.apache.ddlutils.model.Database;
import org.apache.ddlutils.model.ForeignKey;
import org.apache.ddlutils.model.Index;
import org.apache.ddlutils.model.IndexColumn;
import org.apache.ddlutils.model.NonUniqueIndex;
import org.apache.ddlutils.model.Reference;
import org.apache.ddlutils.model.Table;
import org.apache.ddlutils.model.UniqueIndex;

/**
 * An utility class to create a Database model from a live database.
 *
 * @author J. Russell Smyth
 * @author Thomas Dudziak
 * @version $Revision$
 */
public class JdbcModelReader
{
    /** The Log to which logging calls will be made. */
    private final Log _log = LogFactory.getLog(JdbcModelReader.class);

    /** The descriptors for the relevant columns in the table meta data. */
    private final List _columnsForTable;
    /** The descriptors for the relevant columns in the table column meta data. */
    private final List _columnsForColumn;
    /** The descriptors for the relevant columns in the primary key meta data. */
    private final List _columnsForPK;
    /** The descriptors for the relevant columns in the foreign key meta data. */
    private final List _columnsForFK;
    /** The descriptors for the relevant columns in the index meta data. */
    private final List _columnsForIndex;

    /** The platform specific settings. */
    private PlatformInfo _platformInfo;
    /** Contains default column sizes (minimum sizes that a JDBC-compliant db must support). */
    private HashMap _defaultSizes = new HashMap();
    /** The default database catalog to read. */
    private String _defaultCatalogPattern = "%";
    /** The default database schema(s) to read. */
    private String _defaultSchemaPattern = "%";
    /** The default pattern for reading all tables. */
    private String _defaultTablePattern = "%";
    /** The table types to recognize per default. */
    private String[] _defaultTableTypes = { "TABLE" };
    /** The active connection while reading a database model. */
    private Connection _connection;

    /**
     * Creates a new model reader instance.
     * 
     * @param platformInfo The platform specific settings
     */
    public JdbcModelReader(PlatformInfo platformInfo)
    {
        _platformInfo = platformInfo;

        _defaultSizes.put(new Integer(Types.CHAR),          "254");
        _defaultSizes.put(new Integer(Types.VARCHAR),       "254");
        _defaultSizes.put(new Integer(Types.LONGVARCHAR),   "254");
        _defaultSizes.put(new Integer(Types.BINARY),        "254");
        _defaultSizes.put(new Integer(Types.VARBINARY),     "254");
        _defaultSizes.put(new Integer(Types.LONGVARBINARY), "254");
        _defaultSizes.put(new Integer(Types.INTEGER),       "32");
        _defaultSizes.put(new Integer(Types.BIGINT),        "64");
        _defaultSizes.put(new Integer(Types.REAL),          "7,0");
        _defaultSizes.put(new Integer(Types.FLOAT),         "15,0");
        _defaultSizes.put(new Integer(Types.DOUBLE),        "15,0");
        _defaultSizes.put(new Integer(Types.DECIMAL),       "15,15");
        _defaultSizes.put(new Integer(Types.NUMERIC),       "15,15");

        _columnsForTable  = initColumnsForTable();
        _columnsForColumn = initColumnsForColumn();
        _columnsForPK     = initColumnsForPK();
        _columnsForFK     = initColumnsForFK();
        _columnsForIndex  = initColumnsForIndex();
    }

    /**
     * Returns the platform specific settings.
     *
     * @return The platform settings
     */
    public PlatformInfo getPlatformInfo()
    {
        return _platformInfo;
    }

    /**
     * Returns descriptors for the columns that shall be read from the result set when
     * reading the meta data for a table. Note that the columns are read in the order
     * defined by this list.<br/>
     * Redefine this method if you want more columns or a different order. 
     * 
     * @return The descriptors for the result set columns
     */
    protected List initColumnsForTable()
    {
        List result = new ArrayList();

        result.add(new MetaDataColumnDescriptor("TABLE_NAME",  Types.VARCHAR));
        result.add(new MetaDataColumnDescriptor("TABLE_TYPE",  Types.VARCHAR, "UNKNOWN"));
        result.add(new MetaDataColumnDescriptor("TABLE_CAT",   Types.VARCHAR));
        result.add(new MetaDataColumnDescriptor("TABLE_SCHEM", Types.VARCHAR));
        result.add(new MetaDataColumnDescriptor("REMARKS",     Types.VARCHAR));

        return result;
    }

    /**
     * Returns descriptors for the columns that shall be read from the result set when
     * reading the meta data for table columns. Note that the columns are read in the order
     * defined by this list.<br/>
     * Redefine this method if you want more columns or a different order.
     * 
     * @return The map column name -> descriptor for the result set columns
     */
    protected List initColumnsForColumn()
    {
        List result = new ArrayList();

        // As suggested by Alexandre Borgoltz, we're reading the COLUMN_DEF first because Oracle
        // has problems otherwise (it seemingly requires a LONG column to be the first to be read)
        // See also DDLUTILS-29
        result.add(new MetaDataColumnDescriptor("COLUMN_DEF",     Types.VARCHAR));
        result.add(new MetaDataColumnDescriptor("COLUMN_NAME",    Types.VARCHAR));
        result.add(new MetaDataColumnDescriptor("DATA_TYPE",      Types.INTEGER, new Integer(java.sql.Types.OTHER)));
        result.add(new MetaDataColumnDescriptor("NUM_PREC_RADIX", Types.INTEGER, new Integer(10)));
        result.add(new MetaDataColumnDescriptor("DECIMAL_DIGITS", Types.INTEGER, new Integer(0)));
        result.add(new MetaDataColumnDescriptor("COLUMN_SIZE",    Types.VARCHAR));
        result.add(new MetaDataColumnDescriptor("IS_NULLABLE",    Types.VARCHAR, "YES"));
        result.add(new MetaDataColumnDescriptor("REMARKS",        Types.VARCHAR));

        return result;
    }

    /**
     * Returns descriptors for the columns that shall be read from the result set when
     * reading the meta data for primary keys. Note that the columns are read in the order
     * defined by this list.<br/>
     * Redefine this method if you want more columns or a different order.
     * 
     * @return The map column name -> descriptor for the result set columns
     */
    protected List initColumnsForPK()
    {
        List result = new ArrayList();

        result.add(new MetaDataColumnDescriptor("COLUMN_NAME", Types.VARCHAR));

        return result;
    }

    /**
     * Returns descriptors for the columns that shall be read from the result set when
     * reading the meta data for foreign keys originating from a table. Note that the
     * columns are read in the order defined by this list.<br/>
     * Redefine this method if you want more columns or a different order.
     * 
     * @return The map column name -> descriptor for the result set columns
     */
    protected List initColumnsForFK()
    {
        List result = new ArrayList();

        result.add(new MetaDataColumnDescriptor("PKTABLE_NAME",  Types.VARCHAR));
        result.add(new MetaDataColumnDescriptor("KEY_SEQ",       Types.TINYINT, new Short((short)0)));
        result.add(new MetaDataColumnDescriptor("FK_NAME",       Types.VARCHAR));
        result.add(new MetaDataColumnDescriptor("PKCOLUMN_NAME", Types.VARCHAR));
        result.add(new MetaDataColumnDescriptor("FKCOLUMN_NAME", Types.VARCHAR));

        return result;
    }

    /**
     * Returns descriptors for the columns that shall be read from the result set when
     * reading the meta data for indices. Note that the columns are read in the order
     * defined by this list.<br/>
     * Redefine this method if you want more columns or a different order.
     * 
     * @return The map column name -> descriptor for the result set columns
     */
    protected List initColumnsForIndex()
    {
        List result = new ArrayList();

        result.add(new MetaDataColumnDescriptor("INDEX_NAME",       Types.VARCHAR));
        result.add(new MetaDataColumnDescriptor("NON_UNIQUE",       Types.BIT, Boolean.TRUE));
        result.add(new MetaDataColumnDescriptor("ORDINAL_POSITION", Types.TINYINT, new Short((short)0)));
        result.add(new MetaDataColumnDescriptor("COLUMN_NAME",      Types.VARCHAR));
        

        return result;
    }

    /**
     * Returns the catalog(s) in the database to read per default.
     *
     * @return The default catalog(s)
     */
    public String getDefaultCatalogPattern()
    {
        return _defaultCatalogPattern;
    }

    /**
     * Sets the catalog(s) in the database to read per default.
     * 
     * @param catalogPattern The catalog(s)
     */
    public void setDefaultCatalogPattern(String catalogPattern)
    {
        _defaultCatalogPattern = catalogPattern;
    }

    /**
     * Returns the schema(s) in the database to read per default.
     *
     * @return The default schema(s)
     */
    public String getDefaultSchemaPattern()
    {
        return _defaultSchemaPattern;
    }

    /**
     * Sets the schema(s) in the database to read per default.
     * 
     * @param schemaPattern The schema(s)
     */
    public void setDefaultSchemaPattern(String schemaPattern)
    {
        _defaultSchemaPattern = schemaPattern;
    }

    /**
     * Returns the default pattern to read the relevant tables from the database.
     *
     * @return The table pattern
     */
    public String getDefaultTablePattern()
    {
        return _defaultTablePattern;
    }

    /**
     * Sets the default pattern to read the relevant tables from the database.
     *
     * @param tablePattern The table pattern
     */
    public void setDefaultTablePattern(String tablePattern)
    {
        _defaultTablePattern = tablePattern;
    }

    /**
     * Returns the table types to recognize per default.
     *
     * @return The default table types
     */
    public String[] getDefaultTableTypes()
    {
        return _defaultTableTypes;
    }

    /**
     * Sets the table types to recognize per default. Typical types are "TABLE", "VIEW",
     * "SYSTEM TABLE", "GLOBAL TEMPORARY", "LOCAL TEMPORARY", "ALIAS", "SYNONYM".
     * 
     * @param types The table types
     */
    public void setDefaultTableTypes(String[] types)
    {
        _defaultTableTypes = types;
    }

    /**
     * Returns the descriptors for the columns to be read from the table meta data result set.
     *
     * @return The column descriptors
     */
    protected List getColumnsForTable()
    {
        return _columnsForTable;
    }

    /**
     * Returns the descriptors for the columns to be read from the column meta data result set.
     *
     * @return The column descriptors
     */
    protected List getColumnsForColumn()
    {
        return _columnsForColumn;
    }

    /**
     * Returns the descriptors for the columns to be read from the primary key meta data result set.
     *
     * @return The column descriptors
     */
    protected List getColumnsForPK()
    {
        return _columnsForPK;
    }

    /**
     * Returns the descriptors for the columns to be read from the foreign key meta data result set.
     *
     * @return The column descriptors
     */
    protected List getColumnsForFK()
    {
        return _columnsForFK;
    }

    /**
     * Returns the descriptors for the columns to be read from the index meta data result set.
     *
     * @return The column descriptors
     */
    protected List getColumnsForIndex()
    {
        return _columnsForIndex;
    }

    /**
     * Returns the active connection. Note that this is only set during a call to
     * {@link #readTables(String, String, String[])}.
     *
     * @return The connection or <code>null</code> if there is no active connection
     */
    protected Connection getConnection()
    {
        return _connection;
    }

    /**
     * Reads the database model from the given connection.
     * 
     * @param connection The connection
     * @param name       The name of the resulting database; <code>null</code> when the default name (the catalog)
     *                   is desired which might be <code>null</code> itself though
     * @return The database model
     */
    public Database getDatabase(Connection connection, String name) throws SQLException
    {
        return getDatabase(connection, name, null, null, null);
    }

    /**
     * Reads the database model from the given connection.
     * 
     * @param connection The connection
     * @param name       The name of the resulting database; <code>null</code> when the default name (the catalog)
     *                   is desired which might be <code>null</code> itself though
     * @param catalog    The catalog to acess in the database; use <code>null</code> for the default value
     * @param schema     The schema to acess in the database; use <code>null</code> for the default value
     * @param tableTypes The table types to process; use <code>null</code> or an empty list for the default ones
     * @return The database model
     */
    public Database getDatabase(Connection connection, String name, String catalog, String schema, String[] tableTypes) throws SQLException
    {
        Database db = new Database();

        if (name == null)
        {
            try 
            {
                db.setName(connection.getCatalog());
                if (catalog == null)
                {
                    catalog = db.getName();
                }
            } 
            catch(Exception e) 
            {
                _log.info("Cannot determine the catalog name from connection.");
            }
        }
        else
        {
            db.setName(name);
        }
        try
        {
            _connection = connection;
            db.addTables(readTables(catalog, schema, tableTypes));
        }
        finally
        {
            _connection = null;
        }
        return db;
    }

    /**
     * Reads the tables from the database metadata.
     * 
     * @param catalog       The catalog to acess in the database; use <code>null</code> for the default value
     * @param schemaPattern The schema(s) to acess in the database; use <code>null</code> for the default value
     * @param tableTypes    The table types to process; use <code>null</code> or an empty list for the default ones
     * @return The tables
     */
    protected Collection readTables(String catalog, String schemaPattern, String[] tableTypes) throws SQLException
    {
        ResultSet tableData = null;

        try
        {
            DatabaseMetaDataWrapper metaData = new DatabaseMetaDataWrapper();

            metaData.setMetaData(_connection.getMetaData());
            metaData.setCatalog(catalog == null ? getDefaultCatalogPattern() : catalog);
            metaData.setSchemaPattern(schemaPattern == null ? getDefaultSchemaPattern() : schemaPattern);
            metaData.setTableTypes((tableTypes == null) || (tableTypes.length == 0) ? getDefaultTableTypes() : tableTypes);
            
            tableData = metaData.getTables(getDefaultTablePattern());

            List tables = new ArrayList();

            while (tableData.next())
            {
                Map   values = readColumns(tableData, getColumnsForTable());
                Table table  = readTable(metaData, values);

                if (table != null)
                {
                    tables.add(table);
                }
            }
            return tables;
        }
        finally
        {
            if (tableData != null)
            {
                tableData.close();
            }
        }
    }

    /**
     * Reads the next table from the meta data.
     * 
     * @param metaData The database meta data
     * @param values   The table metadata values as defined by {@link #getColumnsForTable()}
     * @return The table or <code>null</code> if the result set row did not contain a valid table
     */
    protected Table readTable(DatabaseMetaDataWrapper metaData, Map values) throws SQLException
    {
        String tableName = (String)values.get("TABLE_NAME");
        Table  table     = null;
        
        if ((tableName != null) && (tableName.length() > 0))
        {
            table = new Table();

            table.setName(tableName);
            table.setType((String)values.get("TABLE_TYPE"));
            table.setCatalog((String)values.get("TABLE_CAT"));
            table.setSchema((String)values.get("TABLE_SCHEM"));
            table.setDescription((String)values.get("REMARKS"));

            table.addColumns(readColumns(metaData, tableName));
            table.addForeignKeys(readForeignKeys(metaData, tableName));
            table.addIndices(readIndices(metaData, tableName));

            Collection primaryKeys = readPrimaryKeyNames(metaData, tableName);

            for (Iterator it = primaryKeys.iterator(); it.hasNext();)
            {
                table.findColumn((String)it.next(), true).setPrimaryKey(true);
            }

            if (getPlatformInfo().isReturningSystemIndices())
            {
                removeSystemIndices(table);
            }
        }
        return table;
    }


    /**
     * Removes system indices (generated by the database for primary and foreign keys)
     * from the table.
     * 
     * @param table The table
     */
    protected void removeSystemIndices(Table table)
    {
        removeInternalPrimaryKeyIndex(table);

        for (int fkIdx = 0; fkIdx < table.getForeignKeyCount(); fkIdx++)
        {
            removeInternalForeignKeyIndex(table, table.getForeignKey(fkIdx));
        }
    }

    /**
     * Tries to remove the internal index for the table's primary key.
     * 
     * @param table The table
     */
    protected void removeInternalPrimaryKeyIndex(Table table)
    {
        Column[] pks         = table.getPrimaryKeyColumns();
        List     columnNames = new ArrayList();

        for (int columnIdx = 0; columnIdx < pks.length; columnIdx++)
        {
            columnNames.add(pks[columnIdx].getName());
        }

        for (int indexIdx = 0; indexIdx < table.getIndexCount(); indexIdx++)
        {
            Index index = table.getIndex(indexIdx);

            if (index.isUnique() && matches(index, columnNames) && 
                isInternalPrimaryKeyIndex(table, index))
            {
                table.removeIndex(indexIdx);
                break;
            }
        }
    }

    /**
     * Tries to remove the internal index for the given foreign key.
     * 
     * @param table The table where the table is defined
     * @param fk    The foreign key
     */
    protected void removeInternalForeignKeyIndex(Table table, ForeignKey fk)
    {
        List columnNames = new ArrayList();

        for (int columnIdx = 0; columnIdx < fk.getReferenceCount(); columnIdx++)
        {
            columnNames.add(fk.getReference(columnIdx).getLocalColumnName());
        }

        for (int indexIdx = 0; indexIdx < table.getIndexCount(); indexIdx++)
        {
            Index index = table.getIndex(indexIdx);

            if (!index.isUnique() && matches(index, columnNames) && 
                isInternalForeignKeyIndex(table, fk, index))
            {
                table.removeIndex(indexIdx);
                break;
            }
        }
    }

    /**
     * Checks whether the given index matches the column list.
     * 
     * @param index              The index
     * @param columnsToSearchFor The names of the columns that the index should be for
     * @return <code>true</code> if the index matches the columns
     */
    protected boolean matches(Index index, List columnsToSearchFor)
    {
        if (index.getColumnCount() != columnsToSearchFor.size())
        {
            return false;
        }
        for (int columnIdx = 0; columnIdx < index.getColumnCount(); columnIdx++)
        {
            if (!columnsToSearchFor.get(columnIdx).equals(index.getColumn(columnIdx).getName()))
            {
                return false;
            }
        }
        return true;
    }

    /**
     * Tries to determine whether the index is the internal database-generated index
     * for the given table's primary key.
     * Note that only unique indices with the correct columns are fed to this method.
     * Redefine this method for specific platforms if there are better ways
     * to determine internal indices.
     * 
     * @param table The table owning the index
     * @param index The index to check
     * @return <code>true</code> if the index seems to be an internal primary key one
     */
    protected boolean isInternalPrimaryKeyIndex(Table table, Index index)
    {
        return false;
    }

    /**
     * Tries to determine whether the index is the internal database-generated index
     * for the given foreign key.
     * Note that only non-unique indices with the correct columns are fed to this method.
     * Redefine this method for specific platforms if there are better ways
     * to determine internal indices.
     * 
     * @param table The table owning the index and foreign key
     * @param fk    The foreign key
     * @param index The index to check
     * @return <code>true</code> if the index seems to be an internal primary key one
     */
    protected boolean isInternalForeignKeyIndex(Table table, ForeignKey fk, Index index)
    {
        return false;
    }

    /**
     * Reads the column definitions for the indicated table.
     * 
     * @param metaData  The database meta data
     * @param tableName The name of the table
     * @return The columns
     */
    private Collection readColumns(DatabaseMetaDataWrapper metaData, String tableName) throws SQLException
    {
        ResultSet columnData = null;

        try
        {
            columnData = metaData.getColumns(tableName, null);

            List columns = new ArrayList();

            while (columnData.next())
            {
                Map values = readColumns(columnData, getColumnsForColumn());

                columns.add(readColumn(metaData, values));
            }
            return columns;
        }
        finally
        {
            if (columnData != null)
            {
                columnData.close();
            }
        }
    }

    /**
     * Extracts a column definition from the result set.
     * 
     * @param metaData The database meta data
     * @param values   The column meta data values as defined by {@link #getColumnsForColumn()}
     * @return The column
     */
    protected Column readColumn(DatabaseMetaDataWrapper metaData, Map values) throws SQLException
    {
        Column column = new Column();

        column.setName((String)values.get("COLUMN_NAME"));
        column.setDefaultValue((String)values.get("COLUMN_DEF"));
        column.setTypeCode(((Integer)values.get("DATA_TYPE")).intValue());
        column.setPrecisionRadix(((Integer)values.get("NUM_PREC_RADIX")).intValue());

        String size  = (String)values.get("COLUMN_SIZE");
        int    scale = ((Integer)values.get("DECIMAL_DIGITS")).intValue();

        if (size == null)
        {
            size = (String)_defaultSizes.get(new Integer(column.getTypeCode()));
        }
        // we're setting the size after the precision and radix in case
        // the database prefers to return them in the size value
        column.setSize(size);
        if (scale != 0)
        {
            // if there is a scale value, set it after the size (which probably did not contain
            // a scale specification)
            column.setScale(scale);
        }
        column.setRequired("NO".equalsIgnoreCase(((String)values.get("IS_NULLABLE")).trim()));
        column.setDescription((String)values.get("REMAKRS"));
        return column;
    }

    /**
     * Retrieves the names of the columns that make up the primary key for a given table.
     *
     * @param metaData  The database meta data
     * @param tableName The name of the table from which to retrieve PK information
     * @return The primary key column names
     */
    protected Collection readPrimaryKeyNames(DatabaseMetaDataWrapper metaData, String tableName) throws SQLException
    {
        List      pks   = new ArrayList();
        ResultSet pkData = null;

        try
        {
            pkData = metaData.getPrimaryKeys(tableName);
            while (pkData.next())
            {
                Map values = readColumns(pkData, getColumnsForPK());

                pks.add(readPrimaryKeyName(metaData, values));
            }
        }
        finally
        {
            if (pkData != null)
            {
                pkData.close();
            }
        }
        return pks;
    }

    /**
     * Extracts a primary key name from the result set.
     *
     * @param metaData The database meta data
     * @param values   The primary key meta data values as defined by {@link #getColumnsForPK()}
     * @return The primary key name
     */
    protected String readPrimaryKeyName(DatabaseMetaDataWrapper metaData, Map values) throws SQLException
    {
        return (String)values.get("COLUMN_NAME");
    }

    /**
     * Retrieves the foreign keys of the indicated table.
     *
     * @param metaData  The database meta data
     * @param tableName The name of the table from which to retrieve FK information
     * @return The foreign keys
     */
    protected Collection readForeignKeys(DatabaseMetaDataWrapper metaData, String tableName) throws SQLException
    {
        Map       fks    = new ListOrderedMap();
        ResultSet fkData = null;

        try
        {
            fkData = metaData.getForeignKeys(tableName);

            while (fkData.next())
            {
                Map values = readColumns(fkData, getColumnsForFK());

                readForeignKey(metaData, values, fks);
            }
        }
        finally
        {
            if (fkData != null)
            {
                fkData.close();
            }
        }
        return fks.values();
    }

    /**
     * Reads the next foreign key spec from the result set.
     *
     * @param metaData The database meta data
     * @param values   The foreign key meta data as defined by {@link #getColumnsForFK()}
     * @param knownFks The already read foreign keys for the current table
     */
    protected void readForeignKey(DatabaseMetaDataWrapper metaData, Map values, Map knownFks) throws SQLException
    {
        String     fkName = (String)values.get("FK_NAME");
        ForeignKey fk     = (ForeignKey)knownFks.get(fkName);

        if (fk == null)
        {
            fk = new ForeignKey(fkName);
            fk.setForeignTableName((String)values.get("PKTABLE_NAME"));
            knownFks.put(fkName, fk);
        }

        Reference ref = new Reference();

        ref.setForeignColumnName((String)values.get("PKCOLUMN_NAME"));
        ref.setLocalColumnName((String)values.get("FKCOLUMN_NAME"));
        if (values.containsKey("KEY_SEQ"))
        {
            ref.setSequenceValue(((Short)values.get("KEY_SEQ")).intValue());
        }
        fk.addReference(ref);
    }

    /**
     * Determines the indices for the indicated table.
     * 
     * @param metaData  The database meta data
     * @param tableName The name of the table
     * @return The list of indices
     */
    protected Collection readIndices(DatabaseMetaDataWrapper metaData, String tableName) throws SQLException
    {
        Map       indices   = new ListOrderedMap();
        ResultSet indexData = null;

        try 
        {
            indexData = metaData.getIndices(tableName, false, false);

            while (indexData.next())
            {
                Map values = readColumns(indexData, getColumnsForIndex());

                readIndex(metaData, values, indices);
            }
        }
        finally
        {
            if (indexData != null)
            {
                indexData.close();
            }
        }
        return indices.values();
    }

    /**
     * Reads the next index spec from the result set.
     * 
     * @param metaData     The database meta data
     * @param values       The index meta data as defined by {@link #getColumnsForIndex()}
     * @param knownIndices The already read indices for the current table
     */
    protected void readIndex(DatabaseMetaDataWrapper metaData, Map values, Map knownIndices) throws SQLException
    {
        String indexName = (String)values.get("INDEX_NAME");
        Index  index     = (Index)knownIndices.get(indexName);

        if ((index == null) && (indexName != null))
        {
            if (((Boolean)values.get("NON_UNIQUE")).booleanValue())
            {
                index = new NonUniqueIndex();
            }
            else
            {
                index = new UniqueIndex();
            }

            index.setName(indexName);
            knownIndices.put(indexName, index);
        }

        IndexColumn indexColumn = new IndexColumn();

        indexColumn.setName((String)values.get("COLUMN_NAME"));
        if (values.containsKey("ORDINAL_POSITION"))
        {
            indexColumn.setOrdinalPosition(((Short)values.get("ORDINAL_POSITION")).intValue());
        }
        index.addColumn(indexColumn);
    }

    /**
     * Reads the indicated columns from the result set.
     * 
     * @param resultSet         The result set
     * @param columnDescriptors The dscriptors of the columns to read
     * @return The read values keyed by the column name
     */
    protected Map readColumns(ResultSet resultSet, List columnDescriptors) throws SQLException
    {
        HashMap values = new HashMap();

        for (Iterator it = columnDescriptors.iterator(); it.hasNext();)
        {
            MetaDataColumnDescriptor descriptor = (MetaDataColumnDescriptor)it.next();

            values.put(descriptor.getName(), descriptor.readColumn(resultSet));
        }
        return values;
    }
}
