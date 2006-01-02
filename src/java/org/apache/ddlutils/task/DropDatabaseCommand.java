package org.apache.ddlutils.task;

/*
 * Copyright 1999-2006 The Apache Software Foundation.
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

import org.apache.commons.dbcp.BasicDataSource;
import org.apache.ddlutils.Platform;
import org.apache.ddlutils.model.Database;
import org.apache.tools.ant.BuildException;
import org.apache.tools.ant.Project;
import org.apache.tools.ant.Task;

/**
 * Command for dropping a database.
 * 
 * @author Thomas Dudziak
 * @version $Revision: 289996 $
 */
public class DropDatabaseCommand extends DatabaseCommand
{
    /**
     * {@inheritDoc}
     */
    public boolean isRequiringModel()
    {
        return false;
    }

    /**
     * {@inheritDoc}
     */
    public void execute(Task task, Database model) throws BuildException
    {
        BasicDataSource dataSource = getDataSource();

        if (dataSource == null)
        {
            throw new BuildException("No database specified.");
        }

        Platform platform = getPlatform();

        try
        {
            platform.dropDatabase(dataSource.getDriverClassName(),
                                  dataSource.getUrl(),
                                  dataSource.getUsername(),
                                  dataSource.getPassword());

            task.log("Dropped database", Project.MSG_INFO);
        }
        catch (UnsupportedOperationException ex)
        {
            task.log("Database platform "+getPlatform().getName()+" does not support database dropping via JDBC", Project.MSG_ERR);
        }
        catch (Exception ex)
        {
            if (isFailOnError())
            {
                throw new BuildException(ex);
            }
            else
            {
                task.log(ex.getLocalizedMessage(), Project.MSG_ERR);
            }
        }
    }
}
