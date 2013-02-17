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
package net.luminis.osgitest.results;

import java.io.BufferedWriter;
import java.io.IOException;
import java.io.OutputStream;
import java.io.OutputStreamWriter;
import java.io.Writer;
import java.lang.reflect.Method;
import java.net.InetAddress;
import java.net.UnknownHostException;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Date;
import java.util.Enumeration;
import java.util.Hashtable;
import java.util.List;
import java.util.Properties;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

import javax.xml.parsers.DocumentBuilder;
import javax.xml.parsers.DocumentBuilderFactory;

import junit.framework.AssertionFailedError;
import junit.framework.Test;
import net.luminis.osgitest.testhelper.OSGiSpec;
import net.luminis.osgitest.testhelper.OSGiVersionSpecs;

import org.apache.tools.ant.BuildException;
import org.apache.tools.ant.taskdefs.optional.junit.JUnitResultFormatter;
import org.apache.tools.ant.taskdefs.optional.junit.JUnitTest;
import org.apache.tools.ant.taskdefs.optional.junit.JUnitTestRunner;
import org.apache.tools.ant.taskdefs.optional.junit.JUnitVersionHelper;
import org.apache.tools.ant.taskdefs.optional.junit.XMLConstants;
import org.apache.tools.ant.util.DOMElementWriter;
import org.apache.tools.ant.util.DateUtils;
import org.apache.tools.ant.util.FileUtils;
import org.w3c.dom.Document;
import org.w3c.dom.Element;
import org.w3c.dom.Text;

/**
 * Prints XML output of the test with OSGi vendor information from Pax Exam to a specified Writer.
 * This class is basically a clone of @link{XMLJUnitResultFormatter} with some modifications to
 * provide Pax Exam info.
 *
 * @see XMLJUnitResultFormatter, FormatterElement
 */
public class PaxExamResultFormatter implements JUnitResultFormatter, XMLConstants {

    private static final double ONE_SECOND = 1000.0;

    /*
     * Constants needed for the xml output. Like node names and attribute names.
     */
    private static final String XML_VERSION_DEF = "<?xml version=\"1.0\" encoding=\"UTF-8\" ?>\n";
    private static final String ATTR_OSGI_VENDOR_NAME = "osgi-vendor-name";
    private static final String ELEMENT_OSGI_SPECS = "osgi-specs";
    private static final String ELEMENT_OSGI_SPEC = "osgi-spec";
    private static final String ATTR_OSGI_SPEC_VERSION = "version";
    private static final String ELEMENT_OSGI_SPEC_SECTIONS = "sections";
    private static final String ELEMENT_OSGI_SPEC_SECTION = "section";

    /*
     * constant for unnnamed testsuites/cases
     */
    private static final String UNKNOWN = "unknown";

    private static DocumentBuilder getDocumentBuilder() {
        try {
            return DocumentBuilderFactory.newInstance().newDocumentBuilder();
        } catch (Exception exc) {
            throw new ExceptionInInitializerError(exc);
        }
    }

    /**
     * The XML document.
     */
    private Document m_doc;
    /**
     * The wrapper for the whole testsuite.
     */
    private Element m_rootElement;
    /**
     * Element for the current test.
     */
    private final Hashtable<Test, Element> m_testElements = new Hashtable<Test, Element>();
    /**
     * tests that failed.
     */
    private final Hashtable<Test, Test> m_failedTests = new Hashtable<Test, Test>();
    /**
     * Timing helper.
     */
    private final Hashtable<Test, Long> m_testStarts = new Hashtable<Test, Long>();
    /**
     * Where to write the log to.
     */
    private OutputStream m_out;

    /** No arg constructor. */
    public PaxExamResultFormatter() {
    }

    /** {@inheritDoc}. */
    public void setOutput(OutputStream out) {
        this.m_out = out;
    }

    /** {@inheritDoc}. */
    public void setSystemOutput(String out) {
        formatOutput(SYSTEM_OUT, out);
    }

    /** {@inheritDoc}. */
    public void setSystemError(String out) {
        formatOutput(SYSTEM_ERR, out);
    }

    /**
     * The whole testsuite started.
     * @param suite the testsuite.
     */
    public void startTestSuite(JUnitTest suite) {
        m_doc = getDocumentBuilder().newDocument();
        m_rootElement = m_doc.createElement(TESTSUITE);
        String n = suite.getName();
        m_rootElement.setAttribute(ATTR_NAME, n == null ? UNKNOWN : n);

        //add the timestamp
        final String timestamp = DateUtils.format(new Date(),
                DateUtils.ISO8601_DATETIME_PATTERN);
        m_rootElement.setAttribute(TIMESTAMP, timestamp);
        //and the hostname.
        m_rootElement.setAttribute(HOSTNAME, getHostname());

        // Output properties
        Element propsElement = m_doc.createElement(PROPERTIES);
        m_rootElement.appendChild(propsElement);
        Properties props = suite.getProperties();
        if (props != null) {
            Enumeration<?> e = props.propertyNames();
            while (e.hasMoreElements()) {
                String name = (String) e.nextElement();
                Element propElement = m_doc.createElement(PROPERTY);
                propElement.setAttribute(ATTR_NAME, name);
                propElement.setAttribute(ATTR_VALUE, props.getProperty(name));
                propsElement.appendChild(propElement);
            }
        }
    }

    /**
     * get the local hostname
     * @return the name of the local host, or "localhost" if we cannot work it out
     */
    private String getHostname()  {
        try {
            return InetAddress.getLocalHost().getHostName();
        } catch (UnknownHostException e) {
            return "localhost";
        }
    }

    /**
     * The whole testsuite ended.
     * @param suite the testsuite.
     * @throws BuildException on error.
     */
    public void endTestSuite(JUnitTest suite) throws BuildException {
        m_rootElement.setAttribute(ATTR_TESTS, Long.toString(suite.runCount()));
        m_rootElement.setAttribute(ATTR_FAILURES, Long.toString(suite.failureCount()));
        m_rootElement.setAttribute(ATTR_ERRORS, Long.toString(suite.errorCount()));
        m_rootElement.setAttribute(ATTR_TIME, Double.toString(suite.getRunTime() / ONE_SECOND));
        if (m_out != null) {
            Writer wri = null;
            try {
                wri = new BufferedWriter(new OutputStreamWriter(m_out, "UTF8"));
                wri.write(XML_VERSION_DEF);
                (new DOMElementWriter()).write(m_rootElement, wri, 0, "  ");
                wri.flush();
            } catch (IOException exc) {
                throw new BuildException("Unable to write log file", exc);
            } finally {
                if ((m_out != System.out) && (m_out != System.err)) {
                    FileUtils.close(wri);
                }
            }
        }
    }

    /**
     * Interface TestListener.
     *
     * <p>A new Test is started.
     * @param t the test.
     */
    public void startTest(Test t) {
        m_testStarts.put(t, Long.valueOf(System.currentTimeMillis()));
    }

    /**
     * Interface TestListener.
     *
     * <p>A Test is finished.
     * @param test the test.
     */
    public void endTest(Test test) {
        if (!m_testStarts.containsKey(test)) {
            startTest(test);
        }

        Element currentTest = null;
        if (!m_failedTests.containsKey(test)) {
            currentTest = m_doc.createElement(TESTCASE);

            addOSGiInfo(currentTest, test);

            /*
             * a TestSuite can contain Tests from multiple classes,
             * even tests with the same name - disambiguate them.
             */
            currentTest.setAttribute(ATTR_CLASSNAME,
                    JUnitVersionHelper.getTestCaseClassName(test));
            m_rootElement.appendChild(currentTest);
            m_testElements.put(test, currentTest);
        } else {
            currentTest = m_testElements.get(test);
        }

        Long l = m_testStarts.get(test);
        currentTest.setAttribute(ATTR_TIME, Double.toString((System.currentTimeMillis() - l.longValue()) / ONE_SECOND));
    }

    /**
     * Extract OSGi info from the testCaseName and puts it as attribute in the testElement
     *
     * @param testElement
     * @param testCaseName
     */
    private void addOSGiInfo(Element testElement, Test test) {
        String testCaseName = JUnitVersionHelper.getTestCaseName(test);
        testElement.setAttribute(ATTR_NAME, UNKNOWN);
        testElement.setAttribute(ATTR_OSGI_VENDOR_NAME, UNKNOWN);

        if(testCaseName != null) {
            /*
             * Split testname in methodname and vendor/version
             * Possibile values are:
             *   - sometest [felix]
             *   - sometest [felix/1.8.0]
             *   - sometest [felix/2.0.0[file:/path/to/definition/file/xml]]
             *
             * ^       - start of input
             * (       - start group 1 (the methodname)
             * [^\\s]+ - all non-space characters
             * )       - end group 1
             *
             * \\s*    - optional spaces
             * \\[     - a literal [
             *
             * (       - start group 2 (the vendor and optional version)
             * [^\\[]+ - retrieve all none [ characters (1 or more)
             * )       - end group 2
             *
             * [\\[]?  - zero or one [
             * .*      - zero or more characters (any)
             * [\\]]?  - zero or one ]
             *
             * \\]     - a literal ]
             * $       - end of input
             */
            String regex = "^([^\\s]+)\\s*\\[([^\\[]+)[\\[]?.*[\\]]?\\]$";
            Pattern pattern = Pattern.compile(regex);
            Matcher matcher = pattern.matcher(testCaseName);

            if (matcher.find()) {
                if(matcher.groupCount() >= 2) {
                    String methodName = matcher.group(1);
                    testElement.setAttribute(ATTR_NAME, methodName);
                    testElement.setAttribute(ATTR_OSGI_VENDOR_NAME, matcher.group(2));

                    String testcaseClassName = JUnitVersionHelper.getTestCaseClassName(test);

                    List<OSGiSpec> osgiSpecs = getOsgiSpecs(testcaseClassName, methodName);
                    Element osgiVersionSpecElements = m_doc.createElement(ELEMENT_OSGI_SPECS);
                    for (OSGiSpec osgiSpec : osgiSpecs) {
                        Element osgiVersionSpecElement = m_doc.createElement(ELEMENT_OSGI_SPEC);
                        osgiVersionSpecElement.setAttribute(ATTR_OSGI_SPEC_VERSION, osgiSpec.version());

                        String[] sections = osgiSpec.sections();
                        Element osgiSectionsElement = m_doc.createElement(ELEMENT_OSGI_SPEC_SECTIONS);
                        for (String section : sections) {
                            Element osgiSectionElement = m_doc.createElement(ELEMENT_OSGI_SPEC_SECTION);
                            osgiSectionElement.setTextContent(section);
                            osgiSectionsElement.appendChild(osgiSectionElement);
                        }
                        if (osgiSectionsElement.getChildNodes().getLength() > 0) {
                            osgiVersionSpecElement.appendChild(osgiSectionsElement);
                        }

                        if (osgiVersionSpecElement.getChildNodes().getLength() > 0) {
                            osgiVersionSpecElements.appendChild(osgiVersionSpecElement);
                        }
                    }

                    if (osgiVersionSpecElements.getChildNodes().getLength() > 0) {
                        testElement.appendChild(osgiVersionSpecElements);
                    }
                }
            }
        }
    }

    /**
     * Returns a list of OSGispec annotations of a specific method. Used to provide osgi specification
     * information in the xml testcase nodes.
     *
     * If no annotations are found, the returned list will be empty.
     * {@link OSGiVersionSpecs} takes precedence over {@link OSGiSpec} if the method
     * contains both annotations.
     *
     * @param testcaseClassName
     * @param methodName
     * @return
     */
    private List<OSGiSpec> getOsgiSpecs(String testcaseClassName, String methodName) {
        List<OSGiSpec> osgiSpecs = new ArrayList<OSGiSpec>();
        try {

            Class<?> testClass = Class.forName(testcaseClassName);
            Method testMethod = testClass.getMethod(methodName, new Class[0]);

            OSGiVersionSpecs osgiVersionSpecs = testMethod.getAnnotation(OSGiVersionSpecs.class);
            if (osgiVersionSpecs != null) {
                osgiSpecs.addAll(Arrays.asList(osgiVersionSpecs.value()));
            }
            else {
                OSGiSpec osgiSpec = testMethod.getAnnotation(OSGiSpec.class);
                if (osgiSpec != null) {
                    osgiSpecs.add(osgiSpec);
                }
            }
        }
        /**
         * If something goes wrong with reflection or so, results should still be
         * formatted and returned. So error are ignored and an empty list is returned.
         */
        catch (Throwable ignore) { }

        return osgiSpecs;
    }

    /**
     * Interface TestListener for JUnit &lt;= 3.4.
     *
     * <p>A Test failed.
     * @param test the test.
     * @param t the exception.
     */
    public void addFailure(Test test, Throwable t) {
        formatError(FAILURE, test, t);
    }

    /**
     * Interface TestListener for JUnit &gt; 3.4.
     *
     * <p>A Test failed.
     * @param test the test.
     * @param t the assertion.
     */
    public void addFailure(Test test, AssertionFailedError t) {
        /**
         * If t isn't cast to a Throwable, JUnit will throw a StackOverflowError.
         * TODO Find out exactly why this happens.
         */
        addFailure(test, (Throwable) t);
    }

    /**
     * Interface TestListener.
     *
     * <p>An error occurred while running the test.
     * @param test the test.
     * @param t the error.
     */
    public void addError(Test test, Throwable t) {
        formatError(ERROR, test, t);
    }

    private void formatError(String type, Test test, Throwable t) {
        Element nested = m_doc.createElement(type);
        Element currentTest = m_rootElement;

        if (test != null) {
            endTest(test);
            m_failedTests.put(test, test);
            currentTest = m_testElements.get(test);
        } else {
            currentTest = m_rootElement;
        }

        currentTest.appendChild(nested);

        String message = t.getMessage();
        if ((message != null) && (message.length() > 0)) {
            nested.setAttribute(ATTR_MESSAGE, t.getMessage());
        }
        nested.setAttribute(ATTR_TYPE, t.getClass().getName());

        String strace = JUnitTestRunner.getFilteredTrace(t);
        Text trace = m_doc.createTextNode(strace);
        nested.appendChild(trace);
    }

    private void formatOutput(String type, String output) {
        Element nested = m_doc.createElement(type);
        m_rootElement.appendChild(nested);
        nested.appendChild(m_doc.createCDATASection(output));
    }
}
