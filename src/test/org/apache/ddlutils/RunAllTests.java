package org.apache.ddlutils;

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

import org.apache.ddlutils.platform.TestAxionPlatform;
import org.apache.ddlutils.platform.TestCloudscapePlatform;
import org.apache.ddlutils.platform.TestDB2Platform;
import org.apache.ddlutils.platform.TestDerbyPlatform;
import org.apache.ddlutils.platform.TestFirebirdPlatform;
import org.apache.ddlutils.platform.TestHsqlDbPlatform;
import org.apache.ddlutils.platform.TestInterbasePlatform;
import org.apache.ddlutils.platform.TestMSSqlPlatform;
import org.apache.ddlutils.platform.TestMaxDbPlatform;
import org.apache.ddlutils.platform.TestMcKoiPlatform;
import org.apache.ddlutils.platform.TestMySqlPlatform;
import org.apache.ddlutils.platform.TestOracle8Platform;
import org.apache.ddlutils.platform.TestOracle9Platform;
import org.apache.ddlutils.platform.TestPlatformUtils;
import org.apache.ddlutils.platform.TestPostgresqlPlatform;
import org.apache.ddlutils.platform.TestSapDbPlatform;
import org.apache.ddlutils.platform.TestSybasePlatform;

import junit.framework.Test;
import junit.framework.TestCase;
import junit.framework.TestSuite;

/**
 * Helper class to run all DdlUtils tests.
 * 
 * @author Thomas Dudziak
 * @version $Revision: 289996 $
 */
public class RunAllTests extends TestCase
{
    /**
     * Creates a new instance.
     * 
     * @param name The name of the test case
     */
    public RunAllTests(String name)
    {
        super(name);
    }

    /**
     * Runs the test cases on the commandline using the text ui.
     * 
     * @param args The invocation arguments
     */
    public static void main(String[] args)
    {
        junit.textui.TestRunner.run(suite());
    }

    /**
     * Returns a test suite containing all test cases.
     * 
     * @return The test suite
     */
    public static Test suite()
    {
        TestSuite suite = new TestSuite("Ddlutils tests");

        suite.addTestSuite(TestPlatformUtils.class);
        suite.addTestSuite(TestAxionPlatform.class);
        suite.addTestSuite(TestCloudscapePlatform.class);
        suite.addTestSuite(TestDB2Platform.class);
        suite.addTestSuite(TestDerbyPlatform.class);
        suite.addTestSuite(TestFirebirdPlatform.class);
        suite.addTestSuite(TestHsqlDbPlatform.class);
        suite.addTestSuite(TestInterbasePlatform.class);
        suite.addTestSuite(TestMaxDbPlatform.class);
        suite.addTestSuite(TestMcKoiPlatform.class);
        suite.addTestSuite(TestMSSqlPlatform.class);
        suite.addTestSuite(TestMySqlPlatform.class);
        suite.addTestSuite(TestOracle8Platform.class);
        suite.addTestSuite(TestOracle9Platform.class);
        suite.addTestSuite(TestPostgresqlPlatform.class);
        suite.addTestSuite(TestSapDbPlatform.class);
        suite.addTestSuite(TestSybasePlatform.class);
        
        return suite;
    }
}
