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

import java.io.ByteArrayInputStream;
import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.io.InputStream;
import java.util.ArrayList;
import java.util.Collection;
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.jar.Attributes;
import java.util.jar.JarEntry;
import java.util.jar.JarOutputStream;
import java.util.jar.Manifest;

import org.osgi.framework.Bundle;
import org.osgi.framework.Constants;

/*
 * Utility classes to make using generateBundle easier.
 */

/**
 * A BundleSpecifier is used a generic construct for storing information about bundles.
 * It is used in BundleUtil to generate bundles based on the information in this
 * class. Creation of BundleSpecifiers is possible by using BundleUtil's factory-
 * method (createBundleSpecifier), after which the various set... and add... statements
 * can be chained.
 */
public class BundleSpecifier {
    private String m_name = "";
    private String m_fragmentHostname;

    private final List<ImportPackage> m_imports = new ArrayList<ImportPackage>();
    private final List<ExportPackage> m_exports = new ArrayList<ExportPackage>();
    private final Map<String, Object> m_toPack = new HashMap<String, Object>();
    private final List<String> m_classPath = new ArrayList<String>();

    private final String m_bundlePrefix;
    private final Class m_genericActivator;
    private final String m_defaultPackages;

    /**
     * This constructor is intended to be used by BundleUtil only.
     * @param name The name of the bundle to be created.
     * @param bundlePrefix A prefix for the bundle's name.
     * @param genericActivator A generic activator to be packed.
     * @param defaultPackages A string of default packages to be always imported.
     */
    BundleSpecifier(String name, String bundlePrefix, Class genericActivator, String defaultPackages) {
        m_bundlePrefix = bundlePrefix;
        m_defaultPackages = defaultPackages;
        m_genericActivator = genericActivator;
        pack(m_genericActivator);
        setName(name);
    }

    /**
     * Helper function to generate a jar entry name for a class.
     * @param c The class.
     * @return A string containing the correct jar entry name.
     */
    private String getClassJarName(Class c) {
        return c.getName().replace('.', '/') + ".class";
    }

    /**
     * Sets the name of bundle. Use of the constructor is preferred.
     * @param name
     * @return This object, allowing chaining.
     */
    public BundleSpecifier setName(String name) {
        if (name != null) {
            m_name = name;
        }
        else {
            m_name = "";
        }
        return this;
    }

    /**
     * Adds an ImportPackage to this bundle, for in Import-Package statements.
     * @param p The ImportPackage
     * @return This object, allowing chaining.
     * @see ImportPackage
     */
    public BundleSpecifier addImport(ImportPackage p) {
        if (p != null) {
            m_imports.add(p);
        }
        return this;
    }

    /**
     * Adds an ExportPackage to this bundle, for in Export-Package statements.
     * @param p The ExportPackage
     * @return This object, allowing chaining.
     * @see ExportPackage
     */
    public BundleSpecifier addExport(ExportPackage p) {
        if (p != null) {
            m_exports.add(p);
        }
        return this;
    }

    /**
     * Sets the fragment host for this bundle using a bundle
     * as host identifier.
     * @param bundle The bundle that should be assigned as fragment host.
     * @return This object, allowing chaining.
     */
    public BundleSpecifier setFragmentHost(Bundle bundle) {
        return setFragmentHost(bundle.getSymbolicName());
    }

    /**
     * Sets the fragment host for this bundle using a symbolic name
     * as host identifier.
     * @param hostSymbolicName The symbolic name of the bundle that should
     * be assigned as fragment host.
     * @return This object, allowing chaining.
     */
    public BundleSpecifier setFragmentHost(String hostSymbolicName) {
        m_fragmentHostname = hostSymbolicName;
        return this;
    }

    /**
     * Adds a class to be packed in the bundle.
     * Included for backward compatibility.
     * @param c The class to be packed.
     * @return This object, allowing chaining.
     */
    public BundleSpecifier addClass(Class c) {
        return pack(c);
    }

    /**
     * Adds a class to be packed in the bundle.
     * @param c The class to be packed.
     * @return This object, allowing chaining.
     */
    public BundleSpecifier pack(Class c) {
        return pack(getClassJarName(c), c);
    }

    /**
     * Adds another BundleSpecifier whose resulting jar should
     * be packed in this bundle.
     * @param bs The bundle specifier to be packed.
     * @param onClassPath Specifies whether or not the packed bundle should be on
     * this bundle's classpath.
     * @return This object, allowing chaining.
     */
    public BundleSpecifier pack(BundleSpecifier bs, boolean onClassPath) {
        if (onClassPath) {
            m_classPath.add(bs.getJarName());
        }
        return pack(bs.getJarName(), bs);
    }

    /**
     * Adds an InputStream, whose contents should be packed
     * in this bundle. The contents of the stream will be copied
     * immediately.
     * @param is The InputStream specifier to be packed.
     * @return This object, allowing chaining.
     * @throws IOException Is thrown when is is closed for some reason while we are
     * still reading from it.
     */
    public BundleSpecifier pack(String name, InputStream is) throws IOException {
        return pack(name, toByteArrayOutputStream(is));
    }

    /**
     * Packs an object to be included in the jar. This method
     * is too generic for outside use; use the specific versions
     * for given classes instead.
     * @param o The object specifier to be packed.
     * @return This object, allowing chaining.
     */
    private BundleSpecifier pack(String name, Object o) {
        if (o != null) {
            m_toPack.put(name, o);
        }
        return this;
    }


    public BundleSpecifier includeDotOnClasspath(boolean include) {
        if (include) {
            m_classPath.add(".");
        }
        else {
            m_classPath.remove("."); // This string gets interned, so that should go right.
        }
        return this;
    }
    /**
     * Returns the name of the bundle, including a prefix.
     * @return the name of the bundle, including a prefix.
     */
    public String getName() {
        return m_bundlePrefix + m_name;
    }

    /**
     * Returns the name of jar which 'contains' this bundle.
     * @return
     */
    public String getJarName() {
        return getName() + ".jar";
    }

    /**
     * Returns an array of the classes to be packed.
     * Included for backward compatibility.
     * @return an array of the classes to be packed.
     */
    public Class[] getClasses() {
        Set<Class> toReturn = new HashSet<Class>();
        for (Map.Entry<String, Object> e : m_toPack.entrySet()) {
            if (e.getValue() instanceof Class) {
                toReturn.add((Class) e.getValue());
            }
        }
        return toReturn.toArray(new Class[] {});
    }

    /**
     * Creates an output stream out of a given object, depending on its type.
     * @param o The object to be packed.
     * @return The output stream contain a binary representation of the object.
     * @throws IOException Thrown when the object cannot be built into a stream
     * for some reason.
     * @throws IllegalArgumentException The object is currently limited to three
     * types of object, Class, BundleSpecifier and ByteArrayOutputStream.
     */
    private ByteArrayOutputStream objectToStream(Object o) throws IOException {
        Set<ByteArrayInputStream> streams = new HashSet<ByteArrayInputStream>();
        if (o instanceof Class) {
            Class c = (Class) o;
            ClassLoader loader = c.getClassLoader();
            if (loader == null) {
                loader = ClassLoader.getSystemClassLoader();
            }
            InputStream in = loader.getResourceAsStream(c.getName().replace('.', '/') + ".class");
            return toByteArrayOutputStream(in);

        }
        else if (o instanceof BundleSpecifier) {
            BundleSpecifier bs = (BundleSpecifier) o;
            return bs.toOutputStream();
        }
        else if (o instanceof ByteArrayOutputStream) {
            return (ByteArrayOutputStream) o;
        }
        else {
            throw new IllegalArgumentException("o should be of class Class, BundleSpecifier or ByteArrayOutputStream.");
        }
    }

    /**
     * Packs the contents of this bundle specifier into a jar,
     * and returns that as an input stream.
     * @return An output stream containing this specifier's bundle.
     * @throws IOException Is thrown if one of the contents that was
     * marked to be packed cannot be put in properly.
     */
    public ByteArrayInputStream toInputStream() throws IOException {
        return new ByteArrayInputStream(toOutputStream().toByteArray());
    }

    /**
     * Packs the contents of this bundle specifier into a jar,
     * and returns that as an output stream.
     * @return An output stream containing this specifier's bundle.
     * @throws IOException Is thrown if one of the contents that was
     * marked to be packed cannot be put in properly.
     */
    private ByteArrayOutputStream toOutputStream() throws IOException {
        ByteArrayOutputStream outputStream = new ByteArrayOutputStream();

        //create the jar outputstream
        JarOutputStream out = new JarOutputStream(outputStream, getManifest());

        for (Map.Entry<String, Object> e : m_toPack.entrySet()) {
            //create jar_entry
            JarEntry entry = new JarEntry(e.getKey());

            //put the entry in!
            out.putNextEntry(entry);

            out.write(objectToStream(e.getValue()).toByteArray());

            out.closeEntry();
        }
        out.close();

        return outputStream;
    }

    /**
     * Helper method to copy the contents of a stream to a local byte array.
     * This allows handling of all exceptions now.
     * @param in The InputStream to be copied in.
     * @return A ByteArrayInputStream containing the information that was passed
     * into the input stream.
     * @throws IOException Occurs when the input stream gets closed while reading from it.
     */
    private ByteArrayOutputStream toByteArrayOutputStream(InputStream in) throws IOException {
        ByteArrayOutputStream bos = new ByteArrayOutputStream();
        byte[] buf = new byte[1024];
        while (true) {
            int reads = in.read(buf, 0, buf.length);
            if (reads <= 0) {
                break;
            }
            bos.write(buf, 0, reads);
        }
        return bos;
    }

    /**
     * Generates a manifest, based on the information in this object,
     * describing the bundle.
     * @return The bundle's manifest.
     */
    public Manifest getManifest() {
        // Create a general manifest.
        Manifest manifest = new Manifest();
        Attributes attributes = manifest.getMainAttributes();
        attributes.putValue(Attributes.Name.MANIFEST_VERSION.toString(), "1");
        attributes.putValue(Constants.BUNDLE_MANIFESTVERSION, "2");

        //set the names
        attributes.putValue(Constants.BUNDLE_NAME, m_bundlePrefix + m_name);
        attributes.putValue(Constants.BUNDLE_SYMBOLICNAME, m_bundlePrefix + m_name);

        // Build the import statements.
        attributes.putValue(Constants.IMPORT_PACKAGE, createPackageStatements(m_imports.toArray(new ImpExPackage[] {}), true));

        // Build the export and uses statements
        if (m_exports.size() > 0) {
            attributes.putValue(Constants.EXPORT_PACKAGE, createPackageStatements(m_exports.toArray(new ImpExPackage[] {}), false));
        }

        // Build the classpath, if necessary.
        if (m_classPath.size() > 0) {
            attributes.putValue(Constants.BUNDLE_CLASSPATH, ImpExPackage.generateMultiStatement(m_classPath));
        }

        // Set fragment host
        if (m_fragmentHostname != null) {
            attributes.putValue(Constants.FRAGMENT_HOST, m_fragmentHostname);
        }
        else {
            //Add the activator.
            attributes.putValue(Constants.BUNDLE_ACTIVATOR, m_genericActivator.getName());
        }

        return manifest;
    }

    /**
     * Helper function: takes an array of import- or export packages, and asks puts
     * all their statements in a comma-separated string, possibly prefixed by the
     * default packages.
     * @param packs
     * @param includeDefaults
     * @return
     */
    private String createPackageStatements(ImpExPackage[] packs, boolean includeDefaults) {
        StringBuilder statement = new StringBuilder(includeDefaults ? m_defaultPackages : "");
        if ((packs != null) && (packs.length > 0)) {
            for (ImpExPackage p : packs) {
                if (statement.length() > 0) {
                    statement.append(", ");
                }
                statement.append(p.getStatement());
            }
        }
        return statement.toString();
    }

    /**
     * ImpExPackage is the base for ImportPackage and ExportPackage, packing the common behavior
     * (using a package, and specifying attributes including a version).
     */
    public abstract static class ImpExPackage {
        private Package m_pack;
        private final Map<String, String> m_attributes = new HashMap<String, String>();

        /**
         * Creates a package dependency.
         * @param p The package to depend on.
         */
        protected ImpExPackage(Package p) {
            setPackage(p);
        }

        /**
         * Sets the package for this dependency.
         * @param p The package to depend on.
         * @return This object, allowing chaining.
         */
        protected ImpExPackage setPackage(Package p) {
            m_pack = p;
            return this;
        }

        /**
         * Sets the package version for this dependency.
         * @param version The package version to depend on.
         * @return This object, allowing chaining.
         */
        protected ImpExPackage setVersion(String version) {
            addAttribute("version", version);
            return this;
        }

        /**
         * Adds an attribute to this statements attribute list.
         * @param attribute The attribute key.
         * @param value The attribute value.
         * @return This object, allowing chaining.
         */
        protected ImpExPackage addAttribute(String attribute, String value) {
            if ((attribute != null) && (value != null)) {
                m_attributes.put(attribute, value);
            }
            return this;
        }

        /**
         * Builds the basic part of a package statement, consisting of at least the package name,
         * with possibly a set of attributes.
         * @return The basic package statement.
         */
        protected String getStatement() {
            StringBuilder statement = new StringBuilder(m_pack.getName());
            if (m_attributes.size() > 0) {
                statement.append("; " + generateMultiStatement(m_attributes));
            }
            return statement.toString();
        }

        /**
         * Helper method which takes a map of attributes, and build a comma-separated key=value string out of it.
         * @param attributes A non-null map of attributes.
         * @return A string representation of attributes.
         */
        protected static String generateMultiStatement(Map<String, String> attributes) {
            StringBuilder statement = new StringBuilder();
            for (Map.Entry<String, String> e : attributes.entrySet()) {
                if (statement.length() > 0) {
                    statement.append(", ");
                }
                statement.append(e.getKey() + "=");
                if (e.getValue().contains(",") || e.getValue().contains(";") || e.getValue().contains(" ")) {
                    statement.append("\"" + e.getValue() + "\"");
                }
                else {
                    statement.append(e.getValue());
                }
            }
            return statement.toString();
        }

        /**
         * Helper method which takes a set of strings and builds a comma-separated string out of it.
         * @param values A non-null map of attributes.
         * @return A string representation of attributes.
         */
        protected static String generateMultiStatement(Collection<String> values) {
            StringBuilder statement = new StringBuilder();
            for (String e : values) {
                if (statement.length() > 0) {
                    statement.append(", ");
                }
                statement.append(e);
            }
            return statement.toString();
        }
    }

    /**
     * Represents an ImportPackage statement, containing a package name, possibly a set of attributes, possibly a set of
     * mandatory attributes, and possibly a 'resolution:=optional' statement.
     */
    public static class ImportPackage extends ImpExPackage {
        private boolean m_resolutionOptional = false;

        /**
         * Creates a package dependency.
         * @param p The package to depend on.
         */
        ImportPackage(Package p) {
            super(p);
        }

        public ImportPackage setResolutionOptional(boolean optional) {
            m_resolutionOptional = optional;
            return this;
        }

        /**
         * Generates an Import-Package statement.
         */
        @Override
        public String getStatement() {
            StringBuilder statement = new StringBuilder(super.getStatement());

            if (m_resolutionOptional) {
                statement.append("; " + "resolution:=optional");
            }

            return statement.toString();
        }

        /*
         * Exposed methods of the superclass, included to make chaining possible.
         */

        /**
         * Sets the package for this dependency.
         * @param p The package to depend on.
         * @return This object, allowing chaining.
         */
        @Override
        public ImportPackage setPackage(Package p) {
            return (ImportPackage) super.setPackage(p);
        }

        /**
         * Sets the package version for this dependency.
         * @param version The package version to depend on.
         * @return This object, allowing chaining.
         */
        @Override
        public ImportPackage setVersion(String version) {
            return (ImportPackage) super.setVersion(version);
        }

        /**
         * Adds an attribute to this statements attribute list.
         * @param attribute The attribute key.
         * @param value The attribute value.
         * @return This object, allowing chaining.
         */
        @Override
        public ImportPackage addAttribute(String attribute, String value) {
            return (ImportPackage) super.addAttribute(attribute, value);
        }
    }

    /**
     * Represents an ExportPackage statement, containing a package name, possibly a set of attributes, and possibly a set of
     * 'uses:=' constraints.
     */
    public static class ExportPackage extends ImpExPackage {
        private final Set<String> m_uses = new HashSet<String>();
        private final Set<String> m_mandatory = new HashSet<String>();

        /**
         * Creates a package dependency.
         * @param p The package to depend on.
         */
        ExportPackage(Package p) {
            super(p);
        }

        /**
         * Adds a uses-dependency to this export.
         * @param p The package for 'uses:='
         * @return
         */
        public ExportPackage addUses(Package p) {
            if (p != null) {
                m_uses.add(p.getName());
            }
            return this;
        }

        /**
         * Adds an attribute to this dependency's attribute list, stating whether or not it is mandatory.
         * @param attribute The attribute name.
         * @param value The attribute's valuel.
         * @param mandatory True if this attribute should be marked as mandatory, false if not.
         */
        public ExportPackage addAttribute(String attribute, String value, boolean mandatory) {
            if ((attribute != null) && (value != null)) {
                if (mandatory) {
                    m_mandatory.add(attribute);
                }
                else {
                    m_mandatory.remove(attribute);
                }
            }
            return addAttribute(attribute, value);
        }

        /**
         * Generates an Export-Package statement.
         */
        @Override
        public String getStatement() {
            StringBuilder statement = new StringBuilder(super.getStatement());

            if (m_mandatory.size() > 0) {
                statement.append("; mandatory:=\"" + generateMultiStatement(m_mandatory) + "\"");
            }
            if (m_uses.size() > 0) {
                statement.append("; uses:=\"" + generateMultiStatement(m_uses) + "\"");
            }

            return statement.toString();
        }

        /*
         * Exposed methods of the superclass, included to make chaining possible.
         */

        /**
         * Sets the package for this dependency.
         * @param p The package to depend on.
         * @return This object, allowing chaining.
         */
        @Override
        public ExportPackage setPackage(Package p) {
            return (ExportPackage) super.setPackage(p);
        }

        /**
         * Sets the package version for this dependency.
         * @param version The package version to depend on.
         * @return This object, allowing chaining.
         */
        @Override
        public ExportPackage setVersion(String version) {
            return (ExportPackage) super.setVersion(version);
        }

        /**
         * Adds an attribute to this statements attribute list.
         * @param attribute The attribute key.
         * @param value The attribute value.
         * @return This object, allowing chaining.
         */
        @Override
        public ExportPackage addAttribute(String attribute, String value) {
            return (ExportPackage) super.addAttribute(attribute, value);
        }
    }

}