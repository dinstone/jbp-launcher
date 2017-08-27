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

import java.text.SimpleDateFormat;
import java.util.Date;
import java.util.logging.Logger;

public class DefaultActivator {

    private static final Logger LOG = Logger.getLogger(DefaultActivator.class.getName());

    private final Thread job;

    public DefaultActivator() {
        job = new JobRunner("Time-Job");
    }

    public void start() {
        job.start();
        LOG.info("Activator start");
    }

    public void stop() {
        try {
            job.interrupt();
            job.join();
        } catch (Exception e) {
            // ignore
            e.printStackTrace();
        }

        LOG.info("Activator stop");
    }

    class JobRunner extends Thread {

        public JobRunner(String name) {
            setName(name);
        }

        @Override
        public void run() {
            SimpleDateFormat format = new SimpleDateFormat("yyyy-MM-dd HH:mm:ss");
            while (!Thread.interrupted()) {
                LOG.info("currentime " + format.format(new Date()));

                try {
                    Thread.sleep(1000);
                } catch (InterruptedException e) {
                    break;
                }
            }
        }

    }

}
