package client;

import common.models.*;
import javax.swing.*;
import javax.swing.table.DefaultTableModel;
import javax.swing.table.DefaultTableCellRenderer;
import java.awt.*;
import java.util.List;

public class AdminPanel extends JPanel {

    public AdminPanel() {
        setLayout(new BorderLayout()); setBackground(ClientMain.BG_DARK);
        add(ClientMain.createTopBar("Admin Dashboard", ClientMain.ACCENT_PURPLE), BorderLayout.NORTH);
        JTabbedPane tabs = new JTabbedPane();
        tabs.setBackground(ClientMain.BG_PANEL); tabs.setFont(new Font("Segoe UI",Font.BOLD,12));
        tabs.addTab("  User Accounts  ", createUserTab());
        tabs.addTab("  Audit Log  ", createAuditTab());
        add(tabs, BorderLayout.CENTER);
    }

    private JTable styledTable(DefaultTableModel m) {
        JTable t = new JTable(m); t.setBackground(ClientMain.BG_PANEL); t.setForeground(ClientMain.FG_PRIMARY);
        t.setGridColor(ClientMain.BORDER_COLOR); t.setSelectionBackground(ClientMain.TABLE_SEL); t.setRowHeight(32);
        t.setFont(new Font("Segoe UI",Font.PLAIN,12)); t.getTableHeader().setBackground(ClientMain.BG_CARD);
        t.getTableHeader().setForeground(ClientMain.FG_SECONDARY); t.getTableHeader().setFont(new Font("Segoe UI",Font.BOLD,11));
        t.setSelectionMode(ListSelectionModel.SINGLE_SELECTION); t.setShowVerticalLines(false);
        t.setDefaultRenderer(Object.class, new DefaultTableCellRenderer(){
            public Component getTableCellRendererComponent(JTable tb,Object v,boolean s,boolean f,int r,int c){
                Component comp=super.getTableCellRendererComponent(tb,v,s,f,r,c);
                if(!s)comp.setBackground(r%2==0?ClientMain.BG_PANEL:ClientMain.TABLE_ROW_ALT);
                comp.setForeground(ClientMain.FG_PRIMARY);
                String txt=v!=null?v.toString():"";
                if("ADMIN".equals(txt))comp.setForeground(ClientMain.ACCENT_PURPLE);
                else if("HR".equals(txt))comp.setForeground(ClientMain.ACCENT_GREEN);
                else if("EMPLOYEE".equals(txt))comp.setForeground(ClientMain.ACCENT_BLUE);
                else if("Yes".equals(txt))comp.setForeground(ClientMain.ACCENT_GREEN);
                else if("No".equals(txt))comp.setForeground(ClientMain.ACCENT_RED);
                setBorder(BorderFactory.createEmptyBorder(0,8,0,8)); return comp;}});
        return t;
    }
    private JScrollPane wrap(JTable t){JScrollPane sp=new JScrollPane(t);sp.setBorder(BorderFactory.createLineBorder(ClientMain.BORDER_COLOR,1));sp.getViewport().setBackground(ClientMain.BG_PANEL);return sp;}

    private JPanel createUserTab() {
        JPanel panel = new JPanel(new BorderLayout(10,10)); panel.setBackground(ClientMain.BG_DARK); panel.setBorder(BorderFactory.createEmptyBorder(15,20,15,20));
        String[] cols={"User ID","Username","Role","Emp ID","Active"};
        DefaultTableModel model = new DefaultTableModel(cols,0){public boolean isCellEditable(int r,int c){return false;}};
        JTable table = styledTable(model); panel.add(wrap(table),BorderLayout.CENTER);

        JPanel bp = new JPanel(new FlowLayout(FlowLayout.LEFT,8,0)); bp.setOpaque(false); bp.setBorder(BorderFactory.createEmptyBorder(10,0,0,0));
        JButton addBtn=ClientMain.styledButton("+ Add User",ClientMain.ACCENT_GREEN);
        JButton editBtn=ClientMain.styledButton("Edit",ClientMain.ACCENT_BLUE);
        JButton delBtn=ClientMain.styledButton("Deactivate",ClientMain.ACCENT_RED);
        JButton refBtn=ClientMain.subtleButton("Refresh");
        bp.add(addBtn);bp.add(editBtn);bp.add(delBtn);bp.add(refBtn); panel.add(bp,BorderLayout.SOUTH);

        Runnable load = ()->{try{List<UserAccount> us=ClientMain.hrmService.getAllUsers(ClientMain.sessionToken);SwingUtilities.invokeLater(()->{model.setRowCount(0);for(UserAccount u:us)model.addRow(new Object[]{u.getUserId(),u.getUsername(),u.getRole(),u.getEmployeeId(),u.isActive()?"Yes":"No"});});}catch(Exception ex){ClientMain.showError("Error: "+ex.getMessage());}};
        new Thread(load).start(); refBtn.addActionListener(e->new Thread(load).start());

        addBtn.addActionListener(e->{
            try {
                // Get all employees to find the next available ID
                List<Employee> employees = ClientMain.hrmService.getAllEmployees(ClientMain.sessionToken);
                int nextEmpId = 1;
                if (employees != null && !employees.isEmpty()) {
                    int maxId = employees.stream().mapToInt(Employee::getEmployeeId).max().orElse(0);
                    nextEmpId = maxId + 1;
                }
                final int prefillId = nextEmpId;
                
                JPanel dp=new JPanel(new GridBagLayout());dp.setBackground(ClientMain.BG_PANEL);dp.setPreferredSize(new Dimension(380,260));
                GridBagConstraints g=new GridBagConstraints();g.insets=new Insets(3,8,3,8);g.fill=GridBagConstraints.HORIZONTAL;g.gridx=0;g.gridwidth=2;
                JLabel h=new JLabel("Create User Account");h.setFont(new Font("Segoe UI",Font.BOLD,15));h.setForeground(ClientMain.FG_PRIMARY);g.gridy=0;dp.add(h,g);g.gridwidth=1;
                JLabel ul=new JLabel("USERNAME");ul.setFont(new Font("Segoe UI",Font.BOLD,10));ul.setForeground(ClientMain.FG_DIM);g.gridy=1;g.gridx=0;dp.add(ul,g);
                JTextField uf=ClientMain.styledField(16);g.gridy=2;dp.add(uf,g);
                JLabel pl=new JLabel("PASSWORD (min 6)");pl.setFont(new Font("Segoe UI",Font.BOLD,10));pl.setForeground(ClientMain.FG_DIM);g.gridy=1;g.gridx=1;dp.add(pl,g);
                JPasswordField pf=ClientMain.styledPasswordField(16);g.gridy=2;dp.add(pf,g);
                JLabel rl=new JLabel("ROLE");rl.setFont(new Font("Segoe UI",Font.BOLD,10));rl.setForeground(ClientMain.FG_DIM);g.gridy=3;g.gridx=0;dp.add(rl,g);
                JComboBox<String> rb=new JComboBox<>(new String[]{"EMPLOYEE","HR","ADMIN"});rb.setBackground(ClientMain.BG_INPUT);rb.setForeground(ClientMain.FG_PRIMARY);g.gridy=4;dp.add(rb,g);
                JLabel el=new JLabel("EMPLOYEE ID");el.setFont(new Font("Segoe UI",Font.BOLD,10));el.setForeground(ClientMain.FG_DIM);g.gridy=3;g.gridx=1;dp.add(el,g);
                JTextField ef=ClientMain.styledField(8);ef.setText(String.valueOf(prefillId));ef.setEditable(false);ef.setBackground(ClientMain.BG_CARD);ef.setForeground(ClientMain.FG_DIM);g.gridy=4;dp.add(ef,g);
                int res=JOptionPane.showConfirmDialog(this,dp,"Add User",JOptionPane.OK_CANCEL_OPTION,JOptionPane.PLAIN_MESSAGE);
                if(res==JOptionPane.OK_OPTION){
                    String un=uf.getText().trim();String pw=new String(pf.getPassword());
                    if(un.length()<3){ClientMain.showError("Username must be 3+ chars.");return;}
                    if(pw.length()<6){ClientMain.showError("Password must be 6+ chars.");return;}
                    try{int eid=Integer.parseInt(ef.getText().trim());
                        ClientMain.hrmService.addUser(un,pw,(String)rb.getSelectedItem(),eid,ClientMain.sessionToken);
                        ClientMain.showSuccess("User '"+un+"' created!");new Thread(load).start();
                    }catch(NumberFormatException ex){ClientMain.showError("Emp ID must be a number.");}
                    catch(Exception ex){ClientMain.showError("Error: "+ex.getMessage());}
                }
            } catch (Exception ex) { ClientMain.showError("Error fetching employee data: " + ex.getMessage()); }
        });

        editBtn.addActionListener(e->{int row=table.getSelectedRow();if(row<0){ClientMain.showError("Select a user.");return;}
            int uid=(int)model.getValueAt(row,0);String cu=(String)model.getValueAt(row,1);String cr=(String)model.getValueAt(row,2);boolean ca="Yes".equals(model.getValueAt(row,4));
            JTextField uf=ClientMain.styledField(18);uf.setText(cu);JComboBox<String> rb=new JComboBox<>(new String[]{"EMPLOYEE","HR","ADMIN"});rb.setSelectedItem(cr);
            JCheckBox ab=new JCheckBox("Active",ca);ab.setBackground(ClientMain.BG_PANEL);ab.setForeground(ClientMain.FG_PRIMARY);
            int res=JOptionPane.showConfirmDialog(this,new Object[]{"Username:",uf,"Role:",rb,"",ab},"Edit User #"+uid,JOptionPane.OK_CANCEL_OPTION,JOptionPane.PLAIN_MESSAGE);
            if(res==JOptionPane.OK_OPTION){try{ClientMain.hrmService.updateUser(uid,uf.getText().trim(),(String)rb.getSelectedItem(),ab.isSelected(),ClientMain.sessionToken);ClientMain.showSuccess("Updated!");new Thread(load).start();}catch(Exception ex){ClientMain.showError("Error: "+ex.getMessage());}}
        });

        delBtn.addActionListener(e->{int row=table.getSelectedRow();if(row<0){ClientMain.showError("Select a user.");return;}
            int uid=(int)model.getValueAt(row,0);String un=(String)model.getValueAt(row,1);
            if(JOptionPane.showConfirmDialog(this,"Deactivate '"+un+"'?","Confirm",JOptionPane.YES_NO_OPTION)==0)
            {try{ClientMain.hrmService.removeUser(uid,ClientMain.sessionToken);ClientMain.showSuccess("Deactivated.");new Thread(load).start();}catch(Exception ex){ClientMain.showError("Error: "+ex.getMessage());}}
        });
        return panel;
    }

    private JPanel createAuditTab() {
        JPanel panel = new JPanel(new BorderLayout(10,10)); panel.setBackground(ClientMain.BG_DARK); panel.setBorder(BorderFactory.createEmptyBorder(15,20,15,20));
        String[] cols={"ID","Timestamp","User","Role","Action","Details"};
        DefaultTableModel model = new DefaultTableModel(cols,0){public boolean isCellEditable(int r,int c){return false;}};
        JTable table = styledTable(model); table.setAutoResizeMode(JTable.AUTO_RESIZE_LAST_COLUMN);
        table.getColumnModel().getColumn(4).setCellRenderer(new DefaultTableCellRenderer(){
            public Component getTableCellRendererComponent(JTable t,Object v,boolean s,boolean f,int r,int c){
                Component comp=super.getTableCellRendererComponent(t,v,s,f,r,c);
                comp.setBackground(s?ClientMain.TABLE_SEL:(r%2==0?ClientMain.BG_PANEL:ClientMain.TABLE_ROW_ALT));
                String a=v!=null?v.toString():"";
                if(a.contains("LOGIN"))comp.setForeground(ClientMain.ACCENT_BLUE);
                else if(a.contains("APPROVE"))comp.setForeground(ClientMain.ACCENT_GREEN);
                else if(a.contains("REJECT")||a.contains("DEACTIVATE")||a.contains("FAILED"))comp.setForeground(ClientMain.ACCENT_RED);
                else if(a.contains("REGISTER")||a.contains("ADD"))comp.setForeground(ClientMain.ACCENT_ORANGE);
                else comp.setForeground(ClientMain.FG_PRIMARY);
                setFont(new Font("Segoe UI",Font.BOLD,11));setBorder(BorderFactory.createEmptyBorder(0,8,0,8));return comp;}});
        panel.add(wrap(table),BorderLayout.CENTER);
        JPanel bp=new JPanel(new FlowLayout(FlowLayout.LEFT,8,0));bp.setOpaque(false);bp.setBorder(BorderFactory.createEmptyBorder(10,0,0,0));
        JButton rfb=ClientMain.styledButton("Refresh",ClientMain.ACCENT_BLUE);bp.add(rfb);panel.add(bp,BorderLayout.SOUTH);
        Runnable load=()->{try{List<AuditLogEntry> es=ClientMain.hrmService.getAuditLog(ClientMain.sessionToken);SwingUtilities.invokeLater(()->{model.setRowCount(0);for(int i=es.size()-1;i>=0;i--){AuditLogEntry en=es.get(i);model.addRow(new Object[]{en.getLogId(),en.getTimestamp(),en.getUsername(),en.getRole(),en.getAction(),en.getDetails()});}});}catch(Exception ex){ClientMain.showError("Error: "+ex.getMessage());}};
        new Thread(load).start(); rfb.addActionListener(e->new Thread(load).start());
        return panel;
    }
}
