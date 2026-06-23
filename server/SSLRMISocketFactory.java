package server;

import javax.net.ssl.*;
import java.io.IOException;
import java.io.Serializable;
import java.net.ServerSocket;
import java.net.Socket;
import java.rmi.server.RMIClientSocketFactory;
import java.rmi.server.RMIServerSocketFactory;

/**
 * Custom SSL/TLS Socket Factory for Java RMI.
 * Provides encrypted communication between client and server.
 *
 * This satisfies Task 4: Secure communication between
 * the Employee/HR and the PRS (Payroll System).
 *
 * Both factories implement Serializable so they can be
 * transmitted to the client via the RMI registry.
 */
public class SSLRMISocketFactory {

    /**
     * Server-side SSL socket factory.
     * Creates SSL server sockets using the configured keystore.
     */
    public static class SSLServerFactory implements RMIServerSocketFactory, Serializable {
        private static final long serialVersionUID = 1L;

        @Override
        public ServerSocket createServerSocket(int port) throws IOException {
            SSLServerSocketFactory ssf = (SSLServerSocketFactory) SSLServerSocketFactory.getDefault();
            SSLServerSocket ss = (SSLServerSocket) ssf.createServerSocket(port);
            // Enable strong protocols only
            ss.setEnabledProtocols(new String[]{"TLSv1.2", "TLSv1.3"});
            System.out.println("[SSL] Server socket created on port " + port);
            return ss;
        }

        @Override
        public boolean equals(Object obj) { return obj instanceof SSLServerFactory; }
        @Override
        public int hashCode() { return SSLServerFactory.class.hashCode(); }
    }

    /**
     * Client-side SSL socket factory.
     * Creates SSL client sockets using the configured truststore.
     */
    public static class SSLClientFactory implements RMIClientSocketFactory, Serializable {
        private static final long serialVersionUID = 1L;

        @Override
        public Socket createSocket(String host, int port) throws IOException {
            SSLSocketFactory sf = (SSLSocketFactory) SSLSocketFactory.getDefault();
            SSLSocket socket = (SSLSocket) sf.createSocket(host, port);
            socket.setEnabledProtocols(new String[]{"TLSv1.2", "TLSv1.3"});
            return socket;
        }

        @Override
        public boolean equals(Object obj) { return obj instanceof SSLClientFactory; }
        @Override
        public int hashCode() { return SSLClientFactory.class.hashCode(); }
    }
}
