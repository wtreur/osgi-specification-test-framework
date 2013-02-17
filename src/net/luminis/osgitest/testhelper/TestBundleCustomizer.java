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

import java.io.File;
import java.io.FileInputStream;
import java.io.FileNotFoundException;
import java.io.InputStream;
import java.util.ArrayList;
import java.util.Collection;

import org.ops4j.pax.exam.Customizer;
import org.ops4j.pax.swissbox.tinybundles.core.TinyBundle;
import org.ops4j.pax.swissbox.tinybundles.core.TinyBundles;

/**
 * Class to customize the testbundle.
 * The class is able to embed extra jars in the testbundle and put them on the
 * Bundle-Classpath
 *
 */
public class TestBundleCustomizer extends Customizer {

    /**
     * Collection of embedded jars
     */
    private final Collection<String> m_embeddedJars = new ArrayList<String>();

    /**
     * Add a jar to the list of embedded jars.
     *
     * @param name The absolute path to the jar
     */
    public void addEmbeddedJar(String name) {
        m_embeddedJars.add(name);
    }

    /**
     * Customizes the TestBundle.
     * Packs the jars and put them on the Bundle-Classpath.
     *
     */
    @Override
    public InputStream customizeTestProbe(InputStream testProbe) throws FileNotFoundException {
        TinyBundle tinyBundle = TinyBundles.modifyBundle(testProbe);

        StringBuilder classPath = new StringBuilder(".");

        for (String jarName : m_embeddedJars) {
            File jarFile = new File(jarName);
            String jarFileName = jarFile.getName();

            FileInputStream fileIn = new FileInputStream(jarFile);
            tinyBundle.add(jarFile.getName(), fileIn);
            classPath.append(",")
                .append(jarFileName);
        }

        tinyBundle.set("Bundle-Classpath", classPath.toString());

        return tinyBundle.build();
    }
}
