package server;

import common.interfaces.DatabaseService;
import common.models.UserAccount;
import utils.PasswordHasher;
import java.rmi.registry.LocateRegistry;
import java.rmi.registry.Registry;
import java.rmi.server.RMIClientSocketFactory;
import java.rmi.server.RMIServerSocketFactory;
import javax.rmi.ssl.SslRMIClientSocketFactory;
import javax.rmi.ssl.SslRMIServerSocketFactory;

/**
 * Database Server - Tier 3 of the 3-tier architecture.
 * Handles all CSV data operations and maintains audit logs.
 *
 * FAULT TOLERANCE: When started with --backup, this server acts as the
 * PRIMARY database and replicates every write to a backup DatabaseServer.
 * Without --backup, it runs as a standalone (or backup) database server.
 *
 * Usage:
 *   Standalone/Backup: java -cp out/ server.DatabaseServer [port] [dataDir]
 *   Primary:           java -cp out/ server.DatabaseServer [port] [dataDir] --backup host:port
 *
 * Examples:
 *   Laptop A (primary):  java -cp out/ server.DatabaseServer 1098 data --backup 192.168.1.20:2098
 *   Laptop B (backup):   java -cp out/ server.DatabaseServer 2098 data_backup
 */
public class DatabaseServer {

    public static final int DEFAULT_PORT = 1098;
    public static final String DEFAULT_DATA_DIR = "data";

    public static void main(String[] args) {
        int port = DEFAULT_PORT;
        String dataDir = DEFAULT_DATA_DIR;
        String backupHost = null;
        int backupPort = 0;
        boolean sslEnabled = "true".equals(System.getProperty("ssl.enabled"));

        // Parse arguments
        for (int i = 0; i < args.length; i++) {
            if ("--backup".equals(args[i]) && i + 1 < args.length) {
                String[] parts = args[i + 1].split(":");
                backupHost = parts[0];
                backupPort = Integer.parseInt(parts[1]);
                i++;
            } else if (i == 0) {
                try { port = Integer.parseInt(args[0]); } catch (NumberFormatException e) {}
            } else if (i == 1 && !args[i].startsWith("--")) {
                dataDir = args[i];
            }
        }

        boolean isPrimary = (backupHost != null);
        String mode = isPrimary ? "PRIMARY" : "STANDALONE / BACKUP";

        System.out.println("============================================");
        System.out.println("  BHEL HRM System - Database Server");
        System.out.println("  (3-Tier Architecture - Tier 3)");
        System.out.println("============================================");
        System.out.println("  Mode:      " + mode);
        System.out.println("  Port:      " + port);
        System.out.println("  Data Dir:  " + dataDir);
        System.out.println("  SSL/TLS:   " + (sslEnabled ? "ENABLED" : "DISABLED"));
        if (isPrimary) {
            System.out.println("  Backup:    " + backupHost + ":" + backupPort);
        }
        System.out.println("============================================");

        try {
            // Initialize CSV data store
            CSVDataStore dataStore = new CSVDataStore(dataDir);

            // Seed default data if no users exist
            seedDefaultData(dataStore);

            // SSL socket factories (null if SSL disabled)
            RMIClientSocketFactory csf = null;
            RMIServerSocketFactory ssf = null;

            if (sslEnabled) {
                csf = new SslRMIClientSocketFactory();
                ssf = new SslRMIServerSocketFactory(null, null, false);
                System.out.println("[SSL] Using SSL/TLS socket factories");
            }

            // Create database service
            DatabaseServiceImpl dbService = new DatabaseServiceImpl(dataStore, dataDir, port, csf, ssf);

            // Create RMI registry
            Registry registry;
            if (sslEnabled) {
                registry = LocateRegistry.createRegistry(port, csf, ssf);
            } else {
                registry = LocateRegistry.createRegistry(port);
            }

            // Bind database service to registry
            registry.rebind("DatabaseService", dbService);

            System.out.println("============================================");
            System.out.println("  Database Service registered successfully");
            System.out.println("============================================");
            System.out.println("  Database Server listening on port " + port);
            System.out.println("  Press Ctrl+C to stop");
            System.out.println("============================================");

            dataStore.addAuditLog(0, "SYSTEM", "SYSTEM", "DB_SERVER_START", null, 0,
                "Database server started on port " + port + " [" + mode + "]");

            // PRIMARY: start replication manager
            if (isPrimary) {
                startReplicationManager(dataStore, backupHost, backupPort);
            }

        } catch (Exception e) {
            System.err.println("[DATABASE_SERVER] Fatal error: " + e.getMessage());
            e.printStackTrace();
            System.exit(1);
        }
    }

    /**
     * Background thread that manages replication to the backup DatabaseServer.
     * Connects, pings every 15 seconds, reconnects if backup goes down.
     * Primary never blocks or crashes due to backup unavailability.
     */
    private static void startReplicationManager(CSVDataStore dataStore,
                                                 String backupHost, int backupPort) {
        Thread t = new Thread(() -> {
            System.out.println("[REPLICATION] Connecting to backup at " + backupHost + ":" + backupPort + "...");

            while (true) {
                try {
                    Registry backupRegistry;
                    if ("true".equals(System.getProperty("ssl.enabled"))) {
                        backupRegistry = LocateRegistry.getRegistry(backupHost, backupPort, new javax.rmi.ssl.SslRMIClientSocketFactory());
                    } else {
                        backupRegistry = LocateRegistry.getRegistry(backupHost, backupPort);
                    }
                    DatabaseService backupDb =
                        (DatabaseService) backupRegistry.lookup("DatabaseService");

                    backupDb.ping();
                    dataStore.setReplicator(backupDb);

                    System.out.println("============================================");
                    System.out.println("  [REPLICATION] Connected to backup!");
                    System.out.println("  [REPLICATION] All writes now replicated");
                    System.out.println("============================================");

                    // Health monitoring — ping every 15 seconds
                    while (true) {
                        Thread.sleep(15000);
                        try {
                            backupDb.ping();
                        } catch (Exception e) {
                            System.err.println("[REPLICATION] Backup lost! Continuing standalone...");
                            dataStore.setReplicator(null);
                            break;
                        }
                    }
                } catch (Exception e) {
                    dataStore.setReplicator(null);
                    System.out.println("[REPLICATION] Backup not available. Retrying in 5s...");
                    try { Thread.sleep(5000); } catch (InterruptedException ie) { break; }
                }
            }
        }, "ReplicationManager");
        t.setDaemon(true);
        t.start();
    }

    private static void seedDefaultData(CSVDataStore dataStore) {
        if (!dataStore.getAllUsers().isEmpty()) {
            System.out.println("[DB] Existing data found - skipping seed");
            return;
        }

        System.out.println("[DB] No users found - seeding default data...");

        UserAccount admin = new UserAccount(0, "admin",
            PasswordHasher.hashPassword("admin123"), "ADMIN", 0, true);
        dataStore.addUser(admin);
        System.out.println("[DB]   admin / admin123");

        UserAccount hr = new UserAccount(0, "hr1",
            PasswordHasher.hashPassword("hr1234"), "HR", 0, true);
        dataStore.addUser(hr);
        System.out.println("[DB]   hr1 / hr1234");

        common.models.Employee emp = new common.models.Employee();
        emp.setFirstName("Ahmad");
        emp.setLastName("Ibrahim");
        emp.setIcPassport("990101-14-1234");
        emp.setEmail("ahmad@bhel.com");
        emp.setPhone("+60123456789");
        emp.setDepartment("Engineering");
        emp.setPosition("Software Developer");
        emp.setDateJoined("2024-01-15");
        emp.setActive(true);
        int empId = dataStore.addEmployee(emp);

        UserAccount empUser = new UserAccount(0, "ahmad.ibrahim",
            PasswordHasher.hashPassword("emp123"), "EMPLOYEE", empId, true);
        dataStore.addUser(empUser);
        System.out.println("[DB]   ahmad.ibrahim / emp123");

        System.out.println("[DB] Seed complete");
    }
}
