package common.interfaces;

import java.rmi.Remote;
import java.rmi.RemoteException;
import java.util.List;
import common.models.PayrollRecord;

/**
 * Remote interface for the Payroll System (PRS).
 * This is a stub service demonstrating secure RMI communication
 * between the HRM system and an external Payroll System.
 *
 * Communication between HRM and PRS is secured via SSL/TLS.
 */
public interface PRSService extends Remote {

    /** Get payroll record for a specific employee, month, and year */
    PayrollRecord getPayrollRecord(int employeeId, int month, int year, String sessionToken) throws RemoteException;

    /** Get all payroll records for an employee in a given year */
    List<PayrollRecord> getYearlyPayroll(int employeeId, int year, String sessionToken) throws RemoteException;

    /** Generate payroll for an employee (HR/Admin only) */
    boolean generatePayroll(int employeeId, int month, int year, double basicSalary, String sessionToken) throws RemoteException;
}
