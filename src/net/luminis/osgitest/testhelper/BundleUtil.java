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
import java.io.IOException;
import java.lang.reflect.Method;
import java.util.ArrayList;
import java.util.List;

import org.osgi.framework.Bundle;
import org.osgi.framework.BundleContext;
import org.osgi.framework.BundleEvent;
import org.osgi.framework.BundleException;
import org.osgi.framework.BundleListener;
import org.osgi.framework.Constants;
import org.osgi.framework.FrameworkEvent;
import org.osgi.framework.FrameworkListener;
import org.osgi.framework.InvalidSyntaxException;
import org.osgi.framework.ServiceEvent;
import org.osgi.framework.ServiceListener;
import org.osgi.framework.ServiceReference;
import org.osgi.framework.ServiceRegistration;
import org.osgi.service.packageadmin.ExportedPackage;
import org.osgi.service.packageadmin.PackageAdmin;

/**
 * Class BundleUtil is intended for setting up test-scenarios for testing
 * the behavior of an OSGi framework. It can be used by instantiating an
 * object with a BundleContext, PackageAdmin and a class that inherits from
 * BundleActivator. A number of helper functions are available, which can
 * be divided into four broad categories.
 * <li> Scenario setup. Functions that generate or install bundles with
 * given characteristics, such as installImpExBundle.</li>
 * <li> Wiring checks. Functions like getWiredPackage, checkWiring and
 * isProvidingPackages check the status of wiring after a resolve.</li>
 * <li> Service functions. With the wiring done, these functions can be used
 * to register services, or check the visibility of services from within
 * some bundle.</li>
 * <li> General utitlities. Functions like refreshFrameworkAndWait help in writing
 * scenarios easier.</li>
 */
public class BundleUtil {
    BundleContext m_context;
    PackageAdmin m_admin;
    Class genericActivator;

    /** BUNDLE_PREFIX is attached before the (symbolic) name of every bundle,
     * to allow easy removing of bundles created in a test run.
     */
    public static final String BUNDLE_PREFIX = "testCase.";
    /**
     * In the bundle manifest, some default packages are used.
     */
    public static final String DEFAULT_PACKAGES = "org.osgi.framework";

    /**
     * Creates an instance of the bundleUtil.
     * @param bc The BundleContext to use for installing bundles.
     * @param pa The PackageAdmin, used for resolving and checking wiring.
     * @param ga An activator to be packed in every bundle that is created by this class.
     */
    public BundleUtil(BundleContext bc, PackageAdmin pa, Class ga) {
        m_context = bc;
        m_admin = pa;
        genericActivator = ga;
    }

    /*
     * Tools for setting up scenarios
     **********************************/

    /**
     * Installs a bundle optionally exporting a package using given versions, and importing that same package using a given
     * version range, possibly implementing some service.
     *
     * @param name The bundle name, to prevent name clashes and improve usefulness of test-output.
     * @param interfaceClass The class through which this bundle should communicate.
     * @param implClass If any, a class that implements the service provided by interfaceClass. The service will not get
     *        registered, only packed.
     * @param exportedVersion A string array of exported versions. Use null to not export anything, and use an empty array when
     *        no version statement should be included.
     * @param importedVersion A string stating the version range (e.g. [1,2.0) ) which should be imported by this bundle. Use an
     *        empty string for importing without an explicit version, use null to not import anything.
     * @deprecated Use {@link #createBundleSpecifier(String)} and {@link #installBundle(BundleSpecifier)} instead.
     */
    @Deprecated
    public Bundle installImpExBundle(String name, Class interfaceClass, Class implClass, String[] exportedVersion, String importedVersion) throws BundleException {
        return m_context.installBundle(name, generateImpExBundle(name, interfaceClass, implClass, exportedVersion, importedVersion));
    }

    /**
     * Generates a bundle;
     * @see installImpExBundle
     * @deprecated Use {@link #createBundleSpecifier(String)} and {@link #installBundle(BundleSpecifier)} instead.
     */
    @Deprecated
    public ByteArrayInputStream generateImpExBundle(String name, Class<Object> interfaceClass, Class implClass, String[] exportedVersion, String importedVersion) {
        BundleSpecifier bs = createBundleSpecifier(name);

        if (importedVersion == null) {
            //well, do nothing, but we want to check this.
        }
        else if (importedVersion == "") {
            bs.addImport(createImportPackage(interfaceClass.getPackage()));
        }
        else {
            bs.addImport(createImportPackage(interfaceClass.getPackage()).setVersion(importedVersion));
        }


        if (exportedVersion == null) {
            //well, do nothing, but we want to check this.
        }
        else if (exportedVersion.length == 0) {
            bs.addExport(createExportPackage(interfaceClass.getPackage()));
        }
        else {
            for (String version : exportedVersion) {
                bs.addExport(createExportPackage(interfaceClass.getPackage()).setVersion(version));
            }
        }

        bs.addClass(interfaceClass);
        if (implClass != null) {
            bs.addClass(implClass);
        }

        ByteArrayInputStream toReturn = null;
        try {
            toReturn = generateBundle(bs);
        }
        catch (IOException e) {
            // We only put in correct classes, so we can safely suppress
            // this exception.
        }
        // Since the exception will never occur, toReturn can never be null.
        return toReturn;
    }

    /**
     * Installs a bundle based on a bundle specifier.
     * @param bs The bundle specifier.
     * @throws IOException Is thrown when some of the packed elements of the bundle
     * cannot be packed properly.
     */
    public Bundle installBundle(BundleSpecifier bs) throws BundleException, IOException {
        return m_context.installBundle(bs.getName(), generateBundle(bs));
    }

    /**
     * Generates a bundle based on a BundleSpecifier.
     * @param bs The specifier that defines the bundle
     * @return A ByteArrayInputStream containing a binary representation of the bundle.
     * @throws IOException Is thrown when some of the packed elements of the bundle
     * cannot be packed properly.
     */
    public ByteArrayInputStream generateBundle (BundleSpecifier bs) throws IOException {
        return bs.toInputStream();
    }

    /**
     * Creates a new {@link BundleSpecifier} for a bundle with the given name.
     */
    public BundleSpecifier createBundleSpecifier(String name) {
        return new BundleSpecifier(name, BUNDLE_PREFIX, genericActivator, DEFAULT_PACKAGES);
    }

    /**
     * Creates a new {@link BundleSpecifier.ImportPackage} for the given package.
     */
    public BundleSpecifier.ImportPackage createImportPackage(Package p) {
        return new BundleSpecifier.ImportPackage(p);
    }

    /**
     * Creates a new {@link BundleSpecifier.ExportPackage} for the given package.
     */
    public BundleSpecifier.ExportPackage createExportPackage(Package p) {
        return new BundleSpecifier.ExportPackage(p);
    }

    /*
     * Tools for checking wiring
     ***************************/

    /**
     * Finds the ExportedPackage to which the given Bundle is wired through the given Package.
     *
     * @param pack The package to which bundle should be wired.
     * @param bundle The bundle that imports exportedPackage.
     * @return The ExportedPackage to which the Bundle is wired.
     */
    public ExportedPackage getWiredPackage(Package pack, Bundle bundle) {
        ExportedPackage[] packages = m_admin.getExportedPackages(pack.getName());
        if (packages == null) {
            return null;
        }

        for (ExportedPackage p : packages) {
            Bundle[] imported = p.getImportingBundles();
            if (imported != null) {
                for (Bundle b : imported) {
                    if (b.getBundleId() == bundle.getBundleId()) {
                        return p;
                    }
                }
            }
        }
        return null;
    }

    /**
     * Finds the version of the package through which the given bundle is wired to the exporter of the package.
     *
     * @param pack The package to which bundle is wired.
     * @param bundle The bundle that imports pack.
     * @return The version of the package through which the given bundle is wired to the exporter of the package. Returns an
     *         empty string when the package is not exported.
     */
    public String getWiredPackageVersion(Package pack, Bundle bundle) {
        ExportedPackage p = getWiredPackage(pack, bundle);
        if (p == null) {
            return "";
        }
        return p.getVersion().toString();
    }

    /**
     * Checks whether a package is internal to a bundle.
     *
     * @param pack The package
     * @param bundle
     * @return
     */
    public boolean isPrivatePackage(Package pack, Bundle bundle) {
        Bundle exporter = getWiredPackageExporter(pack, bundle);
        if (exporter == null) {
            return true;
        }
        return (exporter.getBundleId() == bundle.getBundleId());
    }

    /**
     * Check whether a package is exported by some bundle.
     *
     * @param pack Package to be checked.
     */
    public boolean isExported(Class pack) {
        return (m_admin.getExportedPackages(pack.getPackage().getName()) != null);
    }

    /**
     * Finds the Bundle which exports the given package to which the given Bundle is wired.
     *
     * @param pack The package to which bundle should be wired.
     * @param bundle The bundle that imports exportedPackage
     * @return The Bundle which exports the given package to which the given Bundle is wired.
     */
    public Bundle getWiredPackageExporter(Package pack, Bundle bundle) {
        ExportedPackage p = getWiredPackage(pack, bundle);
        if (p == null) {
            return null;
        }
        return p.getExportingBundle();
    }

    /**
     * Checks whether a bundle provides a package to some other bundle. Note that being wired to self does not mean this bundle
     * is providing.
     *
     * @param bundle The bundle to be checked.
     */
    public boolean isProvidingPackages(Bundle bundle) {
        ExportedPackage[] packages = m_admin.getExportedPackages(bundle);
        if (packages == null) {
            return false;
        }

        for (ExportedPackage p : packages) {
            Bundle[] importers = p.getImportingBundles();
            if ((importers != null) && (importers.length > 0)) {
                for (Bundle b : importers) {
                    if (b.getBundleId() != bundle.getBundleId()) {
                        return true;
                    }
                }
            }
        }
        return false;
    }

    /**
     * Check the wiring of two bundles using a specific package. Will assert when the wiring is
     * there when it shouldn't, or is different than it should.
     * @param p The package.
     * @param user The importer of the package.
     * @param provider The exporter of the package.
     * @param allowed Whether or not this wire should exist.
     */
    public void checkWiring(Package p, Bundle user, Bundle provider, boolean allowed) {
        Bundle realProvider = getWiredPackageExporter(p, user);
        if (allowed) {
            assert realProvider != null : "Bundle " + user.getSymbolicName() + " is not wired to any bundle using " + p.getName() + ".";
            assert realProvider.getBundleId() == provider.getBundleId() : "The "+p.getName()+" that "+user.getSymbolicName()+ " (id:" +user.getBundleId() + ") uses should be wired to "+provider.getSymbolicName()+" (id:" +provider.getBundleId() + ") (it is wired to " + realProvider.getSymbolicName()+ "( id:" +realProvider.getBundleId() + "))";
        }
        else {
            if (realProvider == null) {
                // If there is no real provider, there cannot be a wire either.
                return;
            }
            assert getWiredPackageExporter(p, user).getBundleId() != provider.getBundleId() : "The "+p.getName()+" that "+user.getSymbolicName()+" uses should be not wired to "+provider.getSymbolicName()+", but it is.";
        }
    }

    public void checkWiring(Package p, Bundle user, Bundle provider) {
        checkWiring(p, user, provider, true);
    }

    /**
     * Checks whether a given class (or interface) is reachable from a given bundle. It will use the bundle's own version of the
     * class, if necessary.
     *
     * @param c The class to check visibility on.
     * @param b The bundle.
     * @return true when the bundle can reach the class, false when not.
     */
    public boolean isReachable(Class c, Bundle b) {
        return isReachable(c.getName(), b);
    }

    /**
     * @see #isReachable(Class, Bundle)
     */
    public boolean isReachable(String c, Bundle b) {
        try {
            b.loadClass(c);
            return true;
        }
        catch (ClassNotFoundException e) {
            return false;
        }
    }

    /*
     * Tools for checking service usability.
     ***************************************/

    /**
     * Registers an implementation with a given interface for a bundle.
     *
     * @param impl The implementation class.
     * @param in The interface class.
     * @param b The bundle.
     * @return Whether this registration succeeded.
     */
    public boolean registerService(Class impl, Class in, Bundle b) {
        return registerService(impl.getName(), in.getName(), b);
    }

    /**
     * @see #registerService(Class, Class, Bundle)
     */
    public boolean registerService(String impl, String in, Bundle b) {
        BundleContext context = getBundleContext(b);
        if (context == null) {
            return false;
        }
        try {
            ServiceRegistration ref = context.registerService(in, b.loadClass(impl).newInstance(), null);
            return (ref != null);
        }
        catch (Exception e) {
            return false;
        }
    }

    /**
     * Checks whether services are visible for a given interface class.
     * @param in The interface to check.
     * @param b The bundle.
     */
    public boolean seesService(Class in, Bundle b) {
        // get services, filter on interface
        ServiceReference[] services = null;
        try {
            assert getBundleContext(b) != null : "Bundle context is null";
            services = getBundleContext(b).getServiceReferences(in.getName(), null);
        }
        catch (InvalidSyntaxException e) {
            // will not happen with a null filter.
        }
        return (services != null);
    }

    /**
     * Checks whether a bundle can use a service implementation it gets served
     * for a given interface.
     * @param in The interface to check.
     * @param b The bundle.
     */
    public boolean canUseService(Class in, Bundle b) {
        // get services, filter on interface
        ServiceReference[] services = null;
        try {
            services = getBundleContext(b).getServiceReferences(in.getName(), null);
        }
        catch (InvalidSyntaxException e) {
            // will not happen with a null filter.
        }
        if (services == null) {
            return false;
        }

        boolean isAssignable = true;
        for (ServiceReference ref : services) {
            try {
                if (!b.loadClass(in.getName()).isAssignableFrom(getBundleContext(b).getService(ref).getClass())) {
                    isAssignable = false;
                }
            }
            catch (ClassNotFoundException e) {
                // If the interface class cannot be found, we can be sure that it is not assignable.
                isAssignable = false;
            }
            getBundleContext(b).ungetService(ref);
        }

        return isAssignable;
    }

    /**
     * Finds the bundlecontext of a bundle. If the bundle is not starting, running or stopping, or if the framework does not
     * support this, null is returned.
     *
     * @param b A bundle.
     * @return The bundle context for b, or null if none can be found.
     */
    private BundleContext getBundleContext(Bundle b) {
        try {
            Method method = b.getClass().getDeclaredMethod("getBundleContext", (Class[])null);
            method.setAccessible(true);
            return (BundleContext) method.invoke(b, (Class[]) null);
        }
        catch (NoSuchMethodException e) {
            throw new RuntimeException("Bundle.getBundleContext() isn't implemented by the framework");
        }
        catch (Exception e) {
            throw new RuntimeException("Bundle.getBundleContext() can't be invoked.");
        }
    }

    /**
     * Utility class that can be used to check for various events. It can be used as a bundle-
     * framework- or service-listener. It keeps track of the events that have happened, which can then
     * be read using the various get... functions.
     */
    public static class GenericListener implements BundleListener, FrameworkListener, ServiceListener {
        private final List<BundleEvent> m_bundleEvents = new ArrayList<BundleEvent>();
        private final List<FrameworkEvent> m_frameworkEvents = new ArrayList<FrameworkEvent>();
        private final List<ServiceEvent> m_serviceEvents = new ArrayList<ServiceEvent>();

        public void bundleChanged(BundleEvent event) {
            m_bundleEvents.add(event);
        }

        public void frameworkEvent(FrameworkEvent event) {
            m_frameworkEvents.add(event);
        }

        public void serviceChanged(ServiceEvent event) {
            m_serviceEvents.add(event);
        }

        public List<BundleEvent> getBundleEvents() {
            return m_bundleEvents;
        }

        public List<FrameworkEvent> getFrameworkEvents() {
            return m_frameworkEvents;
        }

        public List<ServiceEvent> getServiceEvents() {
            return m_serviceEvents;
        }
    }

    /**
     * Registers a GenericListener as a bundle listener for a given bundle.
     */
    public void registerBundleListener(GenericListener g, Bundle b) {
        getBundleContext(b).addBundleListener(g);
    }

    /**
     * Registers a GenericListener as a framework listener for a given bundle.
     */
    public void registerFrameworkListener(GenericListener g, Bundle b) {
        getBundleContext(b).addFrameworkListener(g);
    }

    /**
     * Registers a GenericListener as a service listener for a given bundle and interface class.
     */
    public void registerServiceListener(GenericListener g, Bundle b, Class c) throws InvalidSyntaxException {
        registerServiceListener(g, b, "(" + Constants.OBJECTCLASS + "=" + c.getName() + ")");
    }

    /**
     * Registers a GenericListener as a service listener for a given bundle and an
     * arbitrary services filter.
     */
    public void registerServiceListener(GenericListener g, Bundle b, String filter) throws InvalidSyntaxException {
        if (filter == null) {
            getBundleContext(b).addServiceListener(g);
        }
        else {
            getBundleContext(b).addServiceListener(g, filter);
        }
    }

    /**
     * Registers a generic listener for, well, everything.
     */
    public void registerGenericListener(GenericListener g, Bundle b) throws InvalidSyntaxException {
        registerBundleListener(g, b);
        registerFrameworkListener(g, b);
        registerServiceListener(g, b, (String) null);
    }

    /*
     * Misc. helper functionality
     *****************************/

    /**
     * Cleans up bundles that have been create by the tests.
     */
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
        refreshFrameworkAndWait(null);
    }

    /**
     * Asks the framework for a refresh of given bundles, and waits for the framework to be done rewiring.
     *
     * @param bundles the bundles to refresh.
     */
    public void refreshFrameworkAndWait(Bundle[] bundles) {
        RefreshListener r = new RefreshListener();
        m_context.addFrameworkListener(r);
        try {
            synchronized (r) {
                m_admin.refreshPackages(bundles);
                r.doWait();
            }
        }
        finally {
            m_context.removeFrameworkListener(r);
        }
    }

    /**
     * Utility class for refreshFrameworkAndWait.
     */
    public static class RefreshListener implements FrameworkListener {
        private boolean done = false;

        public synchronized void frameworkEvent(FrameworkEvent e) {
            if (FrameworkEvent.PACKAGES_REFRESHED == e.getType()) {
                done = true;
                notifyAll();
            }
        }

        public synchronized void doWait() {
            while (!done) {
                try {
                    wait();
                }
                catch (InterruptedException e) {
                    e.printStackTrace();
                }
            }
        }

    }

}
