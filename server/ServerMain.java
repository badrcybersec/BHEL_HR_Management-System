package server;

import common.models.UserAccount;
import common.interfaces.DatabaseService;
import utils.PasswordHasher;
import java.rmi.registry.LocateRegistry;
import java.rmi.registry.Registry;
import java.rmi.server.RMIClientSocketFactory;
import java.rmi.server.RMIServerSocketFactory;
import javax.rmi.ssl.SslRMIClientSocketFactory;
import javax.rmi.ssl.SslRMIServerSocketFactory;

/**
 * Application Server - Tier 2 of the 3-tier architecture.
 * Handles business logic and user sessions.
 * Connects to and delegates data operations to the Database Server.
 *
 * Supports both plain and SSL/TLS RMI modes.
 * SSL mode is enabled by setting -Dssl.enabled=true JVM property
 * along with keystore/truststore configuration.
 *
 * Usage:
 *   Plain:  java -cp out/ server.ServerMain [port] [dbHost] [dbPort]
 *   SSL:    java -cp out/ -Dssl.enabled=true \
 *             -Djavax.net.ssl.keyStore=certs/server.keystore \
 *             -Djavax.net.ssl.keyStorePassword=bhel2024 \
 *             server.ServerMain [port] [dbHost] [dbPort]
 *
 * Default Database Server: rmi://localhost:1098/DatabaseService
 */
public class ServerMain {

    public static final int DEFAULT_PORT = 1099;
    public static final String DEFAULT_DB_HOST = "localhost";
    public static final int DEFAULT_DB_PORT = 1098;

    public static void main(String[] args) {
        int port = DEFAULT_PORT;
        String dbHost = DEFAULT_DB_HOST;
        int dbPort = DEFAULT_DB_PORT;
        boolean sslEnabled = "true".equals(System.getProperty("ssl.enabled"));

        if (args.length >= 1) {
            try { port = Integer.parseInt(args[0]); } catch (NumberFormatException e) { /* use default */ }
        }
        if (args.length >= 2) {
            dbHost = args[1];
        }
        if (args.length >= 3) {
            try { dbPort = Integer.parseInt(args[2]); } catch (NumberFormatException e) { /* use default */ }
        }

        System.out.println("============================================");
        System.out.println("  BHEL HRM System - Application Server");
        System.out.println("  (3-Tier Architecture - Tier 2)");
        System.out.println("============================================");
        System.out.println("  Port:           " + port);
        System.out.println("  Database Host:  " + dbHost);
        System.out.println("  Database Port:  " + dbPort);
        System.out.println("  SSL/TLS:        " + (sslEnabled ? "ENABLED" : "DISABLED"));
        System.out.println("============================================");

        try {
            // Connect to Database Server
            String dbServiceURL = "rmi://" + dbHost + ":" + dbPort + "/DatabaseService";
            System.out.println("  Connecting to: " + dbServiceURL);
            Registry dbRegistry;
            if (sslEnabled) {
                dbRegistry = LocateRegistry.getRegistry(dbHost, dbPort, new javax.rmi.ssl.SslRMIClientSocketFactory());
                System.out.println("[SSL] Using SSL to connect to Database Server");
            } else {
                dbRegistry = LocateRegistry.getRegistry(dbHost, dbPort);
            }
            DatabaseService dbService = (DatabaseService) dbRegistry.lookup("DatabaseService");
            System.out.println("  Connected to Database Server successfully!");
            
            AuditLogger auditLogger = new AuditLogger(dbService);

            // SSL socket factories (null if SSL disabled)
            RMIClientSocketFactory csf = null;
            RMIServerSocketFactory ssf = null;

            if (sslEnabled) {
                csf = new SslRMIClientSocketFactory();
                ssf = new SslRMIServerSocketFactory(null, null, false);
                System.out.println("[SSL] Using SSL/TLS socket factories");
                System.out.println("[SSL] Protocol: TLSv1.2/TLSv1.3");
            }

            // Create service implementations (with or without SSL)
            AuthServiceImpl authService = new AuthServiceImpl(dbService, auditLogger, port + 1, csf, ssf);
            HRMServiceImpl hrmService = new HRMServiceImpl(dbService, authService, auditLogger, port + 2, csf, ssf);
            PRSServiceImpl prsService = new PRSServiceImpl(dbService, authService, auditLogger, port + 3, csf, ssf);

            // Create RMI registry
            Registry registry;
            if (sslEnabled) {
                registry = LocateRegistry.createRegistry(port, csf, ssf);
            } else {
                registry = LocateRegistry.createRegistry(port);
            }

            // Bind services to registry
            registry.rebind("AuthService", authService);
            registry.rebind("HRMService", hrmService);
            registry.rebind("PRSService", prsService);

            System.out.println("============================================");
            System.out.println("  Services registered successfully:");
            System.out.println("  - AuthService  (Authentication)");
            System.out.println("  - HRMService   (HR Management)");
            System.out.println("  - PRSService   (Payroll System)");
            if (sslEnabled) {
                System.out.println("");
                System.out.println("  All services secured with SSL/TLS");
                System.out.println("  Communication is encrypted end-to-end");
            }
            System.out.println("============================================");
            System.out.println("  Server is running on port " + port);
            System.out.println("  Press Ctrl+C to stop");
            System.out.println("============================================");

            auditLogger.logServerStart(port, sslEnabled);

        } catch (Exception e) {
            System.err.println("[SERVER] Fatal error: " + e.getMessage());
            e.printStackTrace();
            System.exit(1);
        }
    }
}
