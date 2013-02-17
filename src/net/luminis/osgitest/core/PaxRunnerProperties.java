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
package net.luminis.osgitest.core;

import java.io.IOException;
import java.io.InputStream;
import java.net.URL;
import java.util.Properties;

import org.ops4j.lang.NullArgumentException;
import org.ops4j.pax.exam.Option;
import org.ops4j.pax.exam.junit.Configuration;
import org.ops4j.pax.runner.Run;

/**
 * Helper class to retrieve Pax Runner options before Pax Runner is launched. (and it's options are available)
 *
 * IMPORTAND: These options aren't reliable if you don't pass the property file (accessible by {@link PaxRunnerProperties#getPropertiesFile()})
 * to Pax Runner with the --config option, or to Pax Exam by passing PaxRunnerOptions.rawPaxRunnerOption("--config", PaxRunnerProperties.getPropertiesFile())
 * to the Pax Exam {@link Option}s array returned by the JUnit method annotated with {@link Configuration}
 *
 * @see PaxRunnerOptions#rawPaxRunnerOption(String, String);
 * @see Configuration
 *
 */
public class PaxRunnerProperties {
    /**
     * The protocol used to access classpath files. (eg classpath:META-INF/some-file)
     * Needed since default properties of Pax Runner are accessed through the classpath.
     */
    public static final String CLASSPATH_PROTOCOL = "classpath";
    /**
     * Default Pax runner properties file. This file is normally defined in
     * {@link Run#start(org.ops4j.pax.runner.CommandLine, org.ops4j.pax.runner.Configuration, org.ops4j.pax.runner.OptionResolver)}
     * Since we want to use the properties of this file, before Pax Runner is launched (en the properties are loaded) it's defined
     * here.
     */
    public static final String DEFAULT_PAX_RUNNER_PROPERTIES_FILE = "classpath:META-INF/runner.properties";
    /**
     * The name of the system property used to define a custom Pax Runner properties file.
     */
    public static final String PROP_PAX_RUNNER_PROPERTY_FILE = "net.luminis.osgitest.pax.runner.propertiesfile";

    /**
     * Helper class. Not necessary to create an instance.
     */
    private PaxRunnerProperties() { }

    /**
     * Load the properties, pax runner (will) use.
     * @return
     */
    public static Properties load() {
        return loadPropertiesFile(getPropertiesFile());
    }

    /**
     * Load the properties from a specific file.
     * It can use a normal file like /tmp/normal-file.properties or a file in the classpath formatted as
     * classpath:META-INF/classpath-file.properties.
     *
     * @param url
     * @return
     */
    private static Properties loadPropertiesFile(String url) {
        NullArgumentException.validateNotEmpty(url, "Configuration url");
        Properties properties = new Properties();
        InputStream inputStream;
        try {
            if (url.startsWith(CLASSPATH_PROTOCOL)) {
                String actualConfigFileName = url.split( ":" )[ 1 ];
                NullArgumentException.validateNotEmpty(actualConfigFileName, "configuration file name");
                inputStream = Thread.currentThread().getContextClassLoader().getResourceAsStream(actualConfigFileName);
            }
            else {
                inputStream = new URL(url).openStream();
            }
            NullArgumentException.validateNotNull(inputStream, String.format("Couldn't find url [%s]", url));

            properties.load(inputStream);

            return properties;
        }
        catch(IOException e) {
            throw new IllegalArgumentException(String.format("Couldn't load configuration from url [%s]", url), e);
        }
    }

    /**
     * Get properties file Pax Runner should use.
     * This file can be defined in the system property named {@link PaxRunnerProperties#PROP_PAX_RUNNER_PROPERTY_FILE}.
     * If not, the {@link DEFAULT_PAX_RUNNER_PROPERTIES_FILE} is used as default.
     * @return
     */
    public static String getPropertiesFile() {
        return System.getProperty(PROP_PAX_RUNNER_PROPERTY_FILE, DEFAULT_PAX_RUNNER_PROPERTIES_FILE);
    }
}
