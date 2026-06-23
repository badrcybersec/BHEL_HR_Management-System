package client;

import common.models.*;
import utils.ValidationUtil;
import javax.swing.*;
import javax.swing.event.DocumentEvent;
import javax.swing.event.DocumentListener;
import javax.swing.table.DefaultTableModel;
import javax.swing.table.DefaultTableCellRenderer;
import java.awt.*;
import java.time.LocalDate;
import java.time.Year;
import java.time.temporal.ChronoUnit;
import java.util.List;

public class EmployeePanel extends JPanel {

    private final int employeeId;

    public EmployeePanel(int employeeId) {
        this.employeeId = employeeId;
        setLayout(new BorderLayout());
        setBackground(ClientMain.BG_DARK);
        add(ClientMain.createTopBar("Employee Dashboard", ClientMain.ACCENT_BLUE), BorderLayout.NORTH);

        JTabbedPane tabs = new JTabbedPane();
        tabs.setBackground(ClientMain.BG_PANEL);
        tabs.setFont(new Font("Segoe UI", Font.BOLD, 12));
        tabs.addTab("  My Profile  ", createProfileTab());
        tabs.addTab("  Family Details  ", createFamilyTab());
        tabs.addTab("  Leave  ", createLeaveTab());
        tabs.addTab("  Payroll  ", createPayrollTab());
        add(tabs, BorderLayout.CENTER);
    }

    private JTable styledTable(DefaultTableModel m) {
        JTable t = new JTable(m);
        t.setBackground(ClientMain.BG_PANEL); t.setForeground(ClientMain.FG_PRIMARY);
        t.setGridColor(ClientMain.BORDER_COLOR); t.setSelectionBackground(ClientMain.TABLE_SEL);
        t.setRowHeight(32); t.setFont(new Font("Segoe UI", Font.PLAIN, 12));
        t.getTableHeader().setBackground(ClientMain.BG_CARD); t.getTableHeader().setForeground(ClientMain.FG_SECONDARY);
        t.getTableHeader().setFont(new Font("Segoe UI", Font.BOLD, 11));
        t.setSelectionMode(ListSelectionModel.SINGLE_SELECTION); t.setShowVerticalLines(false);
        t.setDefaultRenderer(Object.class, new DefaultTableCellRenderer() {
            public Component getTableCellRendererComponent(JTable tb, Object v, boolean s, boolean f, int r, int c) {
                Component comp = super.getTableCellRendererComponent(tb, v, s, f, r, c);
                if (!s) comp.setBackground(r % 2 == 0 ? ClientMain.BG_PANEL : ClientMain.TABLE_ROW_ALT);
                comp.setForeground(ClientMain.FG_PRIMARY);
                setBorder(BorderFactory.createEmptyBorder(0, 8, 0, 8));
                return comp;
            }
        });
        return t;
    }
    private JScrollPane wrap(JTable t) {
        JScrollPane sp = new JScrollPane(t);
        sp.setBorder(BorderFactory.createLineBorder(ClientMain.BORDER_COLOR, 1));
        sp.getViewport().setBackground(ClientMain.BG_PANEL); return sp;
    }

    // ==================== PROFILE ====================
    private JPanel createProfileTab() {
        JPanel panel = new JPanel(new BorderLayout(10, 10));
        panel.setBackground(ClientMain.BG_DARK);
        panel.setBorder(BorderFactory.createEmptyBorder(20, 25, 20, 25));

        JPanel card = new JPanel(new GridBagLayout());
        card.setBackground(ClientMain.BG_PANEL);
        card.setBorder(BorderFactory.createCompoundBorder(
            BorderFactory.createLineBorder(ClientMain.BORDER_COLOR, 1), BorderFactory.createEmptyBorder(20, 25, 20, 25)));
        GridBagConstraints g = new GridBagConstraints();
        g.insets = new Insets(5, 8, 5, 8); g.fill = GridBagConstraints.HORIZONTAL;

        String[] labels = {"First Name", "Last Name", "IC/Passport", "Email", "Phone", "Department", "Position", "Date Joined"};
        JTextField[] fields = new JTextField[labels.length];
        for (int i = 0; i < labels.length; i++) {
            JLabel lbl = new JLabel(labels[i].toUpperCase());
            lbl.setFont(new Font("Segoe UI", Font.BOLD, 10)); lbl.setForeground(ClientMain.FG_DIM);
            g.gridx = (i % 2) * 2; g.gridy = i / 2 * 2; card.add(lbl, g);
            fields[i] = ClientMain.styledField(20);
            boolean editable = false;
            fields[i].setEditable(editable);
            if (!editable) { fields[i].setBackground(ClientMain.BG_CARD); fields[i].setForeground(ClientMain.FG_DIM); }
            g.gridy = i / 2 * 2 + 1; card.add(fields[i], g);
        }
        panel.add(card, BorderLayout.CENTER);

        JPanel btn = new JPanel(new FlowLayout(FlowLayout.RIGHT, 8, 0)); btn.setOpaque(false);
        btn.setBorder(BorderFactory.createEmptyBorder(12, 0, 0, 0));
        JButton refreshBtn = ClientMain.subtleButton("Refresh");
        JButton emailBtn = ClientMain.styledButton("Request Email Update", ClientMain.ACCENT_BLUE);
        JButton phoneBtn = ClientMain.styledButton("Request Phone Update", ClientMain.ACCENT_BLUE);
        btn.add(refreshBtn); btn.add(emailBtn); btn.add(phoneBtn);
        panel.add(btn, BorderLayout.SOUTH);

        Runnable load = () -> { try {
            Employee emp = ClientMain.hrmService.getProfile(employeeId, ClientMain.sessionToken);
            SwingUtilities.invokeLater(() -> {
                fields[0].setText(emp.getFirstName()); fields[1].setText(emp.getLastName());
                fields[2].setText(emp.getIcPassport()); fields[3].setText(emp.getEmail() != null ? emp.getEmail() : "");
                fields[4].setText(emp.getPhone() != null ? emp.getPhone() : "");
                fields[5].setText(emp.getDepartment() != null ? emp.getDepartment() : "");
                fields[6].setText(emp.getPosition() != null ? emp.getPosition() : "");
                fields[7].setText(emp.getDateJoined() != null ? emp.getDateJoined() : "");
            });
        } catch (Exception ex) { ClientMain.showError("Error: " + ex.getMessage()); }};
        new Thread(load).start();
        refreshBtn.addActionListener(e -> new Thread(load).start());
        emailBtn.addActionListener(e -> requestUpdate("email", fields[3].getText()));
        phoneBtn.addActionListener(e -> requestUpdate("phone", fields[4].getText()));
        return panel;
    }

    private void requestUpdate(String field, String curr) {
        String val = JOptionPane.showInputDialog(this, "Enter new " + field + ":", "Update " + field, JOptionPane.PLAIN_MESSAGE);
        if (val != null && !val.trim().isEmpty()) {
            try { ClientMain.hrmService.requestProfileUpdate(employeeId, field, curr, val.trim(), ClientMain.sessionToken);
                ClientMain.showSuccess("Update request submitted. Pending HR approval.");
            } catch (Exception ex) { 
                String errorMsg = ex.getMessage();
                if (errorMsg != null && errorMsg.contains(":")) {
                    errorMsg = errorMsg.substring(errorMsg.lastIndexOf(":") + 1).trim();
                }
                ClientMain.showError(errorMsg != null ? errorMsg : "An error occurred."); 
            }
        }
    }

    // ==================== FAMILY ====================
    private JPanel createFamilyTab() {
        JPanel panel = new JPanel(new BorderLayout(10, 10));
        panel.setBackground(ClientMain.BG_DARK); panel.setBorder(BorderFactory.createEmptyBorder(15, 20, 15, 20));

        String[] cols = {"ID", "Name", "Relationship", "IC/Passport", "Date of Birth"};
        DefaultTableModel model = new DefaultTableModel(cols, 0) { public boolean isCellEditable(int r, int c) { return false; } };
        JTable table = styledTable(model); panel.add(wrap(table), BorderLayout.CENTER);

        JPanel btn = new JPanel(new FlowLayout(FlowLayout.LEFT, 8, 0)); btn.setOpaque(false);
        btn.setBorder(BorderFactory.createEmptyBorder(10, 0, 0, 0));
        JButton addBtn = ClientMain.styledButton("+ Add", ClientMain.ACCENT_GREEN);
        JButton removeBtn = ClientMain.styledButton("Remove", ClientMain.ACCENT_RED);
        JButton refreshBtn = ClientMain.subtleButton("Refresh");
        btn.add(addBtn); btn.add(removeBtn); btn.add(refreshBtn); panel.add(btn, BorderLayout.SOUTH);

        Runnable load = () -> { try {
            List<FamilyMember> ms = ClientMain.hrmService.getFamilyMembers(employeeId, ClientMain.sessionToken);
            SwingUtilities.invokeLater(() -> { model.setRowCount(0);
                for (FamilyMember f : ms) model.addRow(new Object[]{f.getFamilyId(), f.getName(), f.getRelationship(), f.getIcPassport(), f.getDateOfBirth()});
            });
        } catch (Exception ex) { ClientMain.showError("Error: " + ex.getMessage()); }};
        new Thread(load).start();
        refreshBtn.addActionListener(e -> new Thread(load).start());

        addBtn.addActionListener(e -> {
            JTextField nf = ClientMain.styledField(18); JComboBox<String> rb = new JComboBox<>(new String[]{"SPOUSE","CHILD","PARENT","SIBLING","OTHER"});
            JTextField icf = ClientMain.styledField(18); JTextField df = ClientMain.styledField(18);
            int r = JOptionPane.showConfirmDialog(this, new Object[]{"Name:", nf, "Relationship:", rb, "IC/Passport:", icf, "DOB (YYYY-MM-DD):", df}, "Add Family Member", JOptionPane.OK_CANCEL_OPTION);
            if (r == JOptionPane.OK_OPTION) { try {
                String dob = df.getText().trim();
                
                // Validate DOB format
                if (!dob.isEmpty() && !dob.equals("DOB (YYYY-MM-DD)")) {
                    if (!ValidationUtil.validateDate(dob)) {
                        ClientMain.showError("Invalid date format. Use YYYY-MM-DD"); return;
                    }
                    // Check if date is valid and not in future
                    LocalDate dobDate = LocalDate.parse(dob);
                    if (dobDate.isAfter(LocalDate.now())) {
                        ClientMain.showError("Date of birth cannot be in the future."); return;
                    }
                    // Check for reasonable age (not more than 130 years old)
                    long age = ChronoUnit.YEARS.between(dobDate, LocalDate.now());
                    if (age > 130) {
                        ClientMain.showError("Please enter a valid date of birth."); return;
                    }
                }
                
                FamilyMember fm = new FamilyMember(0, employeeId, nf.getText().trim(), (String)rb.getSelectedItem(), icf.getText().trim(), dob.equals("DOB (YYYY-MM-DD)") ? "" : dob);
                ClientMain.hrmService.addFamilyMember(fm, ClientMain.sessionToken); ClientMain.showSuccess("Added!"); new Thread(load).start();
            } catch (Exception ex) { ClientMain.showError("Error: " + ex.getMessage()); }}
        });
        removeBtn.addActionListener(e -> { int row = table.getSelectedRow(); if (row < 0) { ClientMain.showError("Select a member."); return; }
            int fid = (int)model.getValueAt(row,0); if (JOptionPane.showConfirmDialog(this,"Remove?","Confirm",JOptionPane.YES_NO_OPTION)==0)
            { try { ClientMain.hrmService.removeFamilyMember(fid,employeeId,ClientMain.sessionToken); new Thread(load).start(); } catch(Exception ex){ClientMain.showError("Error: "+ex.getMessage());}}
        });
        return panel;
    }

    // ==================== LEAVE ====================
    private JPanel createLeaveTab() {
        JPanel panel = new JPanel(new BorderLayout(10, 10));
        panel.setBackground(ClientMain.BG_DARK); panel.setBorder(BorderFactory.createEmptyBorder(15, 20, 15, 20));

        JPanel balP = new JPanel(new GridLayout(1, 3, 12, 0)); balP.setOpaque(false); balP.setBorder(BorderFactory.createEmptyBorder(0,0,12,0));
        JLabel[] balL = new JLabel[3]; Color[] bc = {ClientMain.ACCENT_BLUE, ClientMain.ACCENT_GREEN, ClientMain.ACCENT_ORANGE};
        String[] bn = {"ANNUAL","MEDICAL","EMERGENCY"};
        for (int i = 0; i < 3; i++) {
            JPanel c = new JPanel(new BorderLayout()); c.setBackground(ClientMain.BG_PANEL);
            c.setBorder(BorderFactory.createCompoundBorder(BorderFactory.createLineBorder(ClientMain.BORDER_COLOR,1),BorderFactory.createEmptyBorder(12,16,12,16)));
            JLabel n = new JLabel(bn[i]); n.setFont(new Font("Segoe UI",Font.BOLD,10)); n.setForeground(bc[i]); c.add(n,BorderLayout.NORTH);
            balL[i] = new JLabel("..."); balL[i].setFont(new Font("Segoe UI",Font.BOLD,22)); balL[i].setForeground(ClientMain.FG_PRIMARY); c.add(balL[i],BorderLayout.CENTER);
            balP.add(c);
        }
        panel.add(balP, BorderLayout.NORTH);

        String[] cols = {"ID","Type","Start","End","Days","Status","Applied"};
        DefaultTableModel model = new DefaultTableModel(cols, 0) { public boolean isCellEditable(int r, int c) { return false; } };
        JTable table = styledTable(model);
        table.getColumnModel().getColumn(5).setCellRenderer(new DefaultTableCellRenderer() {
            public Component getTableCellRendererComponent(JTable t, Object v, boolean s, boolean f, int r, int c) {
                Component comp = super.getTableCellRendererComponent(t,v,s,f,r,c);
                comp.setBackground(s?ClientMain.TABLE_SEL:(r%2==0?ClientMain.BG_PANEL:ClientMain.TABLE_ROW_ALT));
                String st = v != null ? v.toString() : "";
                comp.setForeground("APPROVED".equals(st)?ClientMain.ACCENT_GREEN:"REJECTED".equals(st)?ClientMain.ACCENT_RED:"PENDING".equals(st)?ClientMain.ACCENT_ORANGE:ClientMain.FG_PRIMARY);
                setFont(new Font("Segoe UI",Font.BOLD,11)); setBorder(BorderFactory.createEmptyBorder(0,8,0,8)); return comp;
            }
        });
        panel.add(wrap(table), BorderLayout.CENTER);

        JPanel btn = new JPanel(new FlowLayout(FlowLayout.LEFT, 8, 0)); btn.setOpaque(false); btn.setBorder(BorderFactory.createEmptyBorder(10,0,0,0));
        JButton applyBtn = ClientMain.styledButton("+ Apply for Leave", ClientMain.ACCENT_BLUE);
        JButton refBtn = ClientMain.subtleButton("Refresh");
        btn.add(applyBtn); btn.add(refBtn); panel.add(btn, BorderLayout.SOUTH);

        Runnable load = () -> { try {
            int yr = Year.now().getValue();
            List<LeaveBalance> bals = ClientMain.hrmService.getLeaveBalances(employeeId, yr, ClientMain.sessionToken);
            List<LeaveApplication> apps = ClientMain.hrmService.getMyLeaveApplications(employeeId, ClientMain.sessionToken);
            SwingUtilities.invokeLater(() -> {
                for (LeaveBalance lb : bals) { String txt = lb.getRemainingDays()+" / "+lb.getTotalDays();
                    switch(lb.getLeaveType()) { case "ANNUAL":balL[0].setText(txt);break; case "MEDICAL":balL[1].setText(txt);break; case "EMERGENCY":balL[2].setText(txt);break; }}
                model.setRowCount(0);
                for (LeaveApplication la : apps) model.addRow(new Object[]{la.getLeaveId(),la.getLeaveType(),la.getStartDate(),la.getEndDate(),la.getDaysRequested(),la.getStatus(),la.getAppliedAt()});
            });
        } catch (Exception ex) { ClientMain.showError("Error: " + ex.getMessage()); }};
        new Thread(load).start(); refBtn.addActionListener(e -> new Thread(load).start());

        applyBtn.addActionListener(e -> {
            JComboBox<String> tb = new JComboBox<>(new String[]{"ANNUAL","MEDICAL","EMERGENCY"});
            JTextField sf = ClientMain.styledField(14); sf.setText(LocalDate.now().toString());
            JTextField ef = ClientMain.styledField(14); ef.setText(LocalDate.now().plusDays(1).toString());
            JLabel df = new JLabel("2"); df.setFont(new Font("Segoe UI", Font.BOLD, 12)); df.setForeground(ClientMain.FG_PRIMARY);
            JTextField rf = ClientMain.styledField(22);
            
            // Auto-calculate days when dates change
            DocumentListener dateListener = new DocumentListener() {
                void updateDays() {
                    try {
                        LocalDate start = LocalDate.parse(sf.getText().trim());
                        LocalDate end = LocalDate.parse(ef.getText().trim());
                        if (end.isBefore(start)) { df.setText("Invalid"); df.setForeground(ClientMain.ACCENT_RED); }
                        else { long days = ChronoUnit.DAYS.between(start, end) + 1;
                            df.setText(String.valueOf(days)); df.setForeground(ClientMain.FG_PRIMARY); }
                    } catch (Exception ex) { df.setText("Invalid"); df.setForeground(ClientMain.ACCENT_RED); }
                }
                public void insertUpdate(DocumentEvent e) { updateDays(); }
                public void removeUpdate(DocumentEvent e) { updateDays(); }
                public void changedUpdate(DocumentEvent e) { updateDays(); }
            };
            sf.getDocument().addDocumentListener(dateListener);
            ef.getDocument().addDocumentListener(dateListener);
            
            int res = JOptionPane.showConfirmDialog(this, new Object[]{"Type:",tb,"Start:",sf,"End:",ef,"Days:  ",df,"Reason:",rf},"Apply for Leave",JOptionPane.OK_CANCEL_OPTION);
            if (res == JOptionPane.OK_OPTION) { try {
                LeaveApplication a = new LeaveApplication(); a.setEmployeeId(employeeId);
                a.setLeaveType((String)tb.getSelectedItem()); a.setStartDate(sf.getText().trim()); a.setEndDate(ef.getText().trim());
                int days = Integer.parseInt(df.getText().trim());
                a.setDaysRequested(days); a.setReason(rf.getText().trim());
                ClientMain.hrmService.applyForLeave(a, ClientMain.sessionToken); ClientMain.showSuccess("Submitted!"); new Thread(load).start();
            } catch (Exception ex) { ClientMain.showError("Error: " + ex.getMessage()); }}
        });
        return panel;
    }

    // ==================== PAYROLL (NEW) ====================
    private JPanel createPayrollTab() {
        JPanel panel = new JPanel(new BorderLayout(10, 10));
        panel.setBackground(ClientMain.BG_DARK); panel.setBorder(BorderFactory.createEmptyBorder(15, 20, 15, 20));

        JPanel top = new JPanel(new FlowLayout(FlowLayout.LEFT, 8, 0)); top.setOpaque(false);
        top.setBorder(BorderFactory.createEmptyBorder(0, 0, 10, 0));
        JLabel yl = new JLabel("Year:"); yl.setForeground(ClientMain.FG_SECONDARY);
        JTextField yearF = ClientMain.styledField(6); yearF.setText(String.valueOf(Year.now().getValue()));
        JButton loadBtn = ClientMain.styledButton("Load Payroll", ClientMain.ACCENT_PURPLE);
        top.add(yl); top.add(yearF); top.add(loadBtn);
        panel.add(top, BorderLayout.NORTH);

        String[] cols = {"Month", "Basic Salary (RM)", "Deductions (RM)", "Net Salary (RM)", "Generated"};
        DefaultTableModel model = new DefaultTableModel(cols, 0) { public boolean isCellEditable(int r, int c) { return false; } };
        JTable table = styledTable(model); panel.add(wrap(table), BorderLayout.CENTER);

        // Summary panel
        JPanel summary = new JPanel(new GridLayout(1, 3, 12, 0)); summary.setOpaque(false);
        summary.setBorder(BorderFactory.createEmptyBorder(10, 0, 0, 0));
        JLabel totalGross = new JLabel("Total Gross: -"); totalGross.setForeground(ClientMain.ACCENT_BLUE); totalGross.setFont(new Font("Segoe UI",Font.BOLD,13));
        JLabel totalDed = new JLabel("Total Deductions: -"); totalDed.setForeground(ClientMain.ACCENT_ORANGE); totalDed.setFont(new Font("Segoe UI",Font.BOLD,13));
        JLabel totalNet = new JLabel("Total Net: -"); totalNet.setForeground(ClientMain.ACCENT_GREEN); totalNet.setFont(new Font("Segoe UI",Font.BOLD,13));
        summary.add(totalGross); summary.add(totalDed); summary.add(totalNet);
        panel.add(summary, BorderLayout.SOUTH);

        String[] months = {"","Jan","Feb","Mar","Apr","May","Jun","Jul","Aug","Sep","Oct","Nov","Dec"};
        loadBtn.addActionListener(e -> {
            try {
                int year = Integer.parseInt(yearF.getText().trim());
                List<PayrollRecord> records = ClientMain.prsService.getYearlyPayroll(employeeId, year, ClientMain.sessionToken);
                model.setRowCount(0);
                double sumG = 0, sumD = 0, sumN = 0;
                for (PayrollRecord pr : records) {
                    model.addRow(new Object[]{months[pr.getMonth()], String.format("%.2f", pr.getBasicSalary()),
                        String.format("%.2f", pr.getDeductions()), String.format("%.2f", pr.getNetSalary()), pr.getGeneratedAt()});
                    sumG += pr.getBasicSalary(); sumD += pr.getDeductions(); sumN += pr.getNetSalary();
                }
                totalGross.setText("Total Gross: RM " + String.format("%.2f", sumG));
                totalDed.setText("Total Deductions: RM " + String.format("%.2f", sumD));
                totalNet.setText("Total Net: RM " + String.format("%.2f", sumN));
                if (records.isEmpty()) ClientMain.showError("No payroll records found for " + year);
            } catch (NumberFormatException ex) { ClientMain.showError("Enter a valid year."); }
            catch (Exception ex) { ClientMain.showError("Error: " + ex.getMessage()); }
        });
        return panel;
    }
}
