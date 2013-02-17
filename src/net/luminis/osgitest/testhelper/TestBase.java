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
package net.luminis.osgitest.testhelper;

import static org.ops4j.pax.exam.CoreOptions.options;

import java.io.BufferedReader;
import java.io.File;
import java.io.FileReader;
import java.io.IOException;
import java.util.ArrayList;
import java.util.Properties;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

import net.luminis.osgitest.core.PaxRunnerProperties;

import org.junit.After;
import org.junit.Before;
import org.junit.runner.RunWith;
import org.ops4j.pax.exam.CoreOptions;
import org.ops4j.pax.exam.Inject;
import org.ops4j.pax.exam.Option;
import org.ops4j.pax.exam.container.def.PaxRunnerOptions;
import org.ops4j.pax.exam.junit.Configuration;
import org.ops4j.pax.exam.junit.JUnit4TestRunner;
import org.ops4j.pax.exam.options.CustomFrameworkOption;
import org.ops4j.pax.exam.options.FrameworkOption;
import org.osgi.framework.Bundle;
import org.osgi.framework.BundleContext;
import org.osgi.framework.BundleException;
import org.osgi.service.packageadmin.PackageAdmin;
import org.osgi.util.tracker.ServiceTracker;

/**
 * Base class for writing framework tests with JUnit and Pax Exam.
 */
@RunWith (JUnit4TestRunner.class)
public class TestBase {

    /*
     * Property names that can be set as java system property
     */
    /**
     * VMoption, used to pass remote debug information to the pax exam testcontainer.
     */
    public static final String PROP_VM_OPTION = "net.luminis.osgitest.vmoption";
    /**
     * List of vendors with (optional) version that should be tested.
     */
    public static final String PROP_FRAMEWORK_VENDORS = "net.luminis.osgitest.vendors";
    /**
     * Directory where the definition files for custom frameworks are stored.
     */
    public static final String PROP_FRAMEWORK_DEFINITION_DIR = "net.luminis.osgitest.definitions.dir";
    /**
     * Path to a configuration file containing a list of paths to jar files which will be included in the testbundle.
     * These jar files will end up in the testbundle's Bundle-Classpath as well.
     *
     * The configuration file is a list with absolute filenames separated by line-endings.
     *
     */
    public static final String PROP_TEST_DEPENDENCY_FILE = "net.luminis.osgitest.test.dependencyfile";


    /**
     * The {@link BundleContext} of the test container injected by Pax Exam.
     */
    @Inject
    protected BundleContext m_context;

    /**
     * The PackageAdmin, as provided by the framework.
     */
    protected volatile PackageAdmin m_admin;

    /**
     * A configured {@link BundleUtil}.
     */
    protected volatile BundleUtil m_bu;

    /*
     * Classes we don't want to type, but don't want to import either.
     * These classes can be used to dynamically create bundles that import or
     * export packages.
     */
    @SuppressWarnings("unchecked")
    protected static final Class Foo = net.luminis.osgitest.testhelper.packages.foo.Foo.class;

    protected static final Package fooPackage = Foo.getPackage();

    @SuppressWarnings("unchecked")
    protected static final Class genericActivator = net.luminis.osgitest.testhelper.genericbundle.Activator.class;

    @SuppressWarnings("unchecked")
    protected static final Class FooImpl1 = net.luminis.osgitest.testhelper.packages.foo.FooImpl1.class;

    @SuppressWarnings("unchecked")
    protected static final Class FooImpl2 = net.luminis.osgitest.testhelper.packages.foo.FooImpl2.class;

    @SuppressWarnings("unchecked")
    protected static final Class PInterface1 = net.luminis.osgitest.testhelper.packages.p.PInterface1.class;
    @SuppressWarnings("unchecked")
    protected static final Class PInterface2 = net.luminis.osgitest.testhelper.packages.p.PInterface2.class;
    @SuppressWarnings("unchecked")
    protected static final Class PInterface3 = net.luminis.osgitest.testhelper.packages.p.PInterface3.class;

    @SuppressWarnings("unchecked")
    protected static final Class QInterface1 = net.luminis.osgitest.testhelper.packages.q.QInterface1.class;
    @SuppressWarnings("unchecked")
    protected static final Class QInterface2 = net.luminis.osgitest.testhelper.packages.q.QInterface2.class;

    @SuppressWarnings("unchecked")
    protected static final Class RInterface1 = net.luminis.osgitest.testhelper.packages.r.RInterface1.class;
    @SuppressWarnings("unchecked")
    protected static final Class RInterface2 = net.luminis.osgitest.testhelper.packages.r.RInterface2.class;

    @SuppressWarnings("unchecked")
    protected static final Class SInterface = net.luminis.osgitest.testhelper.packages.s.SInterface.class;

    @SuppressWarnings("unchecked")
    protected static final Class TInterface1 = net.luminis.osgitest.testhelper.packages.t.TInterface1.class;
    @SuppressWarnings("unchecked")
    protected static final Class TInterface2 = net.luminis.osgitest.testhelper.packages.t.TInterface2.class;

    protected static final Package pPackage = net.luminis.osgitest.testhelper.packages.p.PInterface1.class.getPackage();
    protected static final Package qPackage = net.luminis.osgitest.testhelper.packages.q.QInterface1.class.getPackage();
    protected static final Package rPackage = net.luminis.osgitest.testhelper.packages.r.RInterface1.class.getPackage();
    protected static final Package sPackage = net.luminis.osgitest.testhelper.packages.s.SInterface.class.getPackage();
    protected static final Package tPackage = net.luminis.osgitest.testhelper.packages.t.TInterface1.class.getPackage();

    @Before
    public void setUp() throws InterruptedException {
        ServiceTracker tracker = new ServiceTracker(m_context, PackageAdmin.class.getName(), null);
        tracker.open();
        m_admin = (PackageAdmin) tracker.waitForService(5000);
        tracker.close();
        m_bu = new BundleUtil(m_context, m_admin, genericActivator);
    }

    @After
    public void cleanupBundles() {
        for (Bundle b : m_context.getBundles()) {
            if (b.getSymbolicName().startsWith(BundleUtil.BUNDLE_PREFIX)) {
                try {
                    b.uninstall();
                }
                catch (BundleException e) {
                    e.printStackTrace(System.err);
                }
            }
        }
        m_bu.refreshFrameworkAndWait(null);
    }

    /**
     * Configuration setup for every test.
     * @return
     * @throws Exception
     */
    @Configuration
    public static Option[] configuration() throws Exception {
        return options(getFrameworkVendorOptions(),
            PaxRunnerOptions.compendiumProfile(),
            /*
             * Since we manually load a properties file in getFrameworkVendorOption(), pass this
             * file to pax runner, or the vendor support check isn't reliable.
             */
            PaxRunnerOptions.rawPaxRunnerOption("--config", PaxRunnerProperties.getPropertiesFile()),
            getCustomizedBundle(),
            getVmOption(),
            PaxRunnerOptions.vmOption("-ea")); //-ea is needed to catch the assertion errors in the JUnit-tests
    }

    /**
     * Reads framework vendor from system property named in {@link TestBase#PROP_FRAMEWORK_VENDORS}
     * and returns the right {@link Option}.
     * Useful for Pax Exam JUnit {@link Configuration} methods.
     *
     * The system property can be a comma separated list with version after a / or "all" for all available
     * vendors.
     * Example1: felix/1.8.0, equinox/3.5.0, knopflerfish
     * Example2: all
     *
     * If Pax Runner doesn't support a framework, this method looks in the {@link TestBase#PROP_FRAMEWORK_DEFINITION_DIR}
     * directory for framework definitions.
     *
     * Example:
     * Let's say felix/2.0.1 isn't supported in Pax runner, this method looks for a definition file in:
     * file:/definition/dir/felix/2.0.1.xml
     *
     * See documentation for more information and examples.
     *
     * @return
     */
    private static Option getFrameworkVendorOptions() throws Exception {
        String vendors = System.getProperty(PROP_FRAMEWORK_VENDORS);

        if (vendors != null) {
            ArrayList<Option> frameworkOptions = new ArrayList<Option>();

            /*
             * Split the vendor list in comma's, filtering out any whitespace characters.
             * EG: "felix, felix/1.8.0\n\r,equinox/3.5.0"
             *
             * \\s* - search for any white-space character. 0 or more
             * ,?   - a comma, 0 or 1
             * \\s* - any white-space character. 0 or more
             * (    - group start, this is the vendor name (or vendor/version)
             * [    - start sub-pattern
             * ^\\s - any non whitespace character
             * |    - or
             * ^,   - any non , character
             * ]    - end of sub-pattern
             * +    - sub-pattern should occur one or more times
             * )    - end of group
             * \\s* - any white-space character. 0 or more
             * ,?   - a comma, 0 or 1
             * \\s* - any white-space character. 0 or more
             *
             * Thanks to dennisg
             */
            Pattern pattern = Pattern.compile("\\s*,?\\s*([^\\s|^,]+)\\s*,?\\s*");
            Matcher matcher = pattern.matcher(vendors);

            while (matcher.find()) {
                if(matcher.groupCount() > 0) {

                    String frameworkVendor = matcher.group(1);
                    if (frameworkVendor.equals("all")) {
                        return CoreOptions.allFrameworks();
                    }
                    else {
                        Pattern vendorPattern = Pattern.compile("([^/]+)/([^/]+)");
                        Matcher vendorMatcher = vendorPattern.matcher(frameworkVendor);
                        if (vendorMatcher.find()) {
                            String vendorName = vendorMatcher.group(1);
                            String vendorVersion = vendorMatcher.group(2);
                            FrameworkOption frameworkOption = new FrameworkOption(vendorName);

                            if (frameworkSupported(vendorName, vendorVersion)) {
                                frameworkOption.version(vendorVersion);
                            }
                            else {
                                String definitionUrl = new StringBuilder(getDefinitionDir())
                                    .append("/")
                                    .append(vendorName)
                                    .append("/")
                                    .append(vendorVersion)
                                    .append(".xml")
                                    .toString();

                                frameworkOption = new CustomFrameworkOption(definitionUrl,
                                    vendorName,
                                    vendorVersion);
                            }

                            frameworkOptions.add(frameworkOption);
                        }
                        else {
                            FrameworkOption frameworkOption = new FrameworkOption(frameworkVendor);
                            frameworkOptions.add(frameworkOption);
                        }
                    }
                }
            }

            return CoreOptions.composite(frameworkOptions.toArray(new Option[0]));
        }

        return null;
    }


    /**
     * Get the definition dir where custom OSGi-framework definitions are stored.
     *
     * Note: Prefix local directories with "file:" (eg: file:/definition/dir/)
     *
     * @return
     * @throws Exception
     */
    private static String getDefinitionDir() throws Exception {
        String definitionDir = System.getProperty(PROP_FRAMEWORK_DEFINITION_DIR);

        if (definitionDir == null) {
            throw new Exception("Framework definition dir isn't set");
        }

        return definitionDir;
    }

    /**
     * Check if a framework is supported by Pax Runner.
     * This method uses {@link PaxRunnerProperties} to determine if a framework is used. So don't forget to
     * pass it's configuration file to Pax Exam config.
     *
     * @see PaxRunnerProperties
     *
     * @param vendor
     * @param version
     * @return
     */
    private static boolean frameworkSupported(String vendor, String version) {
        Properties properties = PaxRunnerProperties.load();
        String platformProperty = properties.getProperty("platform." + vendor + "." + version);

        return platformProperty != null;
    }


    /**
     * Get the vm option defined in the system property named as {@link TestBase#PROP_VM_OPTION}
     * This can be used to pass remote debug instructions to the Pax Exam testcontainer.
     *
     * @return
     */
    private static Option getVmOption() {
        String vmOptionString = System.getProperty(PROP_VM_OPTION);

        if (vmOptionString != null) {
            return PaxRunnerOptions.vmOption(vmOptionString);
        }

        return null;
    }

    /**
     * Returns a TestBundleCustomizer.
     * The customizer is used to add dependencies to the testbundle and its Bundle-Classpath
     * The dependency information is red from a dependency file set by the Java system
     * property {@link TestBase#PROP_TEST_DEPENDENCY_FILE}
     *
     * @return Option
     */
    private static Option getCustomizedBundle() {
        String testDependencyFileName = System.getProperty(PROP_TEST_DEPENDENCY_FILE);

        if (testDependencyFileName != null) {

            TestBundleCustomizer customizer = new TestBundleCustomizer();

            try {
                File dependencyFile = new File(testDependencyFileName);
                BufferedReader dependecyFileReader = new BufferedReader(new FileReader(dependencyFile));
                String externalJar = null;
                while ( (externalJar = dependecyFileReader.readLine()) != null) {
                    customizer.addEmbeddedJar(externalJar);
                }
                dependecyFileReader.close();

            } catch (IOException e) {
                throw new RuntimeException("Unable to read testbundle dependency configuration file");
            }

            return customizer;
        }

        return null;
    }

}
