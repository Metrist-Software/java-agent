package io.metrist.agent;

import java.io.IOException;
import java.net.DatagramPacket;
import java.net.DatagramSocket;
import java.net.InetAddress;
import java.net.SocketException;
import java.net.UnknownHostException;
import java.util.Optional;
import java.util.logging.Logger;

public class Transport {
    public static Logger logger = Logger.getLogger(Transport.class.getName());

    private DatagramSocket socket;
    private InetAddress address;
    private int port;
    private boolean configured;

    private static Transport instance;

    public Transport(DatagramSocket socket, InetAddress address, int port) {
        this.socket = socket;
        this.address = address;
        this.port = port;
    }

    public Transport() {
        try {
            this.socket = new DatagramSocket();
            final var host = Optional.ofNullable(System.getenv("METRIST_MONITORING_AGENT_HOST"))
                    .orElse("localhost");

            this.address = InetAddress.getByName(host);

            this.port = Optional.ofNullable(System.getenv("METRIST_MONITORING_AGENT_PORT"))
                    .or(() -> Optional.of("51712"))
                    .map(Integer::parseInt)
                    .get();
            this.configured = true;
        } catch (Exception e) {
            this.configured = false;
            logger.severe(e.getMessage());
        }

    }

    public static Transport getInstance() {
        if (instance == null) {
            instance = new Transport();
        }

        return instance;
    }

    public void send(String msg) {
        if (!configured) {
            return;
        }

        logger.fine("Sending message: " + msg);
        byte[] buf = msg.getBytes();
        DatagramPacket packet = new DatagramPacket(buf, buf.length, address, this.port);
        try {
            socket.send(packet);
        } catch (IOException e) {
            logger.severe(e.getMessage());
        }
    }
}
