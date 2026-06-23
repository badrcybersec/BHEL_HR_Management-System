package server;

import common.interfaces.PRSService;
import common.interfaces.DatabaseService;
import common.models.PayrollRecord;
import common.models.UserAccount;
import java.rmi.RemoteException;
import java.rmi.server.RMIClientSocketFactory;
import java.rmi.server.RMIServerSocketFactory;
import java.rmi.server.UnicastRemoteObject;
import java.util.List;

/**
 * Payroll System (PRS) implementation.
 * Handles payroll record management and reporting.
 * Delegates data operations to remote DatabaseService.
 * Supports SSL/TLS when socket factories are provided.
 */
public class PRSServiceImpl extends UnicastRemoteObject implements PRSService {

    private final DatabaseService dbService;
    private final AuthServiceImpl authService;
    private final AuditLogger auditLogger;

    public PRSServiceImpl(DatabaseService dbService, AuthServiceImpl authService,
                          AuditLogger auditLogger, int port,
                          RMIClientSocketFactory csf, RMIServerSocketFactory ssf) throws RemoteException {
        super(port, csf, ssf);
        this.dbService = dbService;
        this.authService = authService;
        this.auditLogger = auditLogger;
    }

    @Override
    public PayrollRecord getPayrollRecord(int employeeId, int month, int year,
                                           String sessionToken) throws RemoteException {
        UserAccount user = authService.validateSession(sessionToken);
        if (user == null) throw new RemoteException("Unauthorized");
        if (user.getRole().equals("EMPLOYEE") && user.getEmployeeId() != employeeId)
            throw new RemoteException("Forbidden: Cannot view another employee's payroll");

        PayrollRecord record = dbService.getPayrollRecord(employeeId, month, year);
        if (record == null)
            throw new RemoteException("No payroll record for employee " + employeeId + " in " + month + "/" + year);
        return record;
    }

    @Override
    public List<PayrollRecord> getYearlyPayroll(int employeeId, int year,
                                                 String sessionToken) throws RemoteException {
        UserAccount user = authService.validateSession(sessionToken);
        if (user == null) throw new RemoteException("Unauthorized");
        if (user.getRole().equals("EMPLOYEE") && user.getEmployeeId() != employeeId)
            throw new RemoteException("Forbidden");
        return dbService.getYearlyPayroll(employeeId, year);
    }

    @Override
    public synchronized boolean generatePayroll(int employeeId, int month, int year,
                                                 double basicSalary, String sessionToken) throws RemoteException {
        UserAccount user = authService.validateSession(sessionToken);
        if (user == null) throw new RemoteException("Unauthorized");
        if (!user.getRole().equals("HR") && !user.getRole().equals("ADMIN"))
            throw new RemoteException("Forbidden: Only HR/Admin can generate payroll");

        if (dbService.getPayrollRecord(employeeId, month, year) != null)
            throw new RemoteException("Payroll already exists for " + month + "/" + year);

        if (dbService.getEmployee(employeeId) == null)
        throw new RemoteException("Employee not found: " + employeeId);
    
        // Simple payroll calculation
        double epf = basicSalary * 0.11;      // EPF 11%
        double socso = basicSalary * 0.005;    // SOCSO 0.5%
        double eis = basicSalary * 0.002;      // EIS 0.2%
        double totalDeductions = epf + socso + eis;
        double netSalary = basicSalary - totalDeductions;

        PayrollRecord record = new PayrollRecord(0, employeeId, month, year,
            basicSalary, Math.round(totalDeductions * 100.0) / 100.0,
            Math.round(netSalary * 100.0) / 100.0, null);
        int id = dbService.addPayrollRecord(record);

        auditLogger.log(user, "GENERATE_PAYROLL", "payroll_records", id,
            "emp#" + employeeId + " " + month + "/" + year + ": RM" + basicSalary + " -> RM" + netSalary);
        return true;
    }
}
