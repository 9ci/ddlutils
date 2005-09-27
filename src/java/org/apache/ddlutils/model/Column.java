package org.apache.ddlutils.model;

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

import java.io.Serializable;

/**
 * Represents a column in the database model.
 * 
 * @author Thomas Dudziak
 * @version $Revision$
 */
public class Column implements Cloneable, Serializable
{
    /** Unique ID for serialization purposes. */
    private static final long serialVersionUID = -6226348998874210093L;

    /** The name of the column. */
    private String _name;
    /** The java name of the column (optional and unused by DdlUtils, for Torque compatibility). */
    private String _javaName;
    /** The column's description. */
    private String _description;
    /** Whether the column is a primary key column. */
    private boolean _primaryKey;
    /** Whether the column is required, ie. it must not contain <code>NULL</code>. */
    private boolean _required;
    /** Whether the column's value is incremented automatically. */
    private boolean _autoIncrement;
    /** The JDBC type code, one of the constants in {@link java.sql.Types}. */
    private int _typeCode;
    /** The name of the JDBC type. */
    private String _type;
    /** The size of the column for JDBC types that require/support this. */
    private String _size;
    /** The scale of the column for JDBC types that require/support this. */
    private int _scale = 0;
    /** The precision radix of the column for JDBC types that require/support this. */
    private int _precisionRadix = 10;
    /** The ordinal position of the column for JDBC types that require/support this. */
    private int _ordinalPosition = 0;
    /** The default value. */
    private String _defaultValue;

    // TODO: Implement equals and hashcode

    /**
     * Returns the name of the column.
     * 
     * @return The name
     */
    public String getName()
    {
        return _name;
    }

    /**
     * Sets the name of the column.
     * 
     * @param name The name
     */
    public void setName(String name)
    {
        _name = name;
    }

    /**
     * Returns the java name of the column. This property is unused by DdlUtils and only
     * for Torque compatibility.
     * 
     * @return The java name
     */
    public String getJavaName()
    {
        return _javaName;
    }

    /**
     * Sets the java name of the column. This property is unused by DdlUtils and only
     * for Torque compatibility.
     * 
     * @param javaName The java name
     */
    public void setJavaName(String javaName)
    {
        _javaName = javaName;
    }

    /**
     * Returns the description of the column.
     *
     * @return The description
     */
    public String getDescription()
    {
        return _description;
    }

    /**
     * Sets the description of the column.
     *
     * @param description The description
     */
    public void setDescription(String description)
    {
        _description = description;
    }

    /**
     * Determines whether this column is a primary key column.
     * 
     * @return <code>true</code> if this column is a primary key column
     */
    public boolean isPrimaryKey()
    {
        return _primaryKey;
    }

    /**
     * Specifies whether this column is a primary key column.
     * 
     * @param primaryKey <code>true</code> if this column is a primary key column
     */
    public void setPrimaryKey(boolean primaryKey)
    {
        _primaryKey = primaryKey;
    }

    /**
     * Determines whether this column is a required column, ie. that it is not allowed
     * to contain <code>NULL</code> values.
     * 
     * @return <code>true</code> if this column is a required column
     */
    public boolean isRequired()
    {
        return _required;
    }

    /**
     * Specifies whether this column is a required column, ie. that it is not allowed
     * to contain <code>NULL</code> values.
     * 
     * @param required <code>true</code> if this column is a required column
     */
    public void setRequired(boolean required)
    {
        _required = required;
    }

    /**
     * Determines whether this column is an auto-increment column.
     * 
     * @return <code>true</code> if this column is an auto-increment column
     */
    public boolean isAutoIncrement()
    {
        return _autoIncrement;
    }

    /**
     * Specifies whether this column is an auto-increment column.
     * 
     * @param autoIncrement <code>true</code> if this column is an auto-increment column
     */
    public void setAutoIncrement(boolean autoIncrement)
    {
        _autoIncrement = autoIncrement;
    }

    /**
     * Returns the code (one of the constants in {@link java.sql.Types}) of the
     * JDBC type of the column.
     * 
     * @return The type code
     */
    public int getTypeCode()
    {
        return _typeCode;
    }

    /**
     * Sets the code (one of the constants in {@link java.sql.Types}) of the
     * JDBC type of the column. 
     * 
     * @param typeCode The type code
     */
    public void setTypeCode(int typeCode)
    {
        _type = TypeMap.getJdbcTypeName(typeCode);
        if (_type == null)
        {
            throw new ModelException("Unknown JDBC type code "+typeCode);
        }
        _typeCode = typeCode;
    }

    /**
     * Returns the JDBC type of the column.
     * 
     * @return The type
     */
    public String getType()
    {
        return _type;
    }

    /**
     * Sets the JDBC type of the column.
     *
     * @param type The type
     */
    public void setType(String type)
    {
        Integer typeCode = TypeMap.getJdbcTypeCode(type);

        if (typeCode == null)
        {
            throw new ModelException("Unknown JDBC type "+type);
        }
        else
        {
            _typeCode = typeCode.intValue();
        }
        _type = type;
    }

    /**
     * Determines whether this column is of a numeric type.
     * 
     * @return <code>true</code> if this column is of a numeric type
     */
    public boolean isOfNumericType()
    {
        return TypeMap.isNumericType(getTypeCode());
    }

    /**
     * Determines whether this column is of a text type.
     * 
     * @return <code>true</code> if this column is of a text type
     */
    public boolean isOfTextType()
    {
        return TypeMap.isTextType(getTypeCode());
    }

    /**
     * Determines whether this column is of a binary type.
     * 
     * @return <code>true</code> if this column is of a binary type
     */
    public boolean isOfBinaryType()
    {
        return TypeMap.isBinaryType(getTypeCode());
    }

    /**
     * Determines whether this column is of a special type.
     * 
     * @return <code>true</code> if this column is of a special type
     */
    public boolean isOfSpecialType()
    {
        return TypeMap.isSpecialType(getTypeCode());
    }
    
    /**
     * Returns the size of the column.
     * 
     * @return The size
     */
    public String getSize()
    {
        return _size;
    }

    /**
     * Returns the size of the column as an integer.
     * 
     * @return The size as an integer
     */
    public int getSizeAsInt()
    {
        return _size == null ? 0 : Integer.parseInt(_size);
    }

    /**
     * Sets the size of the column. This is either a simple integer value or
     * a comma-separated pair of integer values specifying the size and scale.
     * 
     * @param size The size
     */
    public void setSize(String size)
    {
        if (size != null)
        {
            int pos = size.indexOf(",");
    
            if (pos < 0)
            {
                _size = size;
            }
            else
            {
                _size  = size.substring(0, pos);
                _scale = Integer.parseInt(size.substring(pos + 1));
            }
        }
    }
    
    /**
     * Returns the scale of the column.
     * 
     * @return The scale
     */
    public int getScale()
    {
        return this._scale;
    }

    /**
     * Sets the scale of the column.
     *
     * @param scale The scale
     */
    public void setScale(int scale)
    {
        _scale = scale;
    }

    /**
     * Returns the precision radix of the column.
     * 
     * @return The precision radix
     */
    public int getPrecisionRadix()
    {
        return this._precisionRadix;
    }

    /**
     * Sets the precision radix of the column.
     * 
     * @param precisionRadix The precision radix
     */
    public void setPrecisionRadix(int precisionRadix)
    {
        _precisionRadix = precisionRadix;
    }

    /**
     * Returns the ordinal position of the column.
     * 
     * @return The ordinal position
     */
    public int getOrdinalPosition()
    {
        return this._ordinalPosition;
    }

    /**
     * Sets the ordinal position of the column.
     * 
     * @param ordinalPosition The ordinal position
     */
    public void setOrdinalPosition(int ordinalPosition)
    {
        _ordinalPosition = ordinalPosition;
    }

    /**
     * Returns the default value of the column.
     * 
     * @return The default value
     */
    public String getDefaultValue()
    {
        return _defaultValue;
    }

    /**
     * Sets the default value of the column. Note that this expression will be used
     * within quotation marks when generating the column, and thus is subject to
     * the conversion rules of the target database.
     * 
     * @param defaultValue The default value
     */
    public void setDefaultValue(String defaultValue)
    {
        _defaultValue = defaultValue;
    }

    /**
     * {@inheritDoc}
     */
    public Object clone() throws CloneNotSupportedException
    {
        Column result = new Column();

        result._name            = _name;
        result._javaName        = _javaName;
        result._primaryKey      = _primaryKey;
        result._required        = _required;
        result._autoIncrement   = _autoIncrement;
        result._typeCode        = _typeCode;
        result._type            = _type;
        result._size            = _size;
        result._defaultValue    = _defaultValue;
        result._scale           = _scale;
        result._precisionRadix  = _precisionRadix;
        result._ordinalPosition = _ordinalPosition;
        return result;
    }

    /**
     * {@inheritDoc}
     */
    public String toString()
    {
        StringBuffer result = new StringBuffer();

        result.append("Column [name=");
        result.append(getName());
        result.append("; type=");
        result.append(getType());
        result.append("]");

        return result.toString();
    }

    /**
     * Returns a verbose string representation of this column.
     * 
     * @return The string representation
     */
    public String toVerboseString()
    {
        StringBuffer result = new StringBuffer();

        result.append("Column [name=");
        result.append(getName());
        result.append("; javaName=");
        result.append(getJavaName());
        result.append("; type=");
        result.append(getType());
        result.append("; typeCode=");
        result.append(getTypeCode());
        result.append("; size=");
        result.append(getSize());
        result.append("; required=");
        result.append(isRequired());
        result.append("; primaryKey=");
        result.append(isPrimaryKey());
        result.append("; autoIncrement=");
        result.append(isAutoIncrement());
        result.append("; defaultValue=");
        result.append(getDefaultValue());
        result.append("; scale=");
        result.append(getScale());
        result.append("; precisionRadix=");
        result.append(getPrecisionRadix());
        result.append("; ordinalPosition=");
        result.append(getOrdinalPosition());
        result.append("]");

        return result.toString();
    }
}
