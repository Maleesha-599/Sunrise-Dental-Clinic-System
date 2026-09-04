/*
 * Click nbfs://nbhost/SystemFileSystem/Templates/Licenses/license-default.txt to change this license
 * Click nbfs://nbhost/SystemFileSystem/Templates/GUIForms/JInternalFrame.java to edit this template
 */
package GUI.Receptionist;

import Model.MySQL;
import com.itextpdf.text.Element;
import com.itextpdf.text.Paragraph;
import com.itextpdf.text.pdf.PdfPTable;
import java.awt.Desktop;
import java.io.File;
import java.io.FileOutputStream;
import java.sql.ResultSet;
import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;
import javax.swing.JOptionPane;
import javax.swing.table.DefaultTableModel;

/**
 *
 * @author Lenovo
 */
public class Payments extends javax.swing.JInternalFrame {

    private int appointmentId;

    java.awt.Frame Parent;

    public Payments(java.awt.Frame parent) {
        initComponents();

        setClosable(false);
        setIconifiable(false);
        setMaximizable(false);
        setResizable(false);
        setBorder(null);
        setFrameIcon(null);

        jButton5.setEnabled(false);
        jButton6.setEnabled(false);

        Parent = parent;
    }

    private int getPaymentStatusId(String status) throws Exception {

        String sql = "SELECT payment_status_id "
                + "FROM payment_status "
                + "WHERE payment_status = '" + status + "'";

        ResultSet rs = MySQL.executeSearch(sql);

        if (rs.next()) {
            return rs.getInt("payment_status_id");
        }

        return -1;
    }

    private int getAppointmentStatusId(String status) throws Exception {

        String sql = "SELECT appointment_status_id "
                + "FROM appointment_status "
                + "WHERE appointment_status = '" + status + "'";

        ResultSet rs = MySQL.executeSearch(sql);

        if (rs.next()) {
            return rs.getInt("appointment_status_id");
        }

        return -1;
    }

    private boolean paymentAlreadyExists(int appointmentId) throws Exception {

        String sql = "SELECT payment_id "
                + "FROM payment "
                + "WHERE Appointment_appointment_id = " + appointmentId;

        ResultSet rs = MySQL.executeSearch(sql);

        return rs.next();
    }

    private String getPaymentStatus(int appointmentId) throws Exception {

        String sql = "SELECT payment_status.payment_status "
                + "FROM payment "
                + "INNER JOIN payment_status "
                + "ON payment.Payment_status_payment_status_id = payment_status.payment_status_id "
                + "WHERE payment.Appointment_appointment_id = " + appointmentId
                + " ORDER BY payment.payment_id DESC LIMIT 1";

        ResultSet rs = MySQL.executeSearch(sql);

        if (rs.next()) {
            return rs.getString("payment_status");
        }

        return "Not Paid";
    }

    private void loadTreatmentDetails(int appointmentId) {

        try {

            ResultSet resultSet = MySQL.executeSearch(
                    "SELECT treatment.treatment_name, "
                    + "treatment.treatment_cost, "
                    + "treatment.consultation_fee "
                    + "FROM appointment_has_treatment "
                    + "INNER JOIN treatment "
                    + "ON appointment_has_treatment.Treatment_treatment_id "
                    + "= treatment.treatment_id "
                    + "WHERE appointment_has_treatment.Appointment_appointment_id = '"
                    + appointmentId + "';"
            );

            DefaultTableModel model
                    = (DefaultTableModel) jTable1.getModel();

            model.setRowCount(0);

            double treatmentCost = 0.00;
            double consultationFee = 0.00;

            boolean treatmentFound = false;

            while (resultSet.next()) {

                treatmentFound = true;

                String treatmentName
                        = resultSet.getString("treatment_name");

                double cost
                        = resultSet.getDouble("treatment_cost");

                double fee
                        = resultSet.getDouble("consultation_fee");

                treatmentCost += cost;

                consultationFee += fee;

                double rowTotal = cost + fee;

                model.addRow(new Object[]{
                    treatmentName,
                    String.format("%.2f", cost),
                    String.format("%.2f", fee),
                    String.format("%.2f", rowTotal)
                });
            }

            if (!treatmentFound) {

                JOptionPane.showMessageDialog(
                        this,
                        "No treatments found for this appointment.",
                        "Treatment Not Found",
                        JOptionPane.WARNING_MESSAGE
                );

                jTextField9.setText("0.00");
                jTextField10.setText("0.00");
                jTextField11.setText("0.00");

                return;
            }

            double totalAmount
                    = treatmentCost + consultationFee;

            jTextField9.setText(
                    String.format("%.2f", consultationFee)
            );

            jTextField10.setText(
                    String.format("%.2f", treatmentCost)
            );

            jTextField11.setText(
                    String.format("%.2f", totalAmount)
            );

        } catch (Exception e) {

            e.printStackTrace();

            JOptionPane.showMessageDialog(
                    this,
                    "Error loading treatment details.",
                    "Error",
                    JOptionPane.ERROR_MESSAGE
            );
        }
    }

    private boolean loadAppointmentDetails() {

        String appointmentNumber = jTextField1.getText().trim();

        if (appointmentNumber.isEmpty()) {

            JOptionPane.showMessageDialog(
                    this,
                    "Please enter an appointment number.",
                    "Warning",
                    JOptionPane.WARNING_MESSAGE
            );

            return false;
        }

        try {

            ResultSet resultSet = MySQL.executeSearch("SELECT appointment.appointment_id, patient.patient_nic, "
                    + "patient.patient_name, patient.contact_no, dentist.dentist_name, "
                    + "DATE(appointment.appointment_date_and_time) AS appointment_date, "
                    + "TIME(appointment_date_and_time) AS appointment_time FROM `patient` "
                    + "INNER JOIN `appointment` ON appointment.Patient_patient_nic = patient.patient_nic "
                    + "INNER JOIN `dentist` ON appointment.Dentist_dentist_id = dentist.dentist_id "
                    + "WHERE appointment.appointment_number = '" + appointmentNumber + "'");

            if (resultSet.next()) {

                appointmentId = resultSet.getInt("appointment_id");

                String patientNIC = resultSet.getString("patient_nic");
                String patientName = resultSet.getString("patient_name");
                String contactNo = resultSet.getString("contact_no");
                String dentistName = resultSet.getString("dentist_name");
                String appointmentDate = resultSet.getString("appointment_date");
                String appointmentTime = resultSet.getString("appointment_time");

                jTextField4.setText(patientNIC);
                jTextField5.setText(patientName);
                jTextField2.setText(contactNo);
                jTextField3.setText(dentistName);
                jTextField6.setText(appointmentDate);
                jTextField8.setText(appointmentTime);

                loadTreatmentDetails(appointmentId);

                String paymentStatus = getPaymentStatus(appointmentId);

                if (paymentStatus.equalsIgnoreCase("Paid")) {

                    jButton5.setEnabled(false);
                    jButton6.setEnabled(true);

                } else {

                    jButton5.setEnabled(true);
                    jButton6.setEnabled(false);
                }

                jButton4.setEnabled(true);

                return true;

            } else {

                JOptionPane.showMessageDialog(this, "This appointment number not found.", "Warning", JOptionPane.WARNING_MESSAGE);

                jTextField6.setText("");
                jTextField8.setText("");
                jTextField4.setText("");
                jTextField5.setText("");
                jTextField2.setText("");
                jTextField3.setText("");

                jButton4.setEnabled(false);
                jButton5.setEnabled(false);
                jButton6.setEnabled(false);

                jTextField1.requestFocus();

                return false;

            }

        } catch (Exception e) {
            e.printStackTrace();

            JOptionPane.showMessageDialog(
                    this,
                    "Error loading appointment details.",
                    "Error",
                    JOptionPane.ERROR_MESSAGE
            );

            jButton5.setEnabled(false);
            jButton6.setEnabled(false);

            return false;
        }
    }

    private void printPaymentInvoice(int paymentId) {

        try {

            String filePath
                    = "Sunrise_Dental_Payment_Invoice.pdf";

            com.itextpdf.text.Document document
                    = new com.itextpdf.text.Document();

            com.itextpdf.text.pdf.PdfWriter.getInstance(
                    document,
                    new FileOutputStream(filePath)
            );

            document.open();

            Paragraph clinicName
                    = new Paragraph(
                            "SUNRISE DENTAL CLINIC"
                    );

            clinicName.setAlignment(
                    Element.ALIGN_CENTER
            );

            document.add(clinicName);

            Paragraph title
                    = new Paragraph(
                            "PAYMENT INVOICE"
                    );

            title.setAlignment(
                    Element.ALIGN_CENTER
            );

            document.add(title);

            document.add(
                    new Paragraph(" ")
            );

            String sql
                    = "SELECT "
                    + "payment.payment_id, "
                    + "payment.treatment_cost, "
                    + "payment.consultation_fee, "
                    + "payment.total_amount, "
                    + "payment.payment_date_and_time, "
                    + "payment_status.payment_status, "
                    + "appointment.appointment_number, "
                    + "patient.patient_nic "
                    + "FROM payment "
                    + "INNER JOIN payment_status "
                    + "ON payment.Payment_status_payment_status_id "
                    + "= payment_status.payment_status_id "
                    + "INNER JOIN appointment "
                    + "ON payment.Appointment_appointment_id "
                    + "= appointment.appointment_id "
                    + "INNER JOIN patient "
                    + "ON appointment.Patient_patient_nic "
                    + "= patient.patient_nic "
                    + "WHERE payment.payment_id = "
                    + paymentId;

            ResultSet rs
                    = MySQL.executeSearch(sql);

            if (rs.next()) {

                PdfPTable table
                        = new PdfPTable(2);

                table.setWidthPercentage(100);

                table.addCell("Payment ID");
                table.addCell(
                        rs.getString("payment_id")
                );

                table.addCell("Appointment Number");
                table.addCell(
                        rs.getString("appointment_number")
                );

                table.addCell("Patient NIC");
                table.addCell(
                        rs.getString("patient_nic")
                );

                table.addCell("Treatment Cost");
                table.addCell(
                        "Rs. "
                        + rs.getString("treatment_cost")
                );

                table.addCell("Consultation Fee");
                table.addCell(
                        "Rs. "
                        + rs.getString("consultation_fee")
                );

                table.addCell("Total Amount");
                table.addCell(
                        "Rs. "
                        + rs.getString("total_amount")
                );

                table.addCell("Payment Date & Time");
                table.addCell(
                        rs.getString(
                                "payment_date_and_time"
                        )
                );

                table.addCell("Payment Status");
                table.addCell(
                        rs.getString(
                                "payment_status"
                        )
                );

                document.add(table);

                document.add(
                        new Paragraph(" ")
                );

                Paragraph thankYou
                        = new Paragraph(
                                "Thank you for choosing "
                                + "Sunrise Dental Clinic."
                        );

                thankYou.setAlignment(
                        Element.ALIGN_CENTER
                );

                document.add(thankYou);

            } else {

                document.add(
                        new Paragraph(
                                "Payment details not found."
                        )
                );
            }

            document.close();

            File pdfFile
                    = new File(filePath);

            if (Desktop.isDesktopSupported()) {

                Desktop.getDesktop().open(
                        pdfFile
                );
            }

        } catch (Exception e) {

            e.printStackTrace();

            JOptionPane.showMessageDialog(
                    this,
                    "Error generating invoice.\n"
                    + e.getMessage(),
                    "Invoice Error",
                    JOptionPane.ERROR_MESSAGE
            );
        }
    }

    @SuppressWarnings("unchecked")
    // <editor-fold defaultstate="collapsed" desc="Generated Code">//GEN-BEGIN:initComponents
    private void initComponents() {

        jPanel1 = new javax.swing.JPanel();
        jLabel11 = new javax.swing.JLabel();
        jPanel2 = new javax.swing.JPanel();
        jLabel1 = new javax.swing.JLabel();
        jTextField1 = new javax.swing.JTextField();
        jLabel2 = new javax.swing.JLabel();
        jTextField2 = new javax.swing.JTextField();
        jLabel3 = new javax.swing.JLabel();
        jTextField3 = new javax.swing.JTextField();
        jButton4 = new javax.swing.JButton();
        jLabel4 = new javax.swing.JLabel();
        jTextField4 = new javax.swing.JTextField();
        jLabel17 = new javax.swing.JLabel();
        jTextField5 = new javax.swing.JTextField();
        jLabel19 = new javax.swing.JLabel();
        jTextField6 = new javax.swing.JTextField();
        jLabel20 = new javax.swing.JLabel();
        jTextField8 = new javax.swing.JTextField();
        jPanel3 = new javax.swing.JPanel();
        jScrollPane1 = new javax.swing.JScrollPane();
        jTable1 = new javax.swing.JTable();
        jLabel5 = new javax.swing.JLabel();
        jTextField9 = new javax.swing.JTextField();
        jLabel21 = new javax.swing.JLabel();
        jTextField10 = new javax.swing.JTextField();
        jLabel6 = new javax.swing.JLabel();
        jTextField11 = new javax.swing.JTextField();
        jLabel7 = new javax.swing.JLabel();
        jTextField12 = new javax.swing.JTextField();
        jLabel22 = new javax.swing.JLabel();
        jTextField13 = new javax.swing.JTextField();
        jButton5 = new javax.swing.JButton();
        jButton6 = new javax.swing.JButton();

        jLabel11.setFont(new java.awt.Font("Segoe UI Black", 1, 24)); // NOI18N
        jLabel11.setForeground(new java.awt.Color(0, 0, 102));
        jLabel11.setText("Payments");

        jPanel2.setBorder(javax.swing.BorderFactory.createTitledBorder(null, "Appointment Details", javax.swing.border.TitledBorder.DEFAULT_JUSTIFICATION, javax.swing.border.TitledBorder.DEFAULT_POSITION, new java.awt.Font("Segoe UI Variable", 1, 14), new java.awt.Color(5, 125, 165))); // NOI18N

        jLabel1.setText("Appointment Number");

        jTextField1.setCursor(new java.awt.Cursor(java.awt.Cursor.TEXT_CURSOR));
        jTextField1.addActionListener(new java.awt.event.ActionListener() {
            public void actionPerformed(java.awt.event.ActionEvent evt) {
                jTextField1ActionPerformed(evt);
            }
        });
        jTextField1.addKeyListener(new java.awt.event.KeyAdapter() {
            public void keyPressed(java.awt.event.KeyEvent evt) {
                jTextField1KeyPressed(evt);
            }
        });

        jLabel2.setText("Contact Number");

        jTextField2.setFocusable(false);

        jLabel3.setText("Dentist");

        jTextField3.setFocusable(false);

        jButton4.setFont(new java.awt.Font("Segoe UI Black", 1, 18)); // NOI18N
        jButton4.setText("Cancel");
        jButton4.addActionListener(new java.awt.event.ActionListener() {
            public void actionPerformed(java.awt.event.ActionEvent evt) {
                jButton4ActionPerformed(evt);
            }
        });

        jLabel4.setText("Patient NIC");

        jTextField4.setFocusable(false);

        jLabel17.setText("Patient Name");

        jTextField5.setFocusable(false);

        jLabel19.setText("Appointment Date");

        jTextField6.setFocusable(false);

        jLabel20.setText("Appointment Time");

        jTextField8.setFocusable(false);

        javax.swing.GroupLayout jPanel2Layout = new javax.swing.GroupLayout(jPanel2);
        jPanel2.setLayout(jPanel2Layout);
        jPanel2Layout.setHorizontalGroup(
            jPanel2Layout.createParallelGroup(javax.swing.GroupLayout.Alignment.LEADING)
            .addGroup(jPanel2Layout.createSequentialGroup()
                .addContainerGap()
                .addGroup(jPanel2Layout.createParallelGroup(javax.swing.GroupLayout.Alignment.LEADING)
                    .addGroup(jPanel2Layout.createSequentialGroup()
                        .addComponent(jLabel1, javax.swing.GroupLayout.DEFAULT_SIZE, javax.swing.GroupLayout.DEFAULT_SIZE, Short.MAX_VALUE)
                        .addGap(18, 18, 18)
                        .addComponent(jTextField1, javax.swing.GroupLayout.PREFERRED_SIZE, 183, javax.swing.GroupLayout.PREFERRED_SIZE))
                    .addGroup(jPanel2Layout.createSequentialGroup()
                        .addComponent(jLabel3, javax.swing.GroupLayout.DEFAULT_SIZE, javax.swing.GroupLayout.DEFAULT_SIZE, Short.MAX_VALUE)
                        .addGap(18, 18, 18)
                        .addComponent(jTextField3, javax.swing.GroupLayout.PREFERRED_SIZE, 183, javax.swing.GroupLayout.PREFERRED_SIZE))
                    .addComponent(jButton4, javax.swing.GroupLayout.DEFAULT_SIZE, javax.swing.GroupLayout.DEFAULT_SIZE, Short.MAX_VALUE)
                    .addGroup(javax.swing.GroupLayout.Alignment.TRAILING, jPanel2Layout.createSequentialGroup()
                        .addGroup(jPanel2Layout.createParallelGroup(javax.swing.GroupLayout.Alignment.LEADING)
                            .addGroup(jPanel2Layout.createSequentialGroup()
                                .addComponent(jLabel2, javax.swing.GroupLayout.DEFAULT_SIZE, javax.swing.GroupLayout.DEFAULT_SIZE, Short.MAX_VALUE)
                                .addGap(18, 18, 18))
                            .addGroup(jPanel2Layout.createSequentialGroup()
                                .addComponent(jLabel4, javax.swing.GroupLayout.DEFAULT_SIZE, javax.swing.GroupLayout.DEFAULT_SIZE, Short.MAX_VALUE)
                                .addGap(37, 37, 37))
                            .addGroup(jPanel2Layout.createSequentialGroup()
                                .addComponent(jLabel17, javax.swing.GroupLayout.DEFAULT_SIZE, javax.swing.GroupLayout.DEFAULT_SIZE, Short.MAX_VALUE)
                                .addGap(37, 37, 37)))
                        .addGroup(jPanel2Layout.createParallelGroup(javax.swing.GroupLayout.Alignment.LEADING)
                            .addComponent(jTextField2, javax.swing.GroupLayout.PREFERRED_SIZE, 183, javax.swing.GroupLayout.PREFERRED_SIZE)
                            .addComponent(jTextField4, javax.swing.GroupLayout.Alignment.TRAILING, javax.swing.GroupLayout.PREFERRED_SIZE, 183, javax.swing.GroupLayout.PREFERRED_SIZE)
                            .addComponent(jTextField5, javax.swing.GroupLayout.Alignment.TRAILING, javax.swing.GroupLayout.PREFERRED_SIZE, 183, javax.swing.GroupLayout.PREFERRED_SIZE)))
                    .addGroup(jPanel2Layout.createSequentialGroup()
                        .addComponent(jLabel19, javax.swing.GroupLayout.DEFAULT_SIZE, javax.swing.GroupLayout.DEFAULT_SIZE, Short.MAX_VALUE)
                        .addGap(18, 18, 18)
                        .addComponent(jTextField6, javax.swing.GroupLayout.PREFERRED_SIZE, 183, javax.swing.GroupLayout.PREFERRED_SIZE))
                    .addGroup(jPanel2Layout.createSequentialGroup()
                        .addComponent(jLabel20, javax.swing.GroupLayout.DEFAULT_SIZE, javax.swing.GroupLayout.DEFAULT_SIZE, Short.MAX_VALUE)
                        .addGap(20, 20, 20)
                        .addComponent(jTextField8, javax.swing.GroupLayout.PREFERRED_SIZE, 183, javax.swing.GroupLayout.PREFERRED_SIZE)))
                .addContainerGap())
        );
        jPanel2Layout.setVerticalGroup(
            jPanel2Layout.createParallelGroup(javax.swing.GroupLayout.Alignment.LEADING)
            .addGroup(jPanel2Layout.createSequentialGroup()
                .addGap(23, 23, 23)
                .addGroup(jPanel2Layout.createParallelGroup(javax.swing.GroupLayout.Alignment.BASELINE)
                    .addComponent(jLabel1, javax.swing.GroupLayout.PREFERRED_SIZE, 22, javax.swing.GroupLayout.PREFERRED_SIZE)
                    .addComponent(jTextField1, javax.swing.GroupLayout.PREFERRED_SIZE, javax.swing.GroupLayout.DEFAULT_SIZE, javax.swing.GroupLayout.PREFERRED_SIZE))
                .addGap(57, 57, 57)
                .addGroup(jPanel2Layout.createParallelGroup(javax.swing.GroupLayout.Alignment.BASELINE)
                    .addComponent(jLabel4, javax.swing.GroupLayout.PREFERRED_SIZE, 22, javax.swing.GroupLayout.PREFERRED_SIZE)
                    .addComponent(jTextField4, javax.swing.GroupLayout.PREFERRED_SIZE, javax.swing.GroupLayout.DEFAULT_SIZE, javax.swing.GroupLayout.PREFERRED_SIZE))
                .addGap(18, 18, 18)
                .addGroup(jPanel2Layout.createParallelGroup(javax.swing.GroupLayout.Alignment.BASELINE)
                    .addComponent(jLabel17, javax.swing.GroupLayout.PREFERRED_SIZE, 22, javax.swing.GroupLayout.PREFERRED_SIZE)
                    .addComponent(jTextField5, javax.swing.GroupLayout.PREFERRED_SIZE, javax.swing.GroupLayout.DEFAULT_SIZE, javax.swing.GroupLayout.PREFERRED_SIZE))
                .addGap(18, 18, 18)
                .addGroup(jPanel2Layout.createParallelGroup(javax.swing.GroupLayout.Alignment.BASELINE)
                    .addComponent(jLabel2, javax.swing.GroupLayout.PREFERRED_SIZE, 22, javax.swing.GroupLayout.PREFERRED_SIZE)
                    .addComponent(jTextField2, javax.swing.GroupLayout.PREFERRED_SIZE, javax.swing.GroupLayout.DEFAULT_SIZE, javax.swing.GroupLayout.PREFERRED_SIZE))
                .addGap(20, 20, 20)
                .addGroup(jPanel2Layout.createParallelGroup(javax.swing.GroupLayout.Alignment.BASELINE)
                    .addComponent(jLabel3, javax.swing.GroupLayout.PREFERRED_SIZE, 22, javax.swing.GroupLayout.PREFERRED_SIZE)
                    .addComponent(jTextField3, javax.swing.GroupLayout.PREFERRED_SIZE, javax.swing.GroupLayout.DEFAULT_SIZE, javax.swing.GroupLayout.PREFERRED_SIZE))
                .addGap(20, 20, 20)
                .addGroup(jPanel2Layout.createParallelGroup(javax.swing.GroupLayout.Alignment.BASELINE)
                    .addComponent(jLabel19, javax.swing.GroupLayout.PREFERRED_SIZE, 22, javax.swing.GroupLayout.PREFERRED_SIZE)
                    .addComponent(jTextField6, javax.swing.GroupLayout.PREFERRED_SIZE, javax.swing.GroupLayout.DEFAULT_SIZE, javax.swing.GroupLayout.PREFERRED_SIZE))
                .addGap(18, 18, 18)
                .addGroup(jPanel2Layout.createParallelGroup(javax.swing.GroupLayout.Alignment.BASELINE)
                    .addComponent(jLabel20, javax.swing.GroupLayout.PREFERRED_SIZE, 22, javax.swing.GroupLayout.PREFERRED_SIZE)
                    .addComponent(jTextField8, javax.swing.GroupLayout.PREFERRED_SIZE, javax.swing.GroupLayout.DEFAULT_SIZE, javax.swing.GroupLayout.PREFERRED_SIZE))
                .addPreferredGap(javax.swing.LayoutStyle.ComponentPlacement.RELATED, 156, Short.MAX_VALUE)
                .addComponent(jButton4, javax.swing.GroupLayout.PREFERRED_SIZE, 46, javax.swing.GroupLayout.PREFERRED_SIZE)
                .addGap(19, 19, 19))
        );

        jPanel3.setBorder(javax.swing.BorderFactory.createTitledBorder(null, "Treatment details", javax.swing.border.TitledBorder.DEFAULT_JUSTIFICATION, javax.swing.border.TitledBorder.DEFAULT_POSITION, new java.awt.Font("Segoe UI Variable", 1, 14), new java.awt.Color(5, 125, 165))); // NOI18N

        jTable1.setModel(new javax.swing.table.DefaultTableModel(
            new Object [][] {

            },
            new String [] {
                "Treatment", "Treatment cost", "Consultation fee"
            }
        ) {
            boolean[] canEdit = new boolean [] {
                false, false, false
            };

            public boolean isCellEditable(int rowIndex, int columnIndex) {
                return canEdit [columnIndex];
            }
        });
        jTable1.addMouseListener(new java.awt.event.MouseAdapter() {
            public void mouseClicked(java.awt.event.MouseEvent evt) {
                jTable1MouseClicked(evt);
            }
            public void mousePressed(java.awt.event.MouseEvent evt) {
                jTable1MousePressed(evt);
            }
            public void mouseReleased(java.awt.event.MouseEvent evt) {
                jTable1MouseReleased(evt);
            }
        });
        jScrollPane1.setViewportView(jTable1);

        jLabel5.setText("Consultation Fee");

        jTextField9.setFocusable(false);

        jLabel21.setText("Treatment Cost");

        jTextField10.setFocusable(false);

        jLabel6.setFont(new java.awt.Font("Segoe UI", 1, 12)); // NOI18N
        jLabel6.setText("Total Amount");

        jTextField11.setFocusable(false);

        jLabel7.setText("Balance");

        jTextField12.setFocusable(false);

        jLabel22.setText("Amount Received");

        jTextField13.addKeyListener(new java.awt.event.KeyAdapter() {
            public void keyPressed(java.awt.event.KeyEvent evt) {
                jTextField13KeyPressed(evt);
            }
        });

        jButton5.setBackground(new java.awt.Color(5, 125, 165));
        jButton5.setFont(new java.awt.Font("Segoe UI Black", 1, 18)); // NOI18N
        jButton5.setForeground(new java.awt.Color(255, 255, 255));
        jButton5.setText("Payment");
        jButton5.addActionListener(new java.awt.event.ActionListener() {
            public void actionPerformed(java.awt.event.ActionEvent evt) {
                jButton5ActionPerformed(evt);
            }
        });

        jButton6.setBackground(new java.awt.Color(204, 255, 204));
        jButton6.setFont(new java.awt.Font("Segoe UI Black", 1, 18)); // NOI18N
        jButton6.setText("Print Receipt");
        jButton6.addActionListener(new java.awt.event.ActionListener() {
            public void actionPerformed(java.awt.event.ActionEvent evt) {
                jButton6ActionPerformed(evt);
            }
        });

        javax.swing.GroupLayout jPanel3Layout = new javax.swing.GroupLayout(jPanel3);
        jPanel3.setLayout(jPanel3Layout);
        jPanel3Layout.setHorizontalGroup(
            jPanel3Layout.createParallelGroup(javax.swing.GroupLayout.Alignment.LEADING)
            .addGroup(javax.swing.GroupLayout.Alignment.TRAILING, jPanel3Layout.createSequentialGroup()
                .addContainerGap(javax.swing.GroupLayout.DEFAULT_SIZE, Short.MAX_VALUE)
                .addGroup(jPanel3Layout.createParallelGroup(javax.swing.GroupLayout.Alignment.LEADING)
                    .addComponent(jButton5, javax.swing.GroupLayout.PREFERRED_SIZE, 199, javax.swing.GroupLayout.PREFERRED_SIZE)
                    .addComponent(jButton6, javax.swing.GroupLayout.PREFERRED_SIZE, 199, javax.swing.GroupLayout.PREFERRED_SIZE))
                .addGap(138, 138, 138))
            .addGroup(jPanel3Layout.createSequentialGroup()
                .addContainerGap()
                .addGroup(jPanel3Layout.createParallelGroup(javax.swing.GroupLayout.Alignment.LEADING)
                    .addGroup(jPanel3Layout.createSequentialGroup()
                        .addComponent(jScrollPane1)
                        .addContainerGap())
                    .addGroup(jPanel3Layout.createSequentialGroup()
                        .addGroup(jPanel3Layout.createParallelGroup(javax.swing.GroupLayout.Alignment.LEADING, false)
                            .addComponent(jLabel22, javax.swing.GroupLayout.Alignment.TRAILING, javax.swing.GroupLayout.DEFAULT_SIZE, 104, Short.MAX_VALUE)
                            .addComponent(jLabel6, javax.swing.GroupLayout.Alignment.TRAILING, javax.swing.GroupLayout.DEFAULT_SIZE, javax.swing.GroupLayout.DEFAULT_SIZE, Short.MAX_VALUE)
                            .addComponent(jLabel21, javax.swing.GroupLayout.Alignment.TRAILING, javax.swing.GroupLayout.DEFAULT_SIZE, javax.swing.GroupLayout.DEFAULT_SIZE, Short.MAX_VALUE)
                            .addComponent(jLabel5, javax.swing.GroupLayout.Alignment.TRAILING, javax.swing.GroupLayout.DEFAULT_SIZE, javax.swing.GroupLayout.DEFAULT_SIZE, Short.MAX_VALUE)
                            .addComponent(jLabel7, javax.swing.GroupLayout.DEFAULT_SIZE, javax.swing.GroupLayout.DEFAULT_SIZE, Short.MAX_VALUE))
                        .addPreferredGap(javax.swing.LayoutStyle.ComponentPlacement.UNRELATED)
                        .addGroup(jPanel3Layout.createParallelGroup(javax.swing.GroupLayout.Alignment.LEADING)
                            .addComponent(jTextField12, javax.swing.GroupLayout.PREFERRED_SIZE, 294, javax.swing.GroupLayout.PREFERRED_SIZE)
                            .addComponent(jTextField13, javax.swing.GroupLayout.Alignment.TRAILING, javax.swing.GroupLayout.PREFERRED_SIZE, 294, javax.swing.GroupLayout.PREFERRED_SIZE)
                            .addComponent(jTextField11, javax.swing.GroupLayout.PREFERRED_SIZE, 294, javax.swing.GroupLayout.PREFERRED_SIZE)
                            .addComponent(jTextField9, javax.swing.GroupLayout.Alignment.TRAILING, javax.swing.GroupLayout.PREFERRED_SIZE, 294, javax.swing.GroupLayout.PREFERRED_SIZE)
                            .addComponent(jTextField10, javax.swing.GroupLayout.Alignment.TRAILING, javax.swing.GroupLayout.PREFERRED_SIZE, 294, javax.swing.GroupLayout.PREFERRED_SIZE))
                        .addGap(92, 92, 92))))
        );
        jPanel3Layout.setVerticalGroup(
            jPanel3Layout.createParallelGroup(javax.swing.GroupLayout.Alignment.LEADING)
            .addGroup(javax.swing.GroupLayout.Alignment.TRAILING, jPanel3Layout.createSequentialGroup()
                .addGap(18, 18, 18)
                .addComponent(jScrollPane1, javax.swing.GroupLayout.PREFERRED_SIZE, 0, Short.MAX_VALUE)
                .addGap(30, 30, 30)
                .addGroup(jPanel3Layout.createParallelGroup(javax.swing.GroupLayout.Alignment.BASELINE)
                    .addComponent(jLabel5, javax.swing.GroupLayout.PREFERRED_SIZE, 22, javax.swing.GroupLayout.PREFERRED_SIZE)
                    .addComponent(jTextField9, javax.swing.GroupLayout.PREFERRED_SIZE, javax.swing.GroupLayout.DEFAULT_SIZE, javax.swing.GroupLayout.PREFERRED_SIZE))
                .addGap(18, 18, 18)
                .addGroup(jPanel3Layout.createParallelGroup(javax.swing.GroupLayout.Alignment.BASELINE)
                    .addComponent(jLabel21, javax.swing.GroupLayout.PREFERRED_SIZE, 22, javax.swing.GroupLayout.PREFERRED_SIZE)
                    .addComponent(jTextField10, javax.swing.GroupLayout.PREFERRED_SIZE, javax.swing.GroupLayout.DEFAULT_SIZE, javax.swing.GroupLayout.PREFERRED_SIZE))
                .addGap(18, 18, 18)
                .addGroup(jPanel3Layout.createParallelGroup(javax.swing.GroupLayout.Alignment.BASELINE)
                    .addComponent(jLabel6, javax.swing.GroupLayout.PREFERRED_SIZE, 22, javax.swing.GroupLayout.PREFERRED_SIZE)
                    .addComponent(jTextField11, javax.swing.GroupLayout.PREFERRED_SIZE, javax.swing.GroupLayout.DEFAULT_SIZE, javax.swing.GroupLayout.PREFERRED_SIZE))
                .addGap(18, 18, 18)
                .addGroup(jPanel3Layout.createParallelGroup(javax.swing.GroupLayout.Alignment.BASELINE)
                    .addComponent(jLabel22, javax.swing.GroupLayout.PREFERRED_SIZE, 22, javax.swing.GroupLayout.PREFERRED_SIZE)
                    .addComponent(jTextField13, javax.swing.GroupLayout.PREFERRED_SIZE, javax.swing.GroupLayout.DEFAULT_SIZE, javax.swing.GroupLayout.PREFERRED_SIZE))
                .addGap(18, 18, 18)
                .addGroup(jPanel3Layout.createParallelGroup(javax.swing.GroupLayout.Alignment.BASELINE)
                    .addComponent(jLabel7, javax.swing.GroupLayout.PREFERRED_SIZE, 22, javax.swing.GroupLayout.PREFERRED_SIZE)
                    .addComponent(jTextField12, javax.swing.GroupLayout.PREFERRED_SIZE, javax.swing.GroupLayout.DEFAULT_SIZE, javax.swing.GroupLayout.PREFERRED_SIZE))
                .addGap(40, 40, 40)
                .addComponent(jButton5)
                .addGap(18, 18, 18)
                .addComponent(jButton6)
                .addGap(30, 30, 30))
        );

        javax.swing.GroupLayout jPanel1Layout = new javax.swing.GroupLayout(jPanel1);
        jPanel1.setLayout(jPanel1Layout);
        jPanel1Layout.setHorizontalGroup(
            jPanel1Layout.createParallelGroup(javax.swing.GroupLayout.Alignment.LEADING)
            .addGroup(jPanel1Layout.createSequentialGroup()
                .addContainerGap()
                .addGroup(jPanel1Layout.createParallelGroup(javax.swing.GroupLayout.Alignment.LEADING)
                    .addGroup(jPanel1Layout.createSequentialGroup()
                        .addComponent(jLabel11, javax.swing.GroupLayout.DEFAULT_SIZE, javax.swing.GroupLayout.DEFAULT_SIZE, Short.MAX_VALUE)
                        .addContainerGap())
                    .addGroup(jPanel1Layout.createSequentialGroup()
                        .addComponent(jPanel2, javax.swing.GroupLayout.PREFERRED_SIZE, javax.swing.GroupLayout.DEFAULT_SIZE, javax.swing.GroupLayout.PREFERRED_SIZE)
                        .addGap(12, 12, 12)
                        .addComponent(jPanel3, javax.swing.GroupLayout.DEFAULT_SIZE, javax.swing.GroupLayout.DEFAULT_SIZE, Short.MAX_VALUE)
                        .addGap(15, 15, 15))))
        );
        jPanel1Layout.setVerticalGroup(
            jPanel1Layout.createParallelGroup(javax.swing.GroupLayout.Alignment.LEADING)
            .addGroup(jPanel1Layout.createSequentialGroup()
                .addContainerGap()
                .addComponent(jLabel11, javax.swing.GroupLayout.PREFERRED_SIZE, 32, javax.swing.GroupLayout.PREFERRED_SIZE)
                .addPreferredGap(javax.swing.LayoutStyle.ComponentPlacement.RELATED)
                .addGroup(jPanel1Layout.createParallelGroup(javax.swing.GroupLayout.Alignment.LEADING)
                    .addComponent(jPanel2, javax.swing.GroupLayout.DEFAULT_SIZE, javax.swing.GroupLayout.DEFAULT_SIZE, Short.MAX_VALUE)
                    .addComponent(jPanel3, javax.swing.GroupLayout.DEFAULT_SIZE, javax.swing.GroupLayout.DEFAULT_SIZE, Short.MAX_VALUE)))
        );

        getContentPane().add(jPanel1, java.awt.BorderLayout.CENTER);

        pack();
    }// </editor-fold>//GEN-END:initComponents

    private void jButton4ActionPerformed(java.awt.event.ActionEvent evt) {//GEN-FIRST:event_jButton4ActionPerformed

        reset();

    }//GEN-LAST:event_jButton4ActionPerformed

    private void jTable1MouseClicked(java.awt.event.MouseEvent evt) {//GEN-FIRST:event_jTable1MouseClicked

        if (evt.getClickCount() == 2) {

            int viewRow = jTable1.getSelectedRow();

            if (viewRow == -1) {
                return;
            }

            int row = jTable1.convertRowIndexToModel(viewRow);

            jTextField1.setText(String.valueOf(jTable1.getModel().getValueAt(row, 0)));
            jTextField4.setText(String.valueOf(jTable1.getModel().getValueAt(row, 1)));
            jTextField5.setText(String.valueOf(jTable1.getModel().getValueAt(row, 2)));
            jTextField2.setText(String.valueOf(jTable1.getModel().getValueAt(row, 3)));
            jTextField3.setText(String.valueOf(jTable1.getModel().getValueAt(row, 4)));
            jTextField6.setText(String.valueOf(jTable1.getModel().getValueAt(row, 5)));
            jTextField8.setText(String.valueOf(jTable1.getModel().getValueAt(row, 6)));
            jTextField10.setText(String.valueOf(jTable1.getModel().getValueAt(row, 7)));

            try {

                jTextField1.setEditable(false);
                jTextField4.setEditable(false);
                jTextField5.setEditable(false);
                jTextField2.setEditable(false);
                jTextField3.setEditable(false);
                jTextField6.setEditable(false);
                jTextField8.setEditable(false);
                jTextField10.setEditable(false);

            } catch (Exception e) {
                e.printStackTrace();
            }
        }
    }//GEN-LAST:event_jTable1MouseClicked

    private void jTable1MousePressed(java.awt.event.MouseEvent evt) {//GEN-FIRST:event_jTable1MousePressed

    }//GEN-LAST:event_jTable1MousePressed

    private void jTable1MouseReleased(java.awt.event.MouseEvent evt) {//GEN-FIRST:event_jTable1MouseReleased

    }//GEN-LAST:event_jTable1MouseReleased

    private void jButton5ActionPerformed(java.awt.event.ActionEvent evt) {//GEN-FIRST:event_jButton5ActionPerformed

        try {

            if (appointmentId <= 0) {

                JOptionPane.showMessageDialog(
                        this,
                        "Please load a valid appointment first.",
                        "Warning",
                        JOptionPane.WARNING_MESSAGE
                );

                return;
            }

            String treatmentCost = jTextField9.getText().trim();
            String consultationFee = jTextField10.getText().trim();
            String totalAmount = jTextField11.getText().trim();

            if (treatmentCost.isEmpty()
                    || consultationFee.isEmpty()
                    || totalAmount.isEmpty()) {

                JOptionPane.showMessageDialog(
                        this,
                        "Payment details are missing.",
                        "Warning",
                        JOptionPane.WARNING_MESSAGE
                );

                return;
            }

            String paidStatusSQL
                    = "SELECT payment_status_id "
                    + "FROM payment_status "
                    + "WHERE payment_status = 'Paid'";

            ResultSet paidRS = MySQL.executeSearch(paidStatusSQL);

            int paidStatusId = 0;

            if (paidRS.next()) {
                paidStatusId = paidRS.getInt("payment_status_id");
            }

            if (paidStatusId == 0) {

                JOptionPane.showMessageDialog(
                        this,
                        "Paid status was not found in database.",
                        "Database Error",
                        JOptionPane.ERROR_MESSAGE
                );

                return;
            }

            String insertPaymentSQL
                    = "INSERT INTO payment "
                    + "(treatment_cost, consultation_fee, total_amount, "
                    + "payment_date_and_time, "
                    + "Payment_status_payment_status_id, "
                    + "Appointment_appointment_id) "
                    + "VALUES ("
                    + "'" + treatmentCost + "', "
                    + "'" + consultationFee + "', "
                    + "'" + totalAmount + "', "
                    + "NOW(), "
                    + paidStatusId + ", "
                    + appointmentId
                    + ")";

            MySQL.executeIUD(insertPaymentSQL);

            String verifyPaymentSQL
                    = "SELECT payment.payment_id "
                    + "FROM payment "
                    + "WHERE payment.Appointment_appointment_id = "
                    + appointmentId
                    + " ORDER BY payment.payment_id DESC LIMIT 1";

            ResultSet verifyRS
                    = MySQL.executeSearch(verifyPaymentSQL);

            if (!verifyRS.next()) {

                JOptionPane.showMessageDialog(
                        this,
                        "Payment was not inserted. "
                        + "Appointment status was not changed.",
                        "Payment Error",
                        JOptionPane.ERROR_MESSAGE
                );

                return;
            }

            String completedStatusSQL
                    = "SELECT appointment_status_id "
                    + "FROM appointment_status "
                    + "WHERE appointment_status = 'Completed'";

            ResultSet completedRS
                    = MySQL.executeSearch(completedStatusSQL);

            int completedStatusId = 0;

            if (completedRS.next()) {
                completedStatusId
                        = completedRS.getInt("appointment_status_id");
            }

            if (completedStatusId == 0) {

                JOptionPane.showMessageDialog(
                        this,
                        "Completed status was not found.",
                        "Database Error",
                        JOptionPane.ERROR_MESSAGE
                );

                return;
            }

            String updateAppointmentSQL
                    = "UPDATE appointment "
                    + "SET Appointment_status_appointment_status_id = "
                    + completedStatusId
                    + " WHERE appointment_id = "
                    + appointmentId;

            MySQL.executeIUD(updateAppointmentSQL);

            JOptionPane.showMessageDialog(this, "Payment successfully completed.", "Payment Success", JOptionPane.INFORMATION_MESSAGE);

            jButton5.setEnabled(false);

            jButton6.setEnabled(true);

        } catch (Exception e) {

            e.printStackTrace();

            JOptionPane.showMessageDialog(
                    this,
                    "Payment failed. Please try again.",
                    "Error",
                    JOptionPane.ERROR_MESSAGE
            );
        }

    }//GEN-LAST:event_jButton5ActionPerformed

    private void jButton6ActionPerformed(java.awt.event.ActionEvent evt) {//GEN-FIRST:event_jButton6ActionPerformed

        try {

            if (appointmentId <= 0) {

                JOptionPane.showMessageDialog(
                        this,
                        "Please complete a payment first.",
                        "Warning",
                        JOptionPane.WARNING_MESSAGE
                );

                return;
            }

            String sql
                    = "SELECT payment_id "
                    + "FROM payment "
                    + "WHERE Appointment_appointment_id = "
                    + appointmentId
                    + " ORDER BY payment_id DESC LIMIT 1";

            ResultSet rs = MySQL.executeSearch(sql);

            if (rs.next()) {

                int paymentId = rs.getInt("payment_id");

                printPaymentInvoice(paymentId);

            } else {

                JOptionPane.showMessageDialog(
                        this,
                        "No payment found for this appointment.",
                        "Invoice Error",
                        JOptionPane.ERROR_MESSAGE
                );
            }

        } catch (Exception e) {

            e.printStackTrace();

            JOptionPane.showMessageDialog(
                    this,
                    "Error printing invoice.\n"
                    + e.getMessage(),
                    "Error",
                    JOptionPane.ERROR_MESSAGE
            );
        }
        reset();
    }//GEN-LAST:event_jButton6ActionPerformed

    private void jTextField1ActionPerformed(java.awt.event.ActionEvent evt) {//GEN-FIRST:event_jTextField1ActionPerformed
        // TODO add your handling code here:
    }//GEN-LAST:event_jTextField1ActionPerformed

    private void jTextField1KeyPressed(java.awt.event.KeyEvent evt) {//GEN-FIRST:event_jTextField1KeyPressed

        if (evt.getKeyCode() == java.awt.event.KeyEvent.VK_ENTER) {

            evt.consume();

            String appointmentNumber = jTextField1.getText().trim();

            if (appointmentNumber.isEmpty()) {

                JOptionPane.showMessageDialog(
                        this,
                        "Please enter Patient Appointment Number",
                        "Warning",
                        JOptionPane.WARNING_MESSAGE
                );

                jButton4.setEnabled(false);
                jButton5.setEnabled(false);
                jButton6.setEnabled(false);

                return;
            }

            boolean appointmentFound = loadAppointmentDetails();

            if (appointmentFound) {
                jButton4.setEnabled(true);
                jButton5.setEnabled(true);

            } else {
                jButton4.setEnabled(false);
                jButton5.setEnabled(false);
                jButton6.setEnabled(false);
            }

        }


    }//GEN-LAST:event_jTextField1KeyPressed

    private void jTextField13KeyPressed(java.awt.event.KeyEvent evt) {//GEN-FIRST:event_jTextField13KeyPressed

        if (evt.getKeyCode() == java.awt.event.KeyEvent.VK_ENTER) {

            evt.consume();

            try {

                String amountReceivedText
                        = jTextField13.getText().trim();

                String totalAmountText
                        = jTextField11.getText().trim();

                if (amountReceivedText.isEmpty()) {

                    JOptionPane.showMessageDialog(
                            this,
                            "Please enter the amount received.",
                            "Warning",
                            JOptionPane.WARNING_MESSAGE
                    );

                    jTextField13.requestFocus();
                    return;
                }

                double amountReceived
                        = Double.parseDouble(amountReceivedText);

                double totalAmount
                        = Double.parseDouble(totalAmountText);

                if (amountReceived < totalAmount) {

                    JOptionPane.showMessageDialog(
                            this,
                            "Amount received cannot be less than the total amount.",
                            "Invalid Amount",
                            JOptionPane.WARNING_MESSAGE
                    );

                    jTextField12.setText("");
                    jTextField13.requestFocus();
                    return;
                }

                double balance
                        = amountReceived - totalAmount;

                jTextField12.setText(
                        String.format("%.2f", balance)
                );

            } catch (NumberFormatException e) {

                JOptionPane.showMessageDialog(
                        this,
                        "Please enter numbers only.",
                        "Invalid Amount",
                        JOptionPane.WARNING_MESSAGE
                );

                jTextField13.setText("");
                jTextField12.setText("");
                jTextField13.requestFocus();
            }
        }


    }//GEN-LAST:event_jTextField13KeyPressed


    // Variables declaration - do not modify//GEN-BEGIN:variables
    private javax.swing.JButton jButton4;
    private javax.swing.JButton jButton5;
    private javax.swing.JButton jButton6;
    private javax.swing.JLabel jLabel1;
    private javax.swing.JLabel jLabel11;
    private javax.swing.JLabel jLabel17;
    private javax.swing.JLabel jLabel19;
    private javax.swing.JLabel jLabel2;
    private javax.swing.JLabel jLabel20;
    private javax.swing.JLabel jLabel21;
    private javax.swing.JLabel jLabel22;
    private javax.swing.JLabel jLabel3;
    private javax.swing.JLabel jLabel4;
    private javax.swing.JLabel jLabel5;
    private javax.swing.JLabel jLabel6;
    private javax.swing.JLabel jLabel7;
    private javax.swing.JPanel jPanel1;
    private javax.swing.JPanel jPanel2;
    private javax.swing.JPanel jPanel3;
    private javax.swing.JScrollPane jScrollPane1;
    private javax.swing.JTable jTable1;
    private javax.swing.JTextField jTextField1;
    private javax.swing.JTextField jTextField10;
    private javax.swing.JTextField jTextField11;
    private javax.swing.JTextField jTextField12;
    private javax.swing.JTextField jTextField13;
    private javax.swing.JTextField jTextField2;
    private javax.swing.JTextField jTextField3;
    private javax.swing.JTextField jTextField4;
    private javax.swing.JTextField jTextField5;
    private javax.swing.JTextField jTextField6;
    private javax.swing.JTextField jTextField8;
    private javax.swing.JTextField jTextField9;
    // End of variables declaration//GEN-END:variables

    private void reset() {

        jTextField1.setText("");
        jTextField4.setText("");
        jTextField5.setText("");
        jTextField2.setText("");
        jTextField3.setText("");
        jTextField6.setText("");
        jTextField8.setText("");
        jTextField9.setText("");
        jTextField10.setText("");
        jTextField11.setText("");
        jTextField12.setText("");
        jTextField13.setText("");

        DefaultTableModel model = (DefaultTableModel) jTable1.getModel();
        model.setRowCount(0);

        jTable1.clearSelection();

        jButton5.setEnabled(false);
        jButton6.setEnabled(false);

        jTextField1.setEditable(true);
        jTextField1.requestFocus();

    }
}
