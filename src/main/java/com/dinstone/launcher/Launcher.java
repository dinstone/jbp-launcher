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
import java.io.FileInputStream;
import java.io.IOException;
import java.io.InputStream;
import java.net.URL;
import java.util.logging.Level;
import java.util.logging.Logger;

/**
 * java application laucher.
 * 
 * @author dinstone
 * @version 2.0.0
 */
public class Launcher {

    private static final Logger LOG = Logger.getLogger(Launcher.class.getName());

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
            String launcherHome = getLauncherHome();
            LOG.info("launcher.home is " + launcherHome);

            Configuration config = getConfiguration(launcherHome);
            LOG.config("launcher.properties is " + config);

            String applicationHome = getApplicationHome(config, launcherHome);
            LOG.info("application.home is " + applicationHome);

            lifecycle = new LifecycleManager(config);
        } catch (Exception e) {
            LOG.log(Level.SEVERE, "launcher init error.", e);
            throw new RuntimeException(e);
        }
    }

    /**
     * Set the <code>launcher.home</code> System property to the current working directory if it has not been set.
     * 
     * @return
     */
    private String getLauncherHome() {
        String launcherHome = System.getProperty(Configuration.LAUNCHER_HOME);
        if (launcherHome != null) {
            return launcherHome;
        }

        String userDir = System.getProperty("user.dir");
        File bootstrapJar = new File(userDir, "bootstrap.jar");
        if (bootstrapJar.exists()) {
            try {
                File parentDir = new File(userDir, "..");
                System.setProperty(Configuration.LAUNCHER_HOME, parentDir.getCanonicalPath());
            } catch (Exception e) {
                // Ignore
                System.setProperty(Configuration.LAUNCHER_HOME, userDir);
            }
        } else {
            System.setProperty(Configuration.LAUNCHER_HOME, userDir);
        }

        return System.getProperty(Configuration.LAUNCHER_HOME);
    }

    private Configuration getConfiguration(String launcherHome) {
        // first find launcher file from system property
        InputStream is = null;
        try {
            String configUrl = System.getProperty("launcher.config");
            if (configUrl != null) {
                is = (new URL(configUrl)).openStream();
            }
        } catch (Throwable t) {
        }

        // second find launcher file from launcher home's dir
        if (is == null) {
            try {
                File confidDir = new File(launcherHome, "config");
                File configFile = new File(confidDir, "launcher.properties");
                is = new FileInputStream(configFile);
            } catch (Throwable t) {
            }
        }

        Configuration configuration = new Configuration();
        if (is != null) {
            try {
                configuration.loadProperties(is);
            } catch (Throwable t) {
                LOG.log(Level.WARNING, "Failed to load launcher.properties.", t);
            } finally {
                try {
                    is.close();
                } catch (IOException e) {
                }
            }
        }

        return configuration;
    }

    private String getApplicationHome(Configuration config, String launcherHome) {
        String applicationHome = System.getProperty(Configuration.APPLICATION_HOME);
        if (applicationHome != null && applicationHome.length() > 0) {
            config.setProperty(Configuration.APPLICATION_HOME, applicationHome);
            return applicationHome;
        }

        applicationHome = config.getProperty(Configuration.APPLICATION_HOME);
        if (applicationHome != null && applicationHome.length() > 0) {
            System.setProperty(Configuration.APPLICATION_HOME, applicationHome);
            return applicationHome;
        }

        applicationHome = launcherHome;
        System.setProperty(Configuration.APPLICATION_HOME, applicationHome);
        config.setProperty(Configuration.APPLICATION_HOME, applicationHome);
        return applicationHome;
    }

}
