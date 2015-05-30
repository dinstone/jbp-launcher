/*
 * Copyright (C) 2012~2013 dinstone<dinstone@163.com>
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *     http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */

package com.dinstone.launcher;

import java.io.BufferedReader;
import java.io.File;
import java.io.FileInputStream;
import java.io.IOException;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.io.UnsupportedEncodingException;
import java.net.URL;
import java.net.URLClassLoader;
import java.util.LinkedHashSet;
import java.util.Set;
import java.util.logging.Level;
import java.util.logging.Logger;

import javax.naming.ConfigurationException;

public class Launcher {

    private static final Logger LOG = Logger.getLogger(Launcher.class.getName());

    protected static final String APPLICATION_HOME_TOKEN = "${application.home}";

    private LifecycleManager lifecycle;

    public Launcher() {
        init();
    }

    public void start() throws Exception {
        lifecycle.start();
    }

    public void stop() throws Exception {
        lifecycle.stop();
    }

    public static void main(String[] args) {
        try {
            Launcher launcher = new Launcher();

            String command = "start";
            if (args.length > 0) {
                command = args[args.length - 1];
            }

            if (command.equals("start")) {
                launcher.start();

                // Forced to exit the JVM
                System.exit(0);
            } else if (command.equals("stop")) {
                launcher.stop();
            } else {
                LOG.warning("Launcher: command \"" + command + "\" does not exist.");
            }
        } catch (Throwable t) {
            t.printStackTrace();
            System.exit(-1);
        }

    }

    private void init() {
        try {
            String applicationHome = getApplicationHome();
            LOG.config("application.home is " + applicationHome);

            Configuration config = loadConfiguration(applicationHome);
            LOG.config("launcher.properties is " + config);

            ClassLoader applicationClassLoader = getApplicationClassLoader(applicationHome, config);
            LOG.config("application.classloader is " + applicationClassLoader);

            // load activator
            initActivator(applicationClassLoader, config);
        } catch (Exception e) {
            LOG.log(Level.SEVERE, "launcher init error.", e);
            throw new RuntimeException(e);
        }
    }

    /**
     * Set the <code>application.home</code> System property to the current working directory if it has not been set.
     */
    private String getApplicationHome() {
        String applicationHome = System.getProperty("application.home");
        if (applicationHome != null) {
            return applicationHome;
        }

        File bootstrapJar = new File(System.getProperty("user.dir"), "bootstrap.jar");
        if (bootstrapJar.exists()) {
            try {
                File parentDir = new File(System.getProperty("user.dir"), "..");
                System.setProperty("application.home", parentDir.getCanonicalPath());
            } catch (Exception e) {
                // Ignore
                System.setProperty("application.home", System.getProperty("user.dir"));
            }
        } else {
            System.setProperty("application.home", System.getProperty("user.dir"));
        }

        return System.getProperty("application.home");
    }

    private Configuration loadConfiguration(String applicationHome) {
        Configuration configuration = new Configuration(System.getProperties());

        InputStream is = null;
        try {
            String configUrl = System.getProperty("launcher.config");
            if (configUrl != null) {
                is = (new URL(configUrl)).openStream();
            }
        } catch (Throwable t) {
        }

        if (is == null) {
            try {
                File confidDir = new File(applicationHome, "config");
                File configFile = new File(confidDir, "launcher.properties");
                is = new FileInputStream(configFile);
            } catch (Throwable t) {
            }
        }

        if (is != null) {
            try {
                configuration.loadProperties(is);
                is.close();
            } catch (Throwable t) {
                LOG.log(Level.WARNING, "Failed to load launcher.properties.", t);
            }
        }

        return configuration;
    }

    private ClassLoader getApplicationClassLoader(String applicationHome, Configuration config) throws Exception {
        String classPath = config.getProperty("application.classpath");
        if (classPath == null) {
            // default class path
            classPath = "${application.home}/config,${application.home}/lib/*.jar";
        }

        ClassLoader classLoader = createClassLoader(applicationHome, classPath, null);
        if (classLoader == null) {
            classLoader = this.getClass().getClassLoader();
        }

        // set applicationLoader as current thread context class loader
        Thread.currentThread().setContextClassLoader(classLoader);

        return classLoader;
    }

    private ClassLoader createClassLoader(String applicationHome, String classPath, ClassLoader parent)
            throws Exception {
        if ((classPath == null) || (classPath.equals(""))) {
            return parent;
        }

        Set<URL> classPaths = new LinkedHashSet<URL>();
        String[] tokens = classPath.split(",");
        for (String token : tokens) {
            int index = token.indexOf(APPLICATION_HOME_TOKEN);
            if (index == 0) {
                token = applicationHome + token.substring(APPLICATION_HOME_TOKEN.length());
            }

            try {
                URL url = new URL(token);
                classPaths.add(url);
                continue;
            } catch (Exception e) {
            }

            if (token.endsWith("*.jar")) {
                token = token.substring(0, token.length() - "*.jar".length());

                File directory = new File(token);
                if (!directory.exists() || !directory.isDirectory() || !directory.canRead()) {
                    continue;
                }

                String[] filenames = directory.list();
                for (int j = 0; j < filenames.length; j++) {
                    String filename = filenames[j].toLowerCase();
                    if (!filename.endsWith(".jar")) {
                        continue;
                    }
                    File file = new File(directory, filenames[j]);
                    if (!file.exists() || !file.canRead()) {
                        continue;
                    }

                    LOG.log(Level.CONFIG, "Including glob jar file [{0}]", file.getAbsolutePath());
                    URL url = file.toURI().toURL();
                    classPaths.add(url);
                }
            } else if (token.endsWith(".jar")) {
                File file = new File(token);
                if (!file.exists() || !file.canRead()) {
                    continue;
                }

                LOG.log(Level.CONFIG, "Including jar file [{0}]", file.getAbsolutePath());
                URL url = file.toURI().toURL();
                classPaths.add(url);
            } else {
                File directory = new File(token);
                if (!directory.exists() || !directory.isDirectory() || !directory.canRead()) {
                    continue;
                }

                LOG.log(Level.CONFIG, "Including directory {0}", directory.getAbsolutePath());
                URL url = directory.toURI().toURL();
                classPaths.add(url);
            }
        }

        URL[] urls = classPaths.toArray(new URL[classPaths.size()]);
        if (LOG.isLoggable(Level.CONFIG)) {
            for (int i = 0; i < urls.length; i++) {
                LOG.log(Level.CONFIG, "location " + i + " is " + urls[i]);
            }
        }

        ClassLoader classLoader = null;
        if (parent == null) {
            classLoader = new URLClassLoader(urls);
        } else {
            classLoader = new URLClassLoader(urls, parent);
        }
        return classLoader;
    }

    private void initActivator(ClassLoader applicationClassLoader, Configuration config) throws Exception {
        String activatorClassName = config.getProperty("application.activator");
        if (activatorClassName == null || activatorClassName.length() == 0) {
            activatorClassName = findActivatorByJarService(applicationClassLoader, "application.activator");
        }

        if (activatorClassName == null || activatorClassName.length() == 0) {
            throw new IllegalStateException("can't find application activator class");
        }

        try {
            Class<?> activatorClass = applicationClassLoader.loadClass(activatorClassName);

            // new an activator object
            Object activator = activatorClass.newInstance();
            lifecycle = new LifecycleManager(activator, config);
        } catch (Exception e) {
            LOG.log(Level.SEVERE, "can't create activator object", e);
            throw e;
        }
    }

    private String findActivatorByJarService(ClassLoader classLoader, String activatorId) throws ConfigurationException {
        String serviceId = "META-INF/services/" + activatorId;

        InputStream is = classLoader.getResourceAsStream(serviceId);
        if (is == null) {
            return null;
        }

        BufferedReader rd;
        try {
            rd = new BufferedReader(new InputStreamReader(is, "UTF-8"));
        } catch (UnsupportedEncodingException e) {
            rd = new BufferedReader(new InputStreamReader(is));
        }

        String activatorClassName = null;
        try {
            // Does not handle all possible input as specified by the
            // Jar Service Provider specification
            activatorClassName = rd.readLine();
            rd.close();
        } catch (IOException x) {
        }

        return activatorClassName;
    }

}
