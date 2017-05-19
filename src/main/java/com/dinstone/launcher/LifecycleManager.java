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
import java.io.IOException;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.io.OutputStream;
import java.io.UnsupportedEncodingException;
import java.lang.reflect.Method;
import java.net.InetAddress;
import java.net.ServerSocket;
import java.net.Socket;
import java.net.URL;
import java.net.URLClassLoader;
import java.security.AccessControlException;
import java.util.LinkedHashSet;
import java.util.Set;
import java.util.logging.Level;
import java.util.logging.Logger;

import javax.naming.ConfigurationException;

public class LifecycleManager {

    private static final Logger LOG = Logger.getLogger(LifecycleManager.class.getName());

    protected static final String APPLICATION_HOME_TOKEN = "${application.home}";

    /**  */
    private static final String MESSAGE_INVALID = "INVALID";

    /**  */
    private static final String MESSAGE_STOPPED = "STOPPED";

    /**  */
    private static final String DEFAULT_COMMAND = "SHUTDOWN";

    private static final int DEFAULT_LISTEN_PORT = 5555;

    /**
     * The shutdown command string we are looking for.
     */
    private String shutdownCommand = DEFAULT_COMMAND;

    private int listenPort = DEFAULT_LISTEN_PORT;

    private boolean awaitEnabled = true;

    private volatile boolean awaitStop = false;

    private volatile ServerSocket awaitSocket;

    private volatile Thread awaitThread;

    private final Configuration config;

    private Object activator;

    public LifecycleManager(Configuration config) {
        this.config = config;

        String portPro = config.getProperty("lifecycle.listen.port");
        try {
            listenPort = Integer.parseInt(portPro);
        } catch (Exception e) {
            LOG.log(Level.WARNING, "Invalid listener port, will use default port {0}", listenPort);
        }

        String command = config.getProperty("lifecycle.listen.command");
        if (command != null) {
            this.shutdownCommand = command;
        }

        String enabled = config.getProperty("lifecycle.listen.enabled");
        if (enabled != null) {
            this.awaitEnabled = Boolean.parseBoolean(enabled);
        }
    }

    public void start() throws Exception {
        createListener();

        activate();

        startListener();
    }

    private void createListener() {
        if (awaitEnabled) {
            // Set up a server socket to wait on
            try {
                InetAddress hostAddress = InetAddress.getByName("localhost");
                awaitSocket = new ServerSocket(listenPort, 1, hostAddress);
            } catch (IOException e) {
                LOG.log(Level.SEVERE, "Can't create listener on " + listenPort, e);
                throw new RuntimeException(e);
            }
        }
    }

    private void activate() throws Exception {
        createActivator();

        startActivator();
    }

    protected void startListener() {
        if (!awaitEnabled || awaitSocket == null) {
            return;
        }

        ApplicationShutdownHook shutdownHook = new ApplicationShutdownHook();
        Runtime.getRuntime().addShutdownHook(shutdownHook);

        // whether await was aborted
        boolean aborted = await();

        if (!aborted) {
            Runtime.getRuntime().removeShutdownHook(shutdownHook);
        }

        destroyListener();
    }

    private boolean await() {
        awaitThread = Thread.currentThread();

        // Loop waiting for a connection and a valid command
        while (!awaitStop) {
            // Wait for the next connection
            Socket socket = null;
            InputStream stream = null;
            try {
                socket = awaitSocket.accept();
                socket.setSoTimeout(2000); // two seconds
                stream = socket.getInputStream();

                InetAddress radd = socket.getInetAddress();
                LOG.log(Level.INFO, "Have an closing request at " + radd);
            } catch (AccessControlException ace) {
                LOG.log(Level.WARNING, "security exception", ace);
                continue;
            } catch (IOException e) {
                if (awaitStop) {
                    // Wait was aborted with socket.close()
                    LOG.log(Level.INFO, "Accept socket aborted");
                    break;
                } else {
                    LOG.log(Level.SEVERE, "Accept socket failure", e);
                    throw new RuntimeException(e);
                }
            }

            // Read a set of characters from the socket
            StringBuilder command = new StringBuilder();
            int expected = shutdownCommand.length(); // Cut off to avoid DoS attack
            while (expected > 0) {
                int ch = -1;
                try {
                    ch = stream.read();
                } catch (IOException e) {
                    ch = -1;
                }
                if (ch < 32) {// Control character or EOF terminates loop
                    break;
                }
                command.append((char) ch);
                expected--;
            }

            // Match against our command string
            if (command.toString().equals(shutdownCommand)) {
                stopActivator();
                response(socket, MESSAGE_STOPPED);
                break;
            } else {
                LOG.log(Level.INFO, "Invalid command '" + command.toString() + "' received");
                response(socket, MESSAGE_INVALID);
            }
        }

        return awaitStop;
    }

    protected void stopListener() {
        stopActivator();

        LOG.log(Level.INFO, "Notify socket aborted");
        awaitStop = true;

        if (awaitThread != null) {
            destroyListener();

            awaitThread.interrupt();
            try {
                awaitThread.join(1000);
            } catch (InterruptedException e) {
                // Ignored
            }
        }
    }

    private void destroyListener() {
        // Close the server socket
        if (awaitSocket != null) {
            try {
                awaitSocket.close();
            } catch (IOException e) {
            }
        }
    }

    public void stop() throws Exception {
        try {
            InetAddress hostAddress = InetAddress.getByName("localhost");
            Socket socket = new Socket(hostAddress, listenPort);
            // send close request
            OutputStream out = socket.getOutputStream();
            for (int i = 0; i < shutdownCommand.length(); i++) {
                out.write(shutdownCommand.charAt(i));
            }
            out.flush();

            LOG.log(Level.INFO, "Activator is stopping");

            // receive close response
            InputStream in = socket.getInputStream();
            StringBuilder reply = new StringBuilder();
            int res = -1;
            while ((res = in.read()) != -1) {
                reply.append((char) res);
            }
            socket.close();

            // response
            if (reply.toString().equals(MESSAGE_STOPPED)) {
                LOG.log(Level.INFO, "Activator is stopped");
            } else {
                LOG.log(Level.INFO, "Activator stop failure: {0}", reply);
            }
        } catch (Exception e) {
            LOG.log(Level.SEVERE, "Activator stop error: ", e);
        }
    }

    private void createActivator() throws Exception {
        ClassLoader applicationClassLoader = getApplicationClassLoader();

        String activatorClassName = config.getProperty("application.activator");
        if (activatorClassName == null || activatorClassName.length() == 0) {
            activatorClassName = findActivatorByJarService(applicationClassLoader, "application.activator");
        }

        if (activatorClassName == null || activatorClassName.length() == 0) {
            throw new IllegalStateException("can't find application activator class");
        }
        LOG.log(Level.INFO, "application.activator is " + activatorClassName);

        try {
            Class<?> activatorClass = applicationClassLoader.loadClass(activatorClassName);

            // new an activator object
            activator = activatorClass.newInstance();
        } catch (Exception e) {
            LOG.log(Level.SEVERE, "can't create activator object", e);
            throw e;
        }
    }

    private ClassLoader getApplicationClassLoader() throws Exception {
        String classPath = config.getProperty("application.classpath");
        if (classPath == null) {
            // default class path
            classPath = "${application.home}/config,${application.home}/lib/*.jar";
        }

        String applicationHome = config.getProperty(Configuration.APPLICATION_HOME);
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

    private String findActivatorByJarService(ClassLoader classLoader, String activatorId)
            throws ConfigurationException {
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

    private void startActivator() {
        try {
            long startTime = System.currentTimeMillis();

            Method method = activator.getClass().getMethod("start", (Class[]) null);
            method.invoke(activator, (Object[]) null);

            long total = System.currentTimeMillis() - startTime;
            LOG.log(Level.INFO, "Activator startup in {0} ms", total);
        } catch (Exception e) {
            LOG.log(Level.SEVERE, "Activator startup failure", e);
            throw new RuntimeException(e);
        }
    }

    private void stopActivator() {
        try {
            long startTime = System.currentTimeMillis();

            Method method = activator.getClass().getMethod("stop", (Class[]) null);
            method.invoke(activator, (Object[]) null);

            long total = System.currentTimeMillis() - startTime;
            LOG.log(Level.INFO, "Activator shutdown in {0} ms", total);
        } catch (Exception e) {
            LOG.log(Level.SEVERE, "Activator stop error", e);
        }
    }

    private void response(Socket socket, String message) {
        // Close the socket now that we are done with it
        try {
            OutputStream out = socket.getOutputStream();
            for (int i = 0; i < message.length(); i++) {
                out.write(message.charAt(i));
            }
            out.flush();

            socket.close();
        } catch (IOException e) {
        }
    }

    protected class ApplicationShutdownHook extends Thread {

        @Override
        public void run() {
            try {
                LOG.log(Level.INFO, "Application shutdown running");

                stopListener();
            } catch (Throwable ex) {
                LOG.log(Level.WARNING, "Application shutdown failure", ex);
            }
        }
    }
}
