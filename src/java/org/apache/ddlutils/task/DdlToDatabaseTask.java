package org.apache.ddlutils.task;

/*
 * Copyright 1999-2004 The Apache Software Foundation.
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

import java.beans.IntrospectionException;
import java.io.File;
import java.util.ArrayList;
import java.util.Iterator;

import org.apache.ddlutils.io.DatabaseReader;
import org.apache.ddlutils.model.Database;
import org.apache.tools.ant.BuildException;
import org.apache.tools.ant.DirectoryScanner;
import org.apache.tools.ant.Project;
import org.apache.tools.ant.Task;
import org.apache.tools.ant.types.FileSet;

/**
 * Ant task for working with DDL, e.g. generating the database from a schema, inserting data,
 */
public class DdlToDatabaseTask extends Task
{
    /** The input files */
    private ArrayList _fileSets = new ArrayList();
    /** The sub tasks to execute */
    private ArrayList _commands = new ArrayList();

    /**
     * Adds a fileset.
     * 
     * @param fileset The additional input files
     */
    public void addConfiguredFileset(FileSet fileset)
    {
        _fileSets.add(fileset);
    }

    /**
     * Set the xml schema describing the application model.
     *
     * @param schemaFile The schema
     */
    public void setSchemaFile(File schemaFile)
    {
        FileSet fileSet = new FileSet();

        fileSet.setIncludesfile(schemaFile);
        _fileSets.add(fileSet);
    }

    /**
     * Adds the "write dtd to file"-command.
     * 
     * @param command The command
     */
    public void addWriteDtdToFile(WriteDtdToFileCommand command)
    {
        _commands.add(command);
    }

    /**
     * Adds the "write schema to database"-command
     * 
     * @param command The command
     */
    public void addWriteSchemaToDatabase(WriteSchemaToDatabaseCommand command)
    {
        _commands.add(command);
    }

    /**
     * Adds the "write schema sql to file"-command
     * 
     * @param command The command
     */
    public void addWriteSchemaSqlToFile(WriteSchemaSqlToFileCommand command)
    {
        _commands.add(command);
    }

    /**
     * Reads the schemas from the specified files and merges them into one database model.
     * 
     * @return The database model
     */
    private Database readSchemaFiles()
    {
        DatabaseReader reader = null;
        Database       model  = null;

        try
        {
            reader = new DatabaseReader();
        }
        catch (IntrospectionException ex)
        {
            throw new BuildException(ex);
        }
        for (Iterator it = _fileSets.iterator(); it.hasNext();)
        {
            FileSet          fileSet    = (FileSet)it.next();
            File             fileSetDir = fileSet.getDir(getProject());
            DirectoryScanner scanner    = fileSet.getDirectoryScanner(getProject());
            String[]         files      = scanner.getIncludedFiles();

            for (int idx = 0; (files != null) && (idx < files.length); idx++)
            {
                File curSchemaFile = new File(fileSetDir, files[idx]);

                if (!curSchemaFile.exists())
                {
                    log("Could not find schema file "+files[idx], Project.MSG_ERR);
                }
                else if (!curSchemaFile.isFile())
                {
                    log("Path "+curSchemaFile.getAbsolutePath()+" does not denote a schema file", Project.MSG_ERR);
                }
                else if (!curSchemaFile.canRead())
                {
                    log("Could not read schema file "+curSchemaFile.getAbsolutePath(), Project.MSG_ERR);
                }
                else
                {
                    Database curModel = null;

                    try
                    {
                        curModel = (Database)reader.parse(curSchemaFile);
                        log("Read schema file "+curSchemaFile.getAbsolutePath(), Project.MSG_INFO);
                    }
                    catch (Exception ex)
                    {
                        throw new BuildException("Could not read schema file "+curSchemaFile.getAbsolutePath(), ex);
                    }
                    if (model == null)
                    {
                        model = curModel;
                    }
                    else
                    {
                        try
                        {
                            model.mergeWith(curModel);
                        }
                        catch (IllegalArgumentException ex)
                        {
                            throw new BuildException("Could not merge with schema from file "+files[idx], ex);
                        }
                    }
                }
            }
        }
        return model;
    }

    /* (non-Javadoc)
     * @see org.apache.tools.ant.Task#execute()
     */
    public void execute() throws BuildException
    {
        if (_commands.isEmpty())
        {
            System.out.println("No sub tasks specified, so there is nothing to do.");
            return;
        }

        Database model = readSchemaFiles();

        if (model == null)
        {
            System.out.println("No schemas read, so there is nothing to do.");
            return;
        }

        for (Iterator it = _commands.iterator(); it.hasNext();)
        {
            Command command = (Command)it.next();
            
            command.execute(this, model);
        }
    }

}
