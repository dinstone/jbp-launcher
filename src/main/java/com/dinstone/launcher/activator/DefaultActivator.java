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

package com.dinstone.launcher.activator;

import java.util.Enumeration;
import java.util.Map;
import java.util.Properties;
import java.util.logging.Level;
import java.util.logging.Logger;

public class DefaultActivator {

    private static final Logger LOG = Logger.getLogger(DefaultActivator.class.getName());

    public void start() {
        showSystemEnvironment();

        try {
            Thread.sleep(10000);
        } catch (InterruptedException e) {
            e.printStackTrace();
        }
    }

    public void stop() {
        try {
            Thread.sleep(10000);
        } catch (InterruptedException e) {
            e.printStackTrace();
        }
    }

    private void showSystemEnvironment() {
        // System Properties
        Properties sp = System.getProperties();
        Enumeration<?> names = sp.propertyNames();
        while (names.hasMoreElements()) {
            String k = (String) names.nextElement();
            LOG.log(Level.INFO, "System Properties: " + k + "=" + sp.getProperty(k));
        }

        // System Environment
        Map<String, String> env = System.getenv();
        for (String k : env.keySet()) {
            LOG.log(Level.CONFIG, "System Environment: " + k + "=" + env.get(k));
        }
    }

}
