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
package net.luminis.osgitest.ant;

import java.io.BufferedWriter;
import java.io.File;
import java.io.FileWriter;
import java.io.IOException;
import java.util.Iterator;
import java.util.Vector;

import net.luminis.osgitest.testhelper.TestBase;

import org.apache.tools.ant.BuildException;
import org.apache.tools.ant.DirectoryScanner;
import org.apache.tools.ant.taskdefs.optional.junit.FormatterElement;
import org.apache.tools.ant.taskdefs.optional.junit.JUnitTask;
import org.apache.tools.ant.types.FileSet;
import org.apache.tools.ant.types.Environment.Variable;

/**
 * Ant task to run tests in the OSGi testing framework. This task is based on ants own {@link JUnitTasl} but with
 * extra properties designed to instruct the testing framework.
 *
 * The extra options are:
 * - vendors: {@link OSGiTestTask.setVendors()}
 * - platformDefinitionDir: {@link OSGiTest.setPlatformDefinitionDir()}
 * - externalJar: {@link OSGiTest.setExternalJar()}
 *
 */
public class OSGiTestTask extends JUnitTask {

    public static String FORMATTER_CLASSNAME = "net.luminis.osgitest.results.PaxExamResultFormatter";
    public static String FORMATTER_EXTENSION = ".xml";

    private final Vector<TestDependency> m_testDependencies = new Vector<TestDependency>();

    public OSGiTestTask() throws Exception {
        super();
    }

    /**
     * Initialize the task.
     */
    @Override
    public void init() throws BuildException {
        super.init();

        super.setFork(true);

        SummaryAttribute summaryAttribute = new SummaryAttribute();
        summaryAttribute.setValue("true");
        setPrintsummary(summaryAttribute);

        super.setHaltonerror(false);

        FormatterElement fe = new FormatterElement();
        fe.setClassname(FORMATTER_CLASSNAME);
        fe.setExtension(FORMATTER_EXTENSION);
        addFormatter(fe);
        log("Init");
    }

    /**
     * Sets the directory where the definition files for custom OSGi frameworks are stored.
     *
     * Note: Prefix local directories with "file:" (eg: file:/definition/dir/)
     *
     * @param definitionDir
     */
    public void setPlatformDefinitionDir(String definitionDir) {
        Variable vendorVar = new Variable();
        vendorVar.setKey(TestBase.PROP_FRAMEWORK_DEFINITION_DIR);
        vendorVar.setValue(definitionDir);

        addConfiguredSysproperty(vendorVar);
    }

    /**
     * Sets the list of vendors with (optional) version that should be tested.
     *
     * Can be a comma separated list with version after a / or "all" for all available
     * vendors.
     *
     * Example1: felix/1.8.0, equinox/3.5.0, knopflerfish
     * Example2: all
     *
     * If Pax Runner doesn't support a framework, this method looks in the platformDefinition
     * directory for framework definitions.
     *
     * Example:
     * Let's say felix/2.0.1 isn't supported in Pax runner, this method looks for a definition file in:
     * file:/definition/dir/felix/2.0.1.xml
     *
     * @see OSGiTestTask#setPlatformDefinitionDir(String)
     * @param vendors
     */
    public void setVendors(String vendors) {
        Variable vendorVar = new Variable();
        vendorVar.setKey(TestBase.PROP_FRAMEWORK_VENDORS);
        vendorVar.setValue(vendors);

        addConfiguredSysproperty(vendorVar);
    }

    /**
     * Set the VMoption, used to pass remote debug information to the pax exam testcontainer.
     *
     * @param vmOption
     */
    public void setPaxVmOption(String vmOption) {
        Variable vmOptionVar = new Variable();
        vmOptionVar.setKey(TestBase.PROP_VM_OPTION);
        vmOptionVar.setValue(vmOption);

        addConfiguredSysproperty(vmOptionVar);
    }

//    /**
//     * Sets the location of a jar file that should be included in the testbundle created by Pax Exam.
//     *
//     * @param externalJar
//     */
//    public void setExternalJar(String externalJar) {
//        Variable externalJarVar = new Variable();
//        externalJarVar.setKey(TestBase.PROP_EXTERNAL_JAR);
//        externalJarVar.setValue(externalJar);
//
//        addConfiguredSysproperty(externalJarVar);
//    }

    /**
     * Set the fork mode of the tests. Setting this will throw an Exception since fork-mode should always be true if the
     * framework should work.
     *
     * @param fork
     */
    @Override
    public void setFork(boolean fork) {
        throw new BuildException("Fork attribute doesn't accept a custom value. Default is true.", getLocation());
    }

    public TestDependency createTestDependency() {
        TestDependency dependency = new TestDependency();
        m_testDependencies.add(dependency);
        return dependency;
    }

    /**
     * Executes the ant task
     */
    @Override
    public void execute() {
        initTestDependencyConfig();

        super.execute();
    }

    /**
     * Reads the nested TestDependency filesets and writes the filenames in a temporary configuration file.
     * The path to this file is passed to TestBase JUnit test with as a system property.
     *
     * @throws BuildException
     */
    private void initTestDependencyConfig() throws BuildException {
        try {
            File tempFile = File.createTempFile("osgitest-testbundle-dependencies", ".conf");
            tempFile.deleteOnExit();

            String filename = tempFile.getAbsolutePath();
            BufferedWriter testFileWriter = new BufferedWriter(new FileWriter(tempFile));

            for (Iterator<TestDependency> itDependency = m_testDependencies.iterator(); itDependency.hasNext(); ) {
                TestDependency dependency = itDependency.next();

                Vector<FileSet> dependencies = dependency.getDependecies();

                for (Iterator<FileSet> itFileset = dependencies.iterator(); itFileset.hasNext();) {
                    FileSet fileset = itFileset.next();
                    DirectoryScanner ds = fileset.getDirectoryScanner();
                    File baseDir = ds.getBasedir();

                    String[] includedFiles = ds.getIncludedFiles();
                    for (String includedFile : includedFiles) {
                        File file = new File(baseDir, includedFile);
                        testFileWriter.write(file.getAbsolutePath()+"\n");
                    }
                }

            }

            Variable testDependencyFile = new Variable();
            testDependencyFile.setKey(TestBase.PROP_TEST_DEPENDENCY_FILE);
            testDependencyFile.setValue(filename);
            addConfiguredSysproperty(testDependencyFile);

            testFileWriter.close();
        } catch (IOException e) {
            throw new BuildException(e);
        }
    }

    public class TestDependency {
        private final Vector<FileSet> m_dependecies = new Vector<FileSet>();

        public void addFileset(FileSet fileset) {
            m_dependecies.add(fileset);
        }

        public Vector<FileSet> getDependecies() {
            return m_dependecies;
        }
    }

}
