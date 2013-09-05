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
import java.io.InputStream;
import java.net.URL;
import java.util.Properties;
import java.util.logging.Level;
import java.util.logging.Logger;

public class Configuration {

    private static final Logger LOG = Logger.getLogger(Configuration.class.getName());

    private Properties properties = null;

    public Configuration() {
        loadProperties();
    }

    public String getProperty(String name) {
        return properties.getProperty(name);
    }

    /**
     * Return specified property value.
     */
    public String getProperty(String name, String defaultValue) {
        return properties.getProperty(name, defaultValue);
    }

    /**
     * Load properties.
     */
    private void loadProperties() {
        InputStream is = null;
        Throwable error = null;

        try {
            String configUrl = getConfigUrl();
            if (configUrl != null) {
                is = (new URL(configUrl)).openStream();
            }
        } catch (Throwable t) {
            // Ignore
        }

        if (is == null) {
            try {
                File home = new File(getApplicationHome());
                File conf = new File(home, "config");
                File propFile = new File(conf, "launcher.properties");
                is = new FileInputStream(propFile);
            } catch (Throwable t) {
                // Ignore
            }
        }

        if (is != null) {
            try {
                properties = new Properties();
                properties.load(is);
                is.close();
            } catch (Throwable t) {
                error = t;
            }
        }

        if ((is == null) || (error != null)) {
            // Do something
            LOG.log(Level.WARNING, "Failed to load launcher.properties", error);
            // That's fine - we have reasonable defaults.
            properties = new Properties();
        }
    }

    /**
     * Get the value of the application.home environment variable.
     */
    private String getApplicationHome() {
        return System.getProperty("application.home", System.getProperty("user.dir"));
    }

    /**
     * Get the value of the configuration URL.
     */
    private String getConfigUrl() {
        return System.getProperty("launcher.config");
    }

}
