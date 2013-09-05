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

import java.io.File;
import java.net.URL;
import java.net.URLClassLoader;
import java.util.LinkedHashSet;
import java.util.Set;
import java.util.logging.Level;
import java.util.logging.Logger;

public class Launcher {

    private static final Logger LOG = Logger.getLogger(Launcher.class.getName());

    protected static final String APPLICATION_HOME_TOKEN = "${application.home}";

    private static Launcher launcher;

    private Configuration config;

    private ClassLoader applicationLoader;

    private LifecycleManager lifecycle;

    public Launcher() {
        try {
            init();
        } catch (Exception e) {
            throw new IllegalStateException(e);
        }
    }

    public void start() throws Exception {
        lifecycle.start();
    }

    public void stop() throws Exception {
        lifecycle.stop();
    }

    public static void main(String[] args) {
        if (launcher == null) {
            try {
                launcher = new Launcher();
            } catch (Exception e) {
                e.printStackTrace();
                return;
            }
        }

        try {
            String command = "start";
            if (args.length > 0) {
                command = args[args.length - 1];
            }

            if (command.equals("start")) {
                launcher.start();
            } else if (command.equals("stop")) {
                launcher.stop();
            } else {
                LOG.log(Level.WARNING, "Bootstrap: command \"" + command + "\" does not exist.");
            }
        } catch (Throwable t) {
            t.printStackTrace();
        }

    }

    private void init() throws Exception {
        initApplicationHome();

        initConfig();

        initClassLoaders();

        // load activator class
        initActivatorClass();
    }

    /**
     * Set the <code>application.home</code> System property to the current
     * working directory if it has not been set.
     */
    private void initApplicationHome() {
        String applicationHome = System.getProperty("application.home");
        if (applicationHome != null) {
            return;
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

        applicationHome = System.getProperty("application.home");
    }

    private void initConfig() {
        config = new Configuration();
    }

    private void initClassLoaders() throws Exception {
        applicationLoader = createClassLoader("application", null);
        if (applicationLoader == null) {
            applicationLoader = this.getClass().getClassLoader();
        }

        // set applicationLoader as current thread context class loader
        Thread.currentThread().setContextClassLoader(applicationLoader);
    }

    private void initActivatorClass() throws Exception {
        String acp = config.getProperty("activator.class");
        if (acp == null || acp.length() == 0) {
            throw new IllegalStateException("activator.class property is null");
        }

        try {
            Class<?> activatorClass = applicationLoader.loadClass(acp);

            // new an activator object
            Object activator = activatorClass.newInstance();
            lifecycle = new LifecycleManager(activator, config);
        } catch (Exception e) {
            LOG.log(Level.SEVERE, "can't create activator object", e);
            throw e;
        }
    }

    private ClassLoader createClassLoader(String name, ClassLoader parent) throws Exception {
        String value = config.getProperty(name + ".loader");
        if ((value == null) || (value.equals(""))) {
            return parent;
        }

        String applicationHome = System.getProperty("application.home");
        Set<URL> classPaths = new LinkedHashSet<URL>();
        String[] tokens = value.split(",");
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

}
