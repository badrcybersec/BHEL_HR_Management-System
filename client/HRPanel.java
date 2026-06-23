package client;

import common.models.*;
import javax.swing.*;
import javax.swing.table.DefaultTableModel;
import javax.swing.table.DefaultTableCellRenderer;
import java.awt.*;
import java.io.*;
import java.time.Year;
import java.util.List;

public class HRPanel extends JPanel {

    public HRPanel() {
        setLayout(new BorderLayout()); setBackground(ClientMain.BG_DARK);
        add(ClientMain.createTopBar("HR Staff Dashboard", ClientMain.ACCENT_GREEN), BorderLayout.NORTH);
        JTabbedPane tabs = new JTabbedPane();
        tabs.setBackground(ClientMain.BG_PANEL); tabs.setFont(new Font("Segoe UI", Font.BOLD, 12));
        tabs.addTab("  + Register  ", createRegisterTab());
        tabs.addTab("  Employees  ", createEmployeeListTab());
        tabs.addTab("  Profile Updates  ", createProfileUpdatesTab());
        tabs.addTab("  Leave Apps  ", createLeaveTab());
        tabs.addTab("  Payroll  ", createPayrollTab());
        tabs.addTab("  Reports  ", createReportTab());
        add(tabs, BorderLayout.CENTER);
    }

    private JTable styledTable(DefaultTableModel m) {
        JTable t = new JTable(m); t.setBackground(ClientMain.BG_PANEL); t.setForeground(ClientMain.FG_PRIMARY);
        t.setGridColor(ClientMain.BORDER_COLOR); t.setSelectionBackground(ClientMain.TABLE_SEL); t.setRowHeight(32);
        t.setFont(new Font("Segoe UI",Font.PLAIN,12)); t.getTableHeader().setBackground(ClientMain.BG_CARD);
        t.getTableHeader().setForeground(ClientMain.FG_SECONDARY); t.getTableHeader().setFont(new Font("Segoe UI",Font.BOLD,11));
        t.setSelectionMode(ListSelectionModel.SINGLE_SELECTION); t.setShowVerticalLines(false);
        t.setDefaultRenderer(Object.class, new DefaultTableCellRenderer() {
            public Component getTableCellRendererComponent(JTable tb, Object v, boolean s, boolean f, int r, int c) {
                Component comp = super.getTableCellRendererComponent(tb,v,s,f,r,c);
                if (!s) comp.setBackground(r%2==0?ClientMain.BG_PANEL:ClientMain.TABLE_ROW_ALT);
                comp.setForeground(ClientMain.FG_PRIMARY); setBorder(BorderFactory.createEmptyBorder(0,8,0,8)); return comp;
            }
        }); return t;
    }
    private JScrollPane wrap(JTable t) { JScrollPane sp = new JScrollPane(t); sp.setBorder(BorderFactory.createLineBorder(ClientMain.BORDER_COLOR,1)); sp.getViewport().setBackground(ClientMain.BG_PANEL); return sp; }

    // ==================== REGISTER ====================
    private JPanel createRegisterTab() {
        JPanel panel = new JPanel(new GridBagLayout()); panel.setBackground(ClientMain.BG_DARK);
        JPanel card = new JPanel(new GridBagLayout()); card.setBackground(ClientMain.BG_PANEL);
        card.setBorder(BorderFactory.createCompoundBorder(BorderFactory.createLineBorder(ClientMain.BORDER_COLOR,1),BorderFactory.createEmptyBorder(25,35,25,35)));
        GridBagConstraints g = new GridBagConstraints(); g.fill = GridBagConstraints.HORIZONTAL;

        JLabel hdr = new JLabel("Register New Employee"); hdr.setFont(new Font("Segoe UI",Font.BOLD,18)); hdr.setForeground(ClientMain.FG_PRIMARY);
        g.gridx=0; g.gridy=0; g.gridwidth=4; g.insets=new Insets(0,8,12,8); card.add(hdr,g); g.gridwidth=1;

        // Department and position mapping
        java.util.Map<String, String[]> deptPositions = new java.util.LinkedHashMap<>();
        deptPositions.put("Engineering", new String[]{"Software Developer", "Hardware Engineer"});
        deptPositions.put("Manufacturing", new String[]{"Production Manager", "Machine Operator"});
        deptPositions.put("Office", new String[]{"Office Manager", "Office Assistant"});
        deptPositions.put("Sanitation", new String[]{"Janitor", "Cleaner"});

        String[] labels={"First Name *","Last Name *","IC/Passport *","Email","Phone","Department","Position"};
        JComponent[] components = new JComponent[labels.length];
        
        // Create text fields for basic info
        for (int i=0;i<5;i++) {
            components[i] = ClientMain.styledField(18);
        }
        
        // Create dropdowns for Department and Position
        JComboBox<String> deptCombo = new JComboBox<>(deptPositions.keySet().toArray(new String[0]));
        deptCombo.setBackground(ClientMain.BG_INPUT); deptCombo.setForeground(ClientMain.FG_PRIMARY);
        components[5] = deptCombo;
        
        JComboBox<String> posCombo = new JComboBox<>();
        posCombo.setBackground(ClientMain.BG_INPUT); posCombo.setForeground(ClientMain.FG_PRIMARY);
        components[6] = posCombo;
        
        // Update positions when department changes
        deptCombo.addActionListener(e -> {
            String selectedDept = (String) deptCombo.getSelectedItem();
            String[] positions = deptPositions.get(selectedDept);
            posCombo.removeAllItems();
            for (String pos : positions) {
                posCombo.addItem(pos);
            }
        });
        // Initialize position dropdown with first department's positions
        deptCombo.setSelectedIndex(0);
        
        // Add labels and components to card
        for (int i=0;i<labels.length;i++) {
            int col=(i%2)*2, row=(i/2)+1;
            JLabel l = new JLabel(labels[i].toUpperCase()); l.setFont(new Font("Segoe UI",Font.BOLD,10)); l.setForeground(ClientMain.FG_DIM);
            g.gridx=col; g.gridy=row*2; g.insets=new Insets(8,8,2,8); card.add(l,g);
            g.gridy=row*2+1; g.insets=new Insets(0,8,4,8); card.add(components[i],g);
        }

        JButton regBtn = ClientMain.styledButton("  Register Employee  ", ClientMain.ACCENT_GREEN);
        JButton clearBtn = ClientMain.subtleButton("  Clear Form  ");
        JLabel res = new JLabel(" "); res.setFont(new Font("Segoe UI",Font.PLAIN,12));
        int lr=(labels.length/2)+2; g.gridx=0; g.gridy=lr*2; g.gridwidth=2; g.insets=new Insets(16,8,6,8); card.add(regBtn,g);
        g.gridx=2; card.add(clearBtn,g);
        g.gridx=0; g.gridy=lr*2+1; g.gridwidth=4; g.insets=new Insets(4,8,4,8); card.add(res,g); panel.add(card);

        clearBtn.addActionListener(e -> { 
            for (int i=0;i<5;i++) ((JTextField)components[i]).setText(""); 
            deptCombo.setSelectedIndex(0);
            res.setText(" "); 
        });

        regBtn.addActionListener(e -> { try {
            Employee emp = new Employee(); 
            emp.setFirstName(((JTextField)components[0]).getText().trim()); 
            emp.setLastName(((JTextField)components[1]).getText().trim());
            emp.setIcPassport(((JTextField)components[2]).getText().trim());
            emp.setEmail(((JTextField)components[3]).getText().trim().isEmpty()?null:((JTextField)components[3]).getText().trim());
            emp.setPhone(((JTextField)components[4]).getText().trim().isEmpty()?null:((JTextField)components[4]).getText().trim());
            emp.setDepartment((String)deptCombo.getSelectedItem());
            emp.setPosition((String)posCombo.getSelectedItem());
            int id = ClientMain.hrmService.registerEmployee(emp, ClientMain.sessionToken);
            res.setForeground(ClientMain.ACCENT_GREEN);
            res.setText("Registered! ID:"+id+" | User: "+emp.getFirstName().toLowerCase()+"."+emp.getLastName().toLowerCase()+" | PW: IC without dashes");
            for (int i=0;i<5;i++) ((JTextField)components[i]).setText("");
            deptCombo.setSelectedIndex(0);
        } catch (Exception ex) { res.setForeground(ClientMain.ACCENT_RED); 
            String msg = ex.getMessage();
            if (msg != null && msg.contains(":")) { msg = msg.substring(msg.lastIndexOf(":")+1).trim(); }
            if (msg == null || msg.isEmpty()) { msg = "Invalid input"; }
            res.setText(msg); 
        }});
        return panel;
    }

    // ==================== EMPLOYEE LIST ====================
    private JPanel createEmployeeListTab() {
        JPanel panel = new JPanel(new BorderLayout(10,10)); panel.setBackground(ClientMain.BG_DARK);
        panel.setBorder(BorderFactory.createEmptyBorder(15,20,15,20));
        JPanel sb = new JPanel(new FlowLayout(FlowLayout.LEFT,8,0)); sb.setOpaque(false); sb.setBorder(BorderFactory.createEmptyBorder(0,0,10,0));
        JTextField sf = ClientMain.styledField(25); JButton sBtn = ClientMain.styledButton("Search",ClientMain.ACCENT_BLUE); JButton aBtn = ClientMain.subtleButton("Show All");
        sb.add(sf); sb.add(sBtn); sb.add(aBtn); panel.add(sb,BorderLayout.NORTH);
        String[] cols={"ID","First Name","Last Name","IC/Passport","Email","Department","Position"};
        DefaultTableModel model = new DefaultTableModel(cols,0){public boolean isCellEditable(int r,int c){return false;}};
        panel.add(wrap(styledTable(model)),BorderLayout.CENTER);
        Runnable load = ()->{try{List<Employee> es=ClientMain.hrmService.getAllEmployees(ClientMain.sessionToken);SwingUtilities.invokeLater(()->{model.setRowCount(0);for(Employee e:es)model.addRow(new Object[]{e.getEmployeeId(),e.getFirstName(),e.getLastName(),e.getIcPassport(),e.getEmail(),e.getDepartment(),e.getPosition()});});}catch(Exception ex){ClientMain.showError("Error: "+ex.getMessage());}};
        aBtn.addActionListener(e->new Thread(load).start());
        sBtn.addActionListener(e->{String q=sf.getText().trim();if(q.isEmpty()){new Thread(load).start();return;}new Thread(()->{try{List<Employee> rs=ClientMain.hrmService.searchEmployees(q,ClientMain.sessionToken);SwingUtilities.invokeLater(()->{model.setRowCount(0);for(Employee emp:rs)model.addRow(new Object[]{emp.getEmployeeId(),emp.getFirstName(),emp.getLastName(),emp.getIcPassport(),emp.getEmail(),emp.getDepartment(),emp.getPosition()});});}catch(Exception ex){ClientMain.showError("Error: "+ex.getMessage());}}).start();});
        new Thread(load).start(); return panel;
    }

    // ==================== PROFILE UPDATES ====================
    private JPanel createProfileUpdatesTab() {
        JPanel panel = new JPanel(new BorderLayout(10,10)); panel.setBackground(ClientMain.BG_DARK); panel.setBorder(BorderFactory.createEmptyBorder(15,20,15,20));
        String[] cols={"Req ID","Emp ID","Field","Old Value","New Value","Status","Requested At"};
        DefaultTableModel model = new DefaultTableModel(cols,0){public boolean isCellEditable(int r,int c){return false;}};
        JTable table = styledTable(model); panel.add(wrap(table),BorderLayout.CENTER);
        JPanel bp = new JPanel(new FlowLayout(FlowLayout.LEFT,8,0)); bp.setOpaque(false); bp.setBorder(BorderFactory.createEmptyBorder(10,0,0,0));
        JButton ab = ClientMain.styledButton("Approve",ClientMain.ACCENT_GREEN); JButton rb = ClientMain.styledButton("Reject",ClientMain.ACCENT_RED);
        JButton rfb = ClientMain.subtleButton("Refresh"); bp.add(ab);bp.add(rb);bp.add(rfb); panel.add(bp,BorderLayout.SOUTH);
        Runnable load = ()->{try{List<String[]> u=ClientMain.hrmService.getPendingProfileUpdates(ClientMain.sessionToken);SwingUtilities.invokeLater(()->{model.setRowCount(0);for(String[] r:u)model.addRow(new Object[]{r[0],r[1],r[2],r[3],r[4],r[5],r[6]});});}catch(Exception ex){ClientMain.showError("Error: "+ex.getMessage());}};
        new Thread(load).start(); rfb.addActionListener(e->new Thread(load).start());
        ab.addActionListener(e->{int row=table.getSelectedRow();if(row<0){ClientMain.showError("Select a request.");return;}try{int id=Integer.parseInt(model.getValueAt(row,0).toString().trim());ClientMain.hrmService.approveProfileUpdate(id,ClientMain.sessionToken);ClientMain.showSuccess("Approved!");new Thread(load).start();}catch(Exception ex){ClientMain.showError("Error: "+ex.getMessage());}});
        rb.addActionListener(e->{int row=table.getSelectedRow();if(row<0){ClientMain.showError("Select a request.");return;}try{int id=Integer.parseInt(model.getValueAt(row,0).toString().trim());ClientMain.hrmService.rejectProfileUpdate(id,ClientMain.sessionToken);ClientMain.showSuccess("Rejected.");new Thread(load).start();}catch(Exception ex){ClientMain.showError("Error: "+ex.getMessage());}});
        return panel;
    }

    // ==================== LEAVE APPLICATIONS ====================
    private JPanel createLeaveTab() {
        JPanel panel = new JPanel(new BorderLayout(10,10)); panel.setBackground(ClientMain.BG_DARK); panel.setBorder(BorderFactory.createEmptyBorder(15,20,15,20));
        String[] cols={"ID","Employee","Type","Start","End","Days","Reason","Status"};
        DefaultTableModel model = new DefaultTableModel(cols,0){public boolean isCellEditable(int r,int c){return false;}};
        JTable table = styledTable(model);
        table.getColumnModel().getColumn(7).setCellRenderer(new DefaultTableCellRenderer(){
            public Component getTableCellRendererComponent(JTable t,Object v,boolean s,boolean f,int r,int c){
                Component comp=super.getTableCellRendererComponent(t,v,s,f,r,c);
                comp.setBackground(s?ClientMain.TABLE_SEL:(r%2==0?ClientMain.BG_PANEL:ClientMain.TABLE_ROW_ALT));
                String st=v!=null?v.toString():"";comp.setForeground("PENDING".equals(st)?ClientMain.ACCENT_ORANGE:"APPROVED".equals(st)?ClientMain.ACCENT_GREEN:"REJECTED".equals(st)?ClientMain.ACCENT_RED:ClientMain.FG_PRIMARY);
                setFont(new Font("Segoe UI",Font.BOLD,11));setBorder(BorderFactory.createEmptyBorder(0,8,0,8));return comp;}});
        panel.add(wrap(table),BorderLayout.CENTER);
        JPanel bp = new JPanel(new FlowLayout(FlowLayout.LEFT,8,0)); bp.setOpaque(false); bp.setBorder(BorderFactory.createEmptyBorder(10,0,0,0));
        JButton ab = ClientMain.styledButton("Approve",ClientMain.ACCENT_GREEN); JButton rb = ClientMain.styledButton("Reject",ClientMain.ACCENT_RED);
        JButton rfb = ClientMain.subtleButton("Refresh"); bp.add(ab);bp.add(rb);bp.add(rfb); panel.add(bp,BorderLayout.SOUTH);
        Runnable load = ()->{try{List<LeaveApplication> as=ClientMain.hrmService.getPendingLeaveApplications(ClientMain.sessionToken);SwingUtilities.invokeLater(()->{model.setRowCount(0);for(LeaveApplication la:as)model.addRow(new Object[]{la.getLeaveId(),la.getEmployeeName()!=null?la.getEmployeeName():"Emp#"+la.getEmployeeId(),la.getLeaveType(),la.getStartDate(),la.getEndDate(),la.getDaysRequested(),la.getReason(),la.getStatus()});});}catch(Exception ex){ClientMain.showError("Error: "+ex.getMessage());}};
        new Thread(load).start(); rfb.addActionListener(e->new Thread(load).start());
        ab.addActionListener(e->{int row=table.getSelectedRow();if(row<0){ClientMain.showError("Select a leave.");return;}try{ClientMain.hrmService.approveLeave((int)model.getValueAt(row,0),ClientMain.sessionToken);ClientMain.showSuccess("Approved!");new Thread(load).start();}catch(Exception ex){ClientMain.showError("Error: "+ex.getMessage());}});
        rb.addActionListener(e->{int row=table.getSelectedRow();if(row<0){ClientMain.showError("Select a leave.");return;}try{ClientMain.hrmService.rejectLeave((int)model.getValueAt(row,0),ClientMain.sessionToken);ClientMain.showSuccess("Rejected.");new Thread(load).start();}catch(Exception ex){ClientMain.showError("Error: "+ex.getMessage());}});
        return panel;
    }

    // ==================== PAYROLL GENERATION (NEW) ====================
    private JPanel createPayrollTab() {
        JPanel panel = new JPanel(new BorderLayout(10,10)); panel.setBackground(ClientMain.BG_DARK); panel.setBorder(BorderFactory.createEmptyBorder(15,20,15,20));

        // Generate form
        JPanel form = new JPanel(new FlowLayout(FlowLayout.LEFT,8,0)); form.setOpaque(false); form.setBorder(BorderFactory.createEmptyBorder(0,0,10,0));
        JLabel l1=new JLabel("Emp ID:"); l1.setForeground(ClientMain.FG_SECONDARY); JTextField eidf=ClientMain.styledField(6);
        JLabel l2=new JLabel("Month:"); l2.setForeground(ClientMain.FG_SECONDARY);
        JComboBox<String> monthBox = new JComboBox<>(new String[]{"1-Jan","2-Feb","3-Mar","4-Apr","5-May","6-Jun","7-Jul","8-Aug","9-Sep","10-Oct","11-Nov","12-Dec"});
        monthBox.setBackground(ClientMain.BG_INPUT); monthBox.setForeground(ClientMain.FG_PRIMARY);
        JLabel l3=new JLabel("Year:"); l3.setForeground(ClientMain.FG_SECONDARY); JTextField yf=ClientMain.styledField(5); yf.setText(String.valueOf(Year.now().getValue()));
        JLabel l4=new JLabel("Basic Salary (RM):"); l4.setForeground(ClientMain.FG_SECONDARY); JTextField sf=ClientMain.styledField(8); sf.setText("3500.00");
        JButton genBtn = ClientMain.styledButton("Generate Payroll", ClientMain.ACCENT_GREEN);
        form.add(l1);form.add(eidf);form.add(l2);form.add(monthBox);form.add(l3);form.add(yf);form.add(l4);form.add(sf);form.add(genBtn);
        panel.add(form, BorderLayout.NORTH);

        // View section
        JPanel view = new JPanel(new BorderLayout(8,8)); view.setOpaque(false);
        JPanel viewBar = new JPanel(new FlowLayout(FlowLayout.LEFT,8,0)); viewBar.setOpaque(false);
        JLabel vl1=new JLabel("View Emp ID:"); vl1.setForeground(ClientMain.FG_SECONDARY); JTextField veidf=ClientMain.styledField(6);
        JLabel vl2=new JLabel("Year:"); vl2.setForeground(ClientMain.FG_SECONDARY); JTextField vyf=ClientMain.styledField(5); vyf.setText(String.valueOf(Year.now().getValue()));
        JButton viewBtn = ClientMain.styledButton("View Records", ClientMain.ACCENT_PURPLE);
        viewBar.add(vl1);viewBar.add(veidf);viewBar.add(vl2);viewBar.add(vyf);viewBar.add(viewBtn);
        view.add(viewBar, BorderLayout.NORTH);

        String[] cols={"Month","Basic (RM)","Deductions (RM)","Net (RM)","Generated At"};
        DefaultTableModel model = new DefaultTableModel(cols,0){public boolean isCellEditable(int r,int c){return false;}};
        view.add(wrap(styledTable(model)), BorderLayout.CENTER);
        panel.add(view, BorderLayout.CENTER);

        // Result label
        JLabel resLabel = new JLabel(" "); resLabel.setFont(new Font("Segoe UI",Font.PLAIN,12));
        panel.add(resLabel, BorderLayout.SOUTH);

        String[] months={"","Jan","Feb","Mar","Apr","May","Jun","Jul","Aug","Sep","Oct","Nov","Dec"};

        genBtn.addActionListener(e->{
            try {
                int eid=Integer.parseInt(eidf.getText().trim());
                int month=monthBox.getSelectedIndex()+1;
                int year=Integer.parseInt(yf.getText().trim());
                double salary=Double.parseDouble(sf.getText().trim());
                ClientMain.prsService.generatePayroll(eid,month,year,salary,ClientMain.sessionToken);
                resLabel.setForeground(ClientMain.ACCENT_GREEN);
                resLabel.setText("Payroll generated for emp#"+eid+" "+months[month]+"/"+year);
            } catch(NumberFormatException ex){ClientMain.showError("Enter valid numbers.");}
            catch(Exception ex){resLabel.setForeground(ClientMain.ACCENT_RED);resLabel.setText(ex.getMessage());}
        });

        viewBtn.addActionListener(e->{
            try {
                int eid=Integer.parseInt(veidf.getText().trim());
                int year=Integer.parseInt(vyf.getText().trim());
                List<PayrollRecord> recs=ClientMain.prsService.getYearlyPayroll(eid,year,ClientMain.sessionToken);
                model.setRowCount(0);
                for(PayrollRecord pr:recs) model.addRow(new Object[]{months[pr.getMonth()],String.format("%.2f",pr.getBasicSalary()),String.format("%.2f",pr.getDeductions()),String.format("%.2f",pr.getNetSalary()),pr.getGeneratedAt()});
                if(recs.isEmpty()) resLabel.setText("No payroll records found.");
                else resLabel.setText("Loaded "+recs.size()+" records.");
                resLabel.setForeground(ClientMain.FG_SECONDARY);
            } catch(NumberFormatException ex){ClientMain.showError("Enter valid numbers.");}
            catch(Exception ex){ClientMain.showError("Error: "+ex.getMessage());}
        });
        return panel;
    }

    // ==================== YEARLY REPORT ====================
    private JPanel createReportTab() {
        JPanel panel = new JPanel(new BorderLayout(10,10)); panel.setBackground(ClientMain.BG_DARK); panel.setBorder(BorderFactory.createEmptyBorder(15,20,15,20));
        JPanel ib=new JPanel(new FlowLayout(FlowLayout.LEFT,8,0)); ib.setOpaque(false); ib.setBorder(BorderFactory.createEmptyBorder(0,0,10,0));
        JLabel l1=new JLabel("Employee ID:"); l1.setForeground(ClientMain.FG_SECONDARY); JTextField eidf=ClientMain.styledField(8);
        JLabel l2=new JLabel("Year:"); l2.setForeground(ClientMain.FG_SECONDARY); JTextField yf=ClientMain.styledField(6); yf.setText(String.valueOf(Year.now().getValue()));
        JButton gb=ClientMain.styledButton("Generate Report",ClientMain.ACCENT_PURPLE);
        JButton saveBtn=ClientMain.styledButton("Save to File",ClientMain.ACCENT_GREEN);
        ib.add(l1);ib.add(eidf);ib.add(l2);ib.add(yf);ib.add(gb);ib.add(saveBtn); panel.add(ib,BorderLayout.NORTH);

        JTextArea ra = new JTextArea(); ra.setEditable(false); ra.setBackground(ClientMain.BG_PANEL);
        ra.setForeground(ClientMain.ACCENT_GREEN); ra.setFont(new Font("JetBrains Mono",Font.PLAIN,12));
        ra.setBorder(BorderFactory.createEmptyBorder(15,15,15,15));
        ra.setText(" Enter Employee ID and year, then click 'Generate Report'.");
        JScrollPane sp=new JScrollPane(ra); sp.setBorder(BorderFactory.createLineBorder(ClientMain.BORDER_COLOR,1));
        sp.getViewport().setBackground(ClientMain.BG_PANEL); panel.add(sp,BorderLayout.CENTER);

        gb.addActionListener(e->{try{
            int eid=Integer.parseInt(eidf.getText().trim()); int year=Integer.parseInt(yf.getText().trim());
            YearlyReport rpt=ClientMain.hrmService.generateYearlyReport(eid,year,ClientMain.sessionToken);
            StringBuilder sb=new StringBuilder();
            sb.append("\n═══════════════════════════════════════════════════════\n");
            sb.append("           YEARLY EMPLOYEE REPORT - ").append(year).append("           \n");
            sb.append("═══════════════════════════════════════════════════════\n");
            sb.append("  Generated: ").append(rpt.getGeneratedAt()).append("\n\n");
            Employee emp=rpt.getEmployee();
            sb.append("--- EMPLOYEE PROFILE ").append("-".repeat(25)).append("\n");
            sb.append("| Name       : ").append(pad(emp.getFullName(),31)).append("|\n");
            sb.append("| IC/Passport: ").append(pad(emp.getIcPassport(),31)).append("|\n");
            sb.append("| Email      : ").append(pad(emp.getEmail()!=null?emp.getEmail():"N/A",31)).append("|\n");
            sb.append("| Department : ").append(pad(emp.getDepartment()!=null?emp.getDepartment():"N/A",31)).append("|\n");
            sb.append("| Position   : ").append(pad(emp.getPosition()!=null?emp.getPosition():"N/A",31)).append("|\n");
            sb.append("").append("-".repeat(46)).append("\n\n");
            sb.append("--- FAMILY DETAILS ").append("-".repeat(29)).append("\n");
            if(rpt.getFamilyMembers().isEmpty()) sb.append("| No family members on record.                |\n");
            else for(FamilyMember fm:rpt.getFamilyMembers()) sb.append("| ").append(pad(fm.getName()+" ("+fm.getRelationship()+")",44)).append("|\n");
            sb.append("").append("-".repeat(46)).append("\n\n");
            sb.append("--- LEAVE BALANCE ").append("-".repeat(30)).append("\n");
            for(LeaveBalance lb:rpt.getLeaveBalances()) sb.append("| ").append(pad(String.format("%-12s %d/%d used, %d left",lb.getLeaveType(),lb.getUsedDays(),lb.getTotalDays(),lb.getRemainingDays()),44)).append("|\n");
            sb.append("").append("-".repeat(46)).append("\n\n");
            sb.append("--- LEAVE HISTORY ").append("-".repeat(30)).append("\n");
            if(rpt.getLeaveApplications().isEmpty()) sb.append("| No leave applications for ").append(year).append(".               |\n");
            else{sb.append("| ").append(pad(String.format("%-5s %-9s %-11s %-5s %-8s","ID","Type","Start","Days","Status"),44)).append("|\n");
                for(LeaveApplication la:rpt.getLeaveApplications()) sb.append("| ").append(pad(String.format("%-5d %-9s %-11s %-5d %-8s",la.getLeaveId(),la.getLeaveType(),la.getStartDate(),la.getDaysRequested(),la.getStatus()),44)).append("|\n");}
            sb.append("").append("-".repeat(46)).append("\n");
            ra.setText(sb.toString()); ra.setCaretPosition(0);
        }catch(NumberFormatException ex){ClientMain.showError("Enter valid ID and Year.");}catch(Exception ex){ClientMain.showError("Error: "+ex.getMessage());}});

        saveBtn.addActionListener(e -> {
            String content = ra.getText();
            if (content == null || content.trim().isEmpty() || content.contains("Enter Employee ID")) {
                ClientMain.showError("Generate a report first before saving.");
                return;
            }
            JFileChooser fc = new JFileChooser();
            fc.setDialogTitle("Save Yearly Report");
            fc.setSelectedFile(new File("BHEL_Yearly_Report_" + yf.getText().trim() + ".txt"));
            fc.setFileFilter(new javax.swing.filechooser.FileNameExtensionFilter("Text Files (*.txt)", "txt"));
            if (fc.showSaveDialog(panel) == JFileChooser.APPROVE_OPTION) {
                try {
                    File file = fc.getSelectedFile();
                    if (!file.getName().endsWith(".txt")) file = new File(file.getAbsolutePath() + ".txt");
                    try (PrintWriter pw = new PrintWriter(new FileWriter(file))) {
                        pw.print(content);
                    }
                    ClientMain.showSuccess("Report saved to:\n" + file.getAbsolutePath());
                } catch (IOException ex) {
                    ClientMain.showError("Failed to save: " + ex.getMessage());
                }
            }
        });

        return panel;
    }
    private String pad(String s,int len){if(s==null)s="";return s.length()>=len?s.substring(0,len):s+" ".repeat(len-s.length());}
}
