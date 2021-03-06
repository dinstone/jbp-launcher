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

import java.util.Enumeration;
import java.util.Map;
import java.util.Properties;
import java.util.logging.Level;
import java.util.logging.Logger;

/**
 * java application launcher.
 * 
 * @author dinstone
 * @version 2.0.0
 */
public class Launcher {

    private LifecycleManager lifecycle;

    public Launcher() {
        Logger logger = Logger.getLogger(Launcher.class.getName());
        try {
            Configuration config = new Configuration();

            String launcherHome = config.getLauncherHome();
            logger.info("launcher.home is " + launcherHome);

            String applicationHome = config.getApplicationHome();
            logger.info("application.home is " + applicationHome);

            lifecycle = new LifecycleManager(config);
        } catch (Exception e) {
            logger.log(Level.SEVERE, "launcher init error.", e);
            throw new RuntimeException(e);
        }
    }

    public void start() throws Exception {
        lifecycle.start();
    }

    public void stop() throws Exception {
        lifecycle.stop();
    }

    private static void systemPropertyAndEnvironment() {
        Logger logger = Logger.getLogger(Launcher.class.getName());

        // System Properties
        Properties sp = System.getProperties();
        Enumeration<?> names = sp.propertyNames();
        while (names.hasMoreElements()) {
            String k = (String) names.nextElement();
            logger.log(Level.INFO, "System Property: " + k + "=" + sp.getProperty(k));
        }

        // System Environment
        Map<String, String> env = System.getenv();
        for (String k : env.keySet()) {
            logger.log(Level.INFO, "System Environment: " + k + "=" + env.get(k));
        }
    }

    public static void main(String[] args) {
        System.setProperty("java.util.logging.manager", JdkLogManager.class.getName());

        systemPropertyAndEnvironment();

        try {
            String command = "start";
            if (args.length > 0) {
                command = args[args.length - 1];
            }

            if (!command.equals("start") && !command.equals("stop")) {
                System.out.println("Java launcher does not support this command: \"" + command + "\".");

                // Forced to exit the JVM
                System.exit(0);
            }

            Launcher launcher = new Launcher();
            if (command.equals("start")) {
                launcher.start();
            } else if (command.equals("stop")) {
                launcher.stop();
            }
        } catch (Throwable t) {
            t.printStackTrace();
            System.exit(-1);
        }
    }

}
