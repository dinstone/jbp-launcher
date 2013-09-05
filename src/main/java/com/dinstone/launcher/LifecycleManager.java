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

import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;
import java.lang.reflect.Method;
import java.net.InetAddress;
import java.net.ServerSocket;
import java.net.Socket;
import java.security.AccessControlException;
import java.util.logging.Level;
import java.util.logging.Logger;

public class LifecycleManager {

    private static final Logger LOG = Logger.getLogger(LifecycleManager.class.getName());

    /**  */
    private static final String INVALID = "INVALID";

    /**  */
    private static final String STOPPED = "STOPPED";

    /**  */
    private static final String CMDM = "SHUTDOWN";

    /**
     * The shutdown command string we are looking for.
     */
    private String shutdown = CMDM;

    private int port = 5555;

    private boolean await = true;

    private Object activator;

    private ServerSocket serverSocket;

    public LifecycleManager(Object activator, Configuration config) {
        this.activator = activator;

        String portPro = config.getProperty("listen.port");
        try {
            port = Integer.parseInt(portPro);
        } catch (Exception e) {
        }

        String shutdown = config.getProperty("listen.cmdm");
        if (shutdown != null && shutdown.length() > 0) {
            this.shutdown = shutdown;
        }

        String enabled = config.getProperty("lifecycle.enabled");
        this.await = Boolean.parseBoolean(enabled);
    }

    public void start() {
        init();

        startActivator();

        await();
    }

    public void stop() {
        try {
            InetAddress hostAddress = InetAddress.getByName("localhost");
            Socket socket = new Socket(hostAddress, port);
            // send close request
            OutputStream out = socket.getOutputStream();
            for (int i = 0; i < shutdown.length(); i++) {
                out.write(shutdown.charAt(i));
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
            if (reply.toString().equals(STOPPED)) {
                LOG.log(Level.INFO, "Activator is stopped");
            } else {
                LOG.log(Level.INFO, "Activator stop failure: " + reply);
            }
        } catch (Exception e) {
            LOG.log(Level.SEVERE, "Activator stop error: ", e);
        }
    }

    private void init() {
        if (await) {
            // Set up a server socket to wait on
            try {
                InetAddress hostAddress = InetAddress.getByName("localhost");
                serverSocket = new ServerSocket(port, 1, hostAddress);
            } catch (IOException e) {
                LOG.log(Level.SEVERE, "Can't create listener on " + port, e);
                throw new RuntimeException(e);
            }
        }
    }

    private void await() {
        if (serverSocket == null) {
            return;
        }

        // Loop waiting for a connection and a valid command
        while (true) {
            // Wait for the next connection
            Socket socket = null;
            InputStream stream = null;
            try {
                socket = serverSocket.accept();
                socket.setSoTimeout(2000); // two seconds
                stream = socket.getInputStream();

                InetAddress radd = socket.getInetAddress();
                LOG.log(Level.INFO, "Have an closing request at " + radd);
            } catch (AccessControlException ace) {
                LOG.log(Level.WARNING, "security exception", ace);
                continue;
            } catch (IOException e) {
                LOG.log(Level.SEVERE, "accept socket failure", e);
                throw new RuntimeException(e);
            }

            // Read a set of characters from the socket
            StringBuilder command = new StringBuilder();
            int expected = shutdown.length(); // Cut off to avoid DoS attack
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
            if (command.toString().equals(shutdown)) {
                stopActivator();
                response(socket, STOPPED);
                break;
            } else {
                LOG.log(Level.INFO, "Invalid command '" + command.toString() + "' received");
                response(socket, INVALID);
            }
        }

        // Close the server socket and return
        try {
            serverSocket.close();
        } catch (IOException e) {
            ;
        }

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
}
