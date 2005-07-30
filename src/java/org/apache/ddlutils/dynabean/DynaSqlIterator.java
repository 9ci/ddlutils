package org.apache.ddlutils.dynabean;

import java.sql.Connection;
import java.sql.ResultSet;
import java.sql.ResultSetMetaData;
import java.sql.SQLException;
import java.sql.Statement;
import java.util.Iterator;
import java.util.Map;
import java.util.NoSuchElementException;

import org.apache.commons.beanutils.BasicDynaBean;
import org.apache.commons.beanutils.BasicDynaClass;
import org.apache.commons.beanutils.DynaBean;
import org.apache.commons.beanutils.DynaClass;
import org.apache.commons.beanutils.DynaProperty;
import org.apache.commons.collections.map.ListOrderedMap;
import org.apache.ddlutils.model.Column;
import org.apache.ddlutils.model.Table;

/**
 * This is an iterator that is specifically targeted at traversing result sets.
 * If the query is against a known table, then {@link org.apache.ddlutils.dynabean.SqlDynaBean} instances
 * are created from the rows, otherwise normal {@link org.apache.commons.beanutils.DynaBean} instances
 * are created.
 */
public class DynaSqlIterator implements Iterator
{
    /** The base result set */
    private ResultSet _resultSet;
    /** The dyna class to use for creating beans */
    private DynaClass _dynaClass;
    /** Maps column names to properties */
    private Map _columnsToProperties = new ListOrderedMap();
    /** Whether we're already at the end of the result set */
    private boolean _isAtEnd = false;
    /** Whether to close the statement and connection after finishing */
    private boolean _cleanUpAfterFinish;

    /**
     * Creates a new iterator.
     * 
     * @param dynaSql            The dyna sql instance
     * @param resultSet          The result set
     * @param cleanUpAfterFinish Whether to close the statement and connection after finishing
     *                           the iteration, upon on exception, or when this iterator is garbage collected
     */
    public DynaSqlIterator(DynaSql dynaSql, ResultSet resultSet, boolean cleanUpAfterFinish) throws DynaSqlException
    {
        if (resultSet != null)
        {
            _resultSet          = resultSet;
            _cleanUpAfterFinish = cleanUpAfterFinish;

            try
            {
                initFromMetaData(dynaSql, resultSet);
            }
            catch (SQLException ex)
            {
                cleanUp();
                throw new DynaSqlException("Could not read the metadata of the result set", ex);
            }
        }
        else
        {
            _isAtEnd = true;
        }
    }

    /**
     * Initializes this iterator from the resultset metadata.
     * 
     * @param dynaSql   The dyna sql instance
     * @param resultSet The result set
     */
    private void initFromMetaData(DynaSql dynaSql, ResultSet resultSet) throws SQLException
    {
        ResultSetMetaData metaData         = resultSet.getMetaData();
        String            tableName        = null;
        boolean           singleKnownTable = true;
        boolean           caseSensitive    = (dynaSql.getSqlBuilder() == null ? false : dynaSql.getSqlBuilder().isCaseSensitive());

        for (int idx = 1; idx <= metaData.getColumnCount(); idx++)
        {
            String tableOfColumn = metaData.getTableName(idx);

            if ((tableOfColumn != null) && (tableOfColumn.length() > 0))
            {
                if (tableName == null)
                {
                    tableName = tableOfColumn;
                }
                else if (!tableName.equals(tableOfColumn))
                {
                    singleKnownTable = false;
                }
            }

            Table  table      = dynaSql.getDatabase().findTable(tableOfColumn, caseSensitive);
            String columnName = metaData.getColumnName(idx);
            String propName   = columnName;

            if (table != null)
            {
                Column column = table.findColumn(columnName, caseSensitive);

                if (column != null)
                {
                    propName = column.getName();
                }
            }
            _columnsToProperties.put(columnName, propName);
        }
        if (singleKnownTable && (tableName != null))
        {
            _dynaClass = dynaSql.getDynaClass(tableName);
        }
        else
        {
            DynaProperty[] props = new DynaProperty[_columnsToProperties.size()];
            int            idx   = 0;

            for (Iterator it = _columnsToProperties.values().iterator(); it.hasNext(); idx++)
            {
                props[idx] = new DynaProperty((String)it.next());
            }
            _dynaClass = new BasicDynaClass("result", BasicDynaBean.class, props);
        }
    }
    
    /* (non-Javadoc)
     * @see java.util.Iterator#hasNext()
     */
    public boolean hasNext() throws DynaSqlException
    {
        if (_isAtEnd)
        {
            return false;
        }
        else
        {
            try
            {
                if (_resultSet.next())
                {
                    return true;
                }
                else
                {
                    _isAtEnd = true;
                    cleanUp();
                    return false;
                }
            }
            catch (SQLException ex)
            {
                cleanUp();
                throw new DynaSqlException("Exception while advancing the resultset cursor", ex);
            }
        }
    }

    /* (non-Javadoc)
     * @see java.util.Iterator#next()
     */
    public Object next() throws DynaSqlException
    {
        if (_isAtEnd)
        {
            throw new NoSuchElementException("No more elements in the resultset");
        }
        else
        {
            try
            {
                DynaBean bean = _dynaClass.newInstance();

                for (Iterator it = _columnsToProperties.entrySet().iterator(); it.hasNext();)
                {
                    Map.Entry entry = (Map.Entry)it.next();

                    bean.set((String)entry.getValue(), _resultSet.getObject((String)entry.getKey()));
                }
                return bean;
            }
            catch (Exception ex)
            {
                cleanUp();
                throw new DynaSqlException("Exception while reading the row from the resultset", ex);
            }
        }
    }

    /* (non-Javadoc)
     * @see java.util.Iterator#remove()
     */
    public void remove() throws DynaSqlException
    {
        try
        {
            _resultSet.deleteRow();
        }
        catch (SQLException ex)
        {
            cleanUp();
            throw new DynaSqlException("Failed to delete current row", ex);
        }
    }

    /**
     * Closes the resources (connection, statement, resultset).
     */
    public void cleanUp()
    {
        if (_cleanUpAfterFinish && (_resultSet != null))
        {
            try
            {
                Statement  stmt = _resultSet.getStatement();
                Connection conn = stmt.getConnection();

                // also closes the resultset
                stmt.close();
                conn.close();
            }
            catch (SQLException ex)
            {
                // we ignore it
            }
            _resultSet = null;
        }
    }

    /* (non-Javadoc)
     * @see java.lang.Object#finalize()
     */
    protected void finalize() throws Throwable
    {
        cleanUp();
    }

    /**
     * Determines whether the connection is still open.
     * 
     * @return <code>true</code> if the connection is still open
     */
    public boolean isConnectionOpen()
    {
        if (_resultSet == null)
        {
            return false;
        }
        try
        {
            Statement  stmt = _resultSet.getStatement();
            Connection conn = stmt.getConnection();

            return !conn.isClosed();
        }
        catch (SQLException ex)
        {
            return false;
        }
    }
}
