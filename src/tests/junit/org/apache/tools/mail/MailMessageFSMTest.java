package org.apache.tools.mail;

import org.apache.tools.ant.DummyMailServer;
import org.junit.Before;
import org.junit.Test;

import java.io.*;
import java.net.InetAddress;
import java.net.ServerSocket;
import java.net.Socket;

import static org.junit.Assert.assertTrue;
import static org.junit.Assert.fail;

/*  Validates the Finite State Machine (FSM) model of MailMessage.
 *  1. Valid state transitions (Happy Path)
 *  2. Invalid transitions/failures (Error Paths).
 */
public class MailMessageFSMTest {

    private String local;

    @Before
    public void setUp() {
        try {
            local = InetAddress.getLocalHost().getCanonicalHostName();
        } catch (java.net.UnknownHostException uhe) {
            // ignore
        }
    }

    /*
     *  Test Case 1 - Happy Path
     *  Covers: CONNECTED -> SENDER_SET -> RECIPIENT_SET -> DATA_MODE -> SENT_CLOSE
     *  MailServer: DummyMailServer (Ant project's standard server)
     *  This verifies the client successfully navigates all states to completion.
     * 
     * @throws InterruptedException if something goes wrong
     */
    @Test
    public void testFSMHappyPath() throws InterruptedException {
        // Start existing Ant DummyMailServer
        final DummyMailServer server = DummyMailServer.startMailServer(this.local);
        
        // Run client in a separate thread to prevent blocking
        Thread clientThread = new Thread(() -> {
            try {
                // State: CONNECTED
                MailMessage msg = new MailMessage("localhost", server.getPort());
                
                // State: SENDER_SET
                msg.from("Mail Message <fsm-test@ant.apache.org>");
                
                // State: RECIPIENT_SET
                msg.to("to@you.com");
                msg.setSubject("Test subject");
                
                // State: DATA_MODE
                PrintStream out = msg.getPrintStream();
                out.println("Happy Path Testing...");
                
                // State: SENT_CLOSE
                msg.sendAndClose();
                
            } catch (IOException e) {
                fail("FSM Happy Path failed: " + e.getMessage());
            }
        });

        clientThread.start();
        clientThread.join(5000); // Wait 5 seconds
        server.disconnect();

        // Verify the server recorded the correct sequence of commands
        String result = server.getResult();
        assertTrue("Should contain 220 (OK_READY)", result.contains("220"));
        assertTrue("Should contain HELO", result.contains("HELO"));
        assertTrue("Should contain 250 (OK_HELO)", result.contains("250"));
        assertTrue("Should contain MAIL FROM", result.contains("MAIL FROM"));
        assertTrue("Should contain 250 (OK_FROM)", result.contains("250"));
        assertTrue("Should contain RCPT TO", result.contains("RCPT TO"));
        assertTrue("Should contain 250 (OK_RCPT_1)", result.contains("250"));
        assertTrue("Should contain DATA", result.contains("DATA"));
        assertTrue("Should contain 354 (OK_DATA)", result.contains("354"));
        assertTrue("Should contain QUIT", result.contains("QUIT"));
        assertTrue("Should contain 221 (OK_QUIT)", result.contains("221"));
    }

    /**
     * Test Case 2: Protocol Violation (Error State)
     * Covers: CONNECTED --to()--> ERROR
     * MailServer: OOOMailServer (Custom server stimulate out-of-order command and returns 500 Error)
     * Verifies the FSM transitions to ERROR when out of order command.
     */
    @Test(expected = IOException.class)
    public void testFSMProtocolViolation() throws IOException, InterruptedException {
        // Start a server stimulate out of order command
        OOOMailServer oooServer = new OOOMailServer();
        oooServer.start();

        try {
            // CONNECTED
            MailMessage msg = new MailMessage("localhost", oooServer.getPort());

            // Attempt transition to RECIPIENT_SET, skip SENDER_SET
            // Server will respond "500 Error", causing MailMessage throw IOException
            msg.to("to@you.com");
            
        } finally {
            oooServer.kill();
        }
    }

    /**
     * Test Case 3: Network Interruption (Error State)
     * Covers: CONNECTED -> SENDER_SET -> RECIPIENT_SET -> ERROR
     * MailServer: GhostMailServer (custom server closes the socket suddenly)
     * Verifies the FSM transitions to ERROR when connection loss 
     */
    @Test(expected = IOException.class)
    public void testFSMNetworkInterruption() throws IOException, InterruptedException {
        // Start server hangs up suddenly
        GhostMailServer ghostServer = new GhostMailServer();
        ghostServer.start();

        try {
            // CONNECTED
            MailMessage msg = new MailMessage("localhost", ghostServer.getPort());

            // SENDER_SET
            msg.from("Mail Message <fsm-test@ant.apache.org>");
                
            // RECIPIENT_SET (Server accepts RCPT, then closes)
            msg.to("to@you.com");
                
            // DATA_MODE
            PrintStream out = msg.getPrintStream();
            out.println("Network Interruption Testing...");

        } finally {
            ghostServer.kill();
        }
    }

    // ---  Out-of-Order Mail Server  ---
    private static class OOOMailServer extends Thread {
        private ServerSocket serverSocket;
        private volatile boolean running = true;

        public OOOMailServer() throws IOException {
            serverSocket = new ServerSocket(0);
        }

        public int getPort() {
            return serverSocket.getLocalPort();
        }

        public void kill() {
            running = false;
            try {
                serverSocket.close();
            } catch (IOException e) {
                // ignored
            }
        }

        @Override
        public void run() {
            while (running) {
                try {
                    Socket socket = serverSocket.accept();
                    try {
                        PrintWriter out = new PrintWriter(socket.getOutputStream(), true);
                        BufferedReader in = new BufferedReader(new InputStreamReader(socket.getInputStream()));
                        
                        out.println("220 OOOMailServer Ready");
                        String line;
                        while ((line = in.readLine()) != null) {
                            if (line.startsWith("HELO")) {
                                out.println("250 OK");
                            } else if (line.startsWith("RCPT TO")) {
                                out.println("500 Error");
                            } else if (line.startsWith("QUIT")) {
                                out.println("221 Bye");
                                break;
                            } else {
                                out.println("250 OK"); 
                            }
                        }
                    } finally {
                        socket.close();
                    }
                } catch (IOException e) {
                    // Ignore exception when serverSocket is closed by kill()
                }
            }
        }
    }

    // --- Ghost Mail Server (Hangs up unexpectedly) ---
    private static class GhostMailServer extends Thread {
        private ServerSocket serverSocket;
        private volatile boolean running = true;

        public GhostMailServer() throws IOException {
            serverSocket = new ServerSocket(0);
        }

        public int getPort() {
            return serverSocket.getLocalPort();
        }

        public void kill() {
            running = false;
            try {
                serverSocket.close();
            } catch (IOException e) {
                // ignore
            }
        }

        @Override
        public void run() {
            while (running) {
                try {
                    Socket socket = serverSocket.accept();
                    try {
                        PrintWriter out = new PrintWriter(socket.getOutputStream(), true);
                        BufferedReader in = new BufferedReader(new InputStreamReader(socket.getInputStream()));

                        out.println("220 RudeServer Ready");
                        String line;
                        while ((line = in.readLine()) != null) {
                            if (line.startsWith("HELO")) {
                                out.println("250 OK");
                            } else if (line.startsWith("MAIL FROM")) {
                                out.println("250 OK");
                            } else if(line.startsWith("RCPT TO")){
                                out.println("250 OK");
                                // Hang up immediately
                                socket.close(); 
                                break;
                            } else {
                                out.println("250 OK");
                            }
                        }
                    } finally {
                        if (!socket.isClosed()) socket.close();
                    }
                } catch (IOException e) {
                    // ignore
                }
            }
        }
    }
}