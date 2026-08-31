package GUI.Receptionist;

import Model.MySQL;
import java.util.HashMap;
import java.sql.ResultSet;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.time.LocalTime;
import java.time.format.DateTimeFormatter;
import javax.swing.JButton;
import javax.swing.JOptionPane;
import raven.datetime.component.date.DateSelectionAble;

/**
 *
 * @author Lenovo
 */
public class Appointments extends javax.swing.JInternalFrame {

    private static HashMap<String, String> dentistMap = new HashMap<>();

    private String patientNIC;
    private String appointmentNumber;
    private LocalTime selectedBookingTime;

    java.awt.Frame Parent;

    public Appointments(java.awt.Frame parent) {
        initComponents();

        setClosable(false);
        setIconifiable(false);
        setMaximizable(false);
        setResizable(false);
        setBorder(null);
        setFrameIcon(null);

        datePicker1.setEnabled(false);
        jFormattedTextField1.setEnabled(false);
        jComboBox2.setEnabled(false);
        jButton1.setEnabled(false);
        jButton11.setEnabled(false);

        datePicker1.setDateSelectionAble(new DateSelectionAble() {
            @Override
            public boolean isDateSelectedAble(LocalDate ld) {
                return !ld.isBefore(LocalDate.now());
            }
        });

        loadDentists();

        Parent = parent;
    }

    private void loadPatient(String nic) {
        try {

            ResultSet resultSet = MySQL.executeSearch("SELECT patient_name, contact_no "
                    + "FROM patient WHERE patient_nic = '" + nic + "';");

            if (resultSet.next()) {

                String patientName = resultSet.getString("patient_name");
                String contactNo = resultSet.getString("contact_no");

                jLabel10.setText(patientName);
                jLabel12.setText(contactNo);

                generateAppointmentNumber();

                datePicker1.setEnabled(true);
                jFormattedTextField1.setEnabled(true);
                jComboBox2.setEnabled(true);
                jButton1.setEnabled(true);
                jButton11.setEnabled(false);

                clearDentistDetails();
                clearSelectedTimeButtons();

                jFormattedTextField1.requestFocus();

            } else {

                JOptionPane.showMessageDialog(this, "Patient not found.", "Warning", JOptionPane.WARNING_MESSAGE);

                jLabel10.setText("");
                jLabel12.setText("");

                datePicker1.setEnabled(false);
                jFormattedTextField1.setEnabled(false);
                jComboBox2.setEnabled(false);
                jButton1.setEnabled(false);
                jButton11.setEnabled(false);

                jFormattedTextField1.setText("");

                clearDentistDetails();
                clearSelectedTimeButtons();

                jTextField8.requestFocus();
            }

        } catch (Exception e) {
            e.printStackTrace();
        }
    }

    private void generateAppointmentNumber() {

        try {

            ResultSet resultSet = MySQL.executeSearch("SELECT MAX(appointment_id) "
                    + "AS last_id FROM appointment");

            int nextId = 1;

            if (resultSet.next()) {

                int lastId = resultSet.getInt("last_id");

                if (!resultSet.wasNull()) {
                    nextId = lastId + 1;
                }
            }

            String appointmentNo = String.format(
                    "APT-%d-%06d",
                    java.time.LocalDate.now().getYear(),
                    nextId
            );

            jTextField6.setText(appointmentNo);

        } catch (Exception e) {
            e.printStackTrace();
        }
    }

    private void loadDentists() {

        try {

            ResultSet resultSet = MySQL.executeSearch("SELECT dentist_id, dentist_name "
                    + "FROM dentist ORDER BY dentist_name;");

            jComboBox2.removeAllItems();
            dentistMap.clear();

            jComboBox2.addItem("Select Dentist");

            while (resultSet.next()) {

                String dentistId = resultSet.getString("dentist_id");
                String dentistName = resultSet.getString("dentist_name");

                jComboBox2.addItem(dentistName);

                dentistMap.put(dentistName, dentistId);
            }

        } catch (Exception e) {
            e.printStackTrace();
        }
    }

    private void checkDentistAvailability(
            String dentistId,
            String dentistName,
            String dateText) {

        try {

            DateTimeFormatter formatter = DateTimeFormatter.ofPattern("dd/MM/yyyy");

            LocalDate selectedDate = LocalDate.parse(dateText, formatter);

            String sql = "SELECT dentist.dentist_name, dentist.specialization, "
                    + "dentist.contact_no, dentist_availability.available_date, "
                    + "dentist_availability.start_time, dentist_availability.end_time, "
                    + "availability_status.availability_status FROM `dentist_availability` "
                    + "INNER JOIN `dentist` ON dentist_availability.Dentist_dentist_id = dentist.dentist_id "
                    + "INNER JOIN `availability_status` "
                    + "ON dentist_availability.availability_status_availability_status_id = availability_status.availability_status_id "
                    + "WHERE dentist_availability.Dentist_dentist_id = '" + dentistId + "' "
                    + "AND dentist_availability.available_date = '" + selectedDate + "';";

            ResultSet resultSet = MySQL.executeSearch(sql);

            if (resultSet.next()) {

                String status = resultSet.getString("availability_status");

                if (status.equalsIgnoreCase("Available")) {

                    String specialization = resultSet.getString("specialization");
                    String contactNo = resultSet.getString("contact_no");
                    String startTime = resultSet.getString("start_time");
                    String endTime = resultSet.getString("end_time");

                    jTextField1.setText(dentistName);
                    jTextField2.setText(specialization);
                    jTextField3.setText(contactNo);

                    jTextField5.setText(startTime + " - " + endTime);

                    jTextField4.setText(dateText);

                    if (startTime == null && endTime == null) {

                        jLabel3.setText("Dentist" + "Not Available");
                        jTextField5.setText("");

                        clearSelectedTimeButtons();
                        jButton11.setEnabled(false);

                    } else if (startTime == null || endTime == null) {

                        jLabel3.setText("Dentist " + "Time Not Available");
                        jTextField5.setText("");

                        clearSelectedTimeButtons();
                        jButton11.setEnabled(false);

                    } else {

                        jLabel3.setText("Dentist " + "Available");

                        jTextField5.setText(startTime + " - " + endTime);

                        loadBookingTimes(dentistId, selectedDate, startTime, endTime);
                    }
                }

            } else {

                JOptionPane.showMessageDialog(
                        this,
                        dentistName
                        + " has no availability on "
                        + dateText
                        + ".",
                        "Dentist Not Available",
                        JOptionPane.WARNING_MESSAGE
                );

                clearDentistDetails();
            }

        } catch (Exception e) {
            e.printStackTrace();

            JOptionPane.showMessageDialog(
                    this,
                    "Error checking dentist availability.",
                    "Error",
                    JOptionPane.ERROR_MESSAGE
            );
        }
    }

    private void loadBookingTimes(
            String dentistId,
            LocalDate selectedDate,
            String startTime,
            String endTime) {

        try {

            clearSelectedTimeButtons();
            selectedBookingTime = null;
            jButton11.setEnabled(false);

            if (startTime == null || endTime == null) {
                return;
            }

            DateTimeFormatter timeFormatter
                    = DateTimeFormatter.ofPattern("HH:mm:ss");

            LocalTime start
                    = LocalTime.parse(startTime, timeFormatter);

            LocalTime end
                    = LocalTime.parse(endTime, timeFormatter);

            JButton[] buttons = {jButton2, jButton5, jButton3, jButton8, jButton9, jButton7, jButton10, jButton6};

            LocalTime current = start;

            int index = 0;

            while (current.plusHours(1).compareTo(end) <= 0
                    && index < buttons.length) {

                JButton button = buttons[index];

                LocalTime slotTime = current;

                button.setText(
                        slotTime.format(
                                DateTimeFormatter.ofPattern("hh:mm a")
                        )
                );

                button.setVisible(true);

                boolean booked
                        = isTimeBooked(
                                dentistId,
                                selectedDate,
                                slotTime
                        );

                button.setEnabled(!booked);

                button.addActionListener(e -> {

                    clearSelectedTimeButtons();

                    selectedBookingTime = slotTime;

                    button.setSelected(true);

                    jButton11.setEnabled(true);

                    System.out.println(
                            "Selected Time: "
                            + selectedBookingTime
                    );
                });

                current = current.plusHours(1);
                index++;
            }

            while (index < buttons.length) {

                buttons[index].setVisible(false);
                buttons[index].setEnabled(false);

                index++;
            }

            jPanel6.revalidate();
            jPanel6.repaint();

        } catch (Exception e) {

            e.printStackTrace();

            clearSelectedTimeButtons();

            JOptionPane.showMessageDialog(
                    this,
                    "Error loading booking times.",
                    "Error",
                    JOptionPane.ERROR_MESSAGE
            );
        }
    }

    private boolean isTimeBooked(
            String dentistId,
            LocalDate selectedDate,
            LocalTime slotTime) {

        try {

            String sql
                    = "SELECT appointment_id "
                    + "FROM appointment "
                    + "WHERE Dentist_dentist_id = '" + dentistId + "' "
                    + "AND appointment_date_and_time >= '"
                    + selectedDate + " " + slotTime + "' "
                    + "AND appointment_date_and_time < '"
                    + selectedDate + " " + slotTime.plusHours(1) + "'";

            ResultSet resultSet = MySQL.executeSearch(sql);

            return resultSet.next();

        } catch (Exception e) {

            e.printStackTrace();
            return false;
        }
    }

    private void clearSelectedTimeButtons() {

        jButton2.setSelected(false);
        jButton5.setSelected(false);
        jButton3.setSelected(false);
        jButton8.setSelected(false);
        jButton9.setSelected(false);
        jButton7.setSelected(false);
        jButton10.setSelected(false);
        jButton6.setSelected(false);

        selectedBookingTime = null;

        jButton11.setEnabled(false);
    }

    private void clearDentistDetails() {

        jTextField1.setText("");
        jTextField2.setText("");
        jTextField3.setText("");
        jTextField4.setText("");
        jTextField5.setText("");

        jLabel3.setText("Dentist Availability");

        clearSelectedTimeButtons();

    }

    private void bookAppointment() {

        try {

            String appointmentNumber
                    = jTextField6.getText().trim();

            String nic
                    = jTextField8.getText().trim();

            String dateText
                    = jFormattedTextField1.getText().trim();

            if (nic.isEmpty()) {

                JOptionPane.showMessageDialog(
                        this,
                        "Please enter Patient NIC first.",
                        "Warning",
                        JOptionPane.WARNING_MESSAGE
                );

                jTextField8.requestFocus();
                return;
            }

            if (dateText.isEmpty()
                    || dateText.equals("--/--/----")) {

                JOptionPane.showMessageDialog(
                        this,
                        "Please select Appointment Date.",
                        "Warning",
                        JOptionPane.WARNING_MESSAGE
                );

                return;
            }

            if (jComboBox2.getSelectedIndex() <= 0) {

                JOptionPane.showMessageDialog(
                        this,
                        "Please select Preferred Dentist.",
                        "Warning",
                        JOptionPane.WARNING_MESSAGE
                );

                return;
            }

            if (selectedBookingTime == null) {

                JOptionPane.showMessageDialog(
                        this,
                        "Please select a booking time.",
                        "Warning",
                        JOptionPane.WARNING_MESSAGE
                );

                return;
            }

            String dentistName
                    = jComboBox2.getSelectedItem().toString();

            String dentistId
                    = dentistMap.get(dentistName);

            if (dentistId == null) {

                JOptionPane.showMessageDialog(
                        this,
                        "Invalid Dentist selected.",
                        "Warning",
                        JOptionPane.WARNING_MESSAGE
                );

                return;
            }

            DateTimeFormatter dateFormatter
                    = DateTimeFormatter.ofPattern("dd/MM/yyyy");

            LocalDate selectedDate
                    = LocalDate.parse(
                            dateText,
                            dateFormatter
                    );

            LocalDateTime appointmentDateTime
                    = LocalDateTime.of(
                            selectedDate,
                            selectedBookingTime
                    );

            if (isTimeBooked(
                    dentistId,
                    selectedDate,
                    selectedBookingTime)) {

                JOptionPane.showMessageDialog(
                        this,
                        "This dentist already has an appointment at this time.",
                        "Time Not Available",
                        JOptionPane.WARNING_MESSAGE
                );

                String availabilityDate
                        = selectedDate.toString();

                checkDentistAvailability(
                        dentistId,
                        dentistName,
                        dateText
                );

                return;
            }

            String appointmentDateTimeString
                    = appointmentDateTime.format(
                            DateTimeFormatter.ofPattern(
                                    "yyyy-MM-dd HH:mm:ss"
                            )
                    );

            MySQL.executeIUD("INSERT INTO appointment "
                    + "(appointment_number, appointment_date_and_time, "
                    + "Appointment_status_appointment_status_id, Patient_patient_nic, "
                    + "Dentist_dentist_id) VALUES ('" + appointmentNumber + "', '"
                    + appointmentDateTimeString + "', '1', '" + nic + "', '" + dentistId + "');");

            JOptionPane.showMessageDialog(
                    this,
                    "Appointment successfully added.",
                    "Success",
                    JOptionPane.INFORMATION_MESSAGE
            );

            selectedBookingTime = null;

            jButton11.setEnabled(false);

            checkDentistAvailability(
                    dentistId,
                    dentistName,
                    dateText
            );

        } catch (Exception e) {

            e.printStackTrace();

            JOptionPane.showMessageDialog(
                    this,
                    "Failed to add appointment.",
                    "Error",
                    JOptionPane.ERROR_MESSAGE
            );
        }
    }

    @SuppressWarnings("unchecked")
    // <editor-fold defaultstate="collapsed" desc="Generated Code">//GEN-BEGIN:initComponents
    private void initComponents() {

        datePicker1 = new raven.datetime.component.date.DatePicker();
        jPanel1 = new javax.swing.JPanel();
        jLabel11 = new javax.swing.JLabel();
        jPanel2 = new javax.swing.JPanel();
        jButton1 = new javax.swing.JButton();
        jLabel13 = new javax.swing.JLabel();
        jComboBox2 = new javax.swing.JComboBox<>();
        jLabel1 = new javax.swing.JLabel();
        jFormattedTextField1 = new javax.swing.JFormattedTextField();
        jButton4 = new javax.swing.JButton();
        jPanel3 = new javax.swing.JPanel();
        jLabel3 = new javax.swing.JLabel();
        jLabel4 = new javax.swing.JLabel();
        jTextField1 = new javax.swing.JTextField();
        jLabel5 = new javax.swing.JLabel();
        jTextField2 = new javax.swing.JTextField();
        jLabel7 = new javax.swing.JLabel();
        jTextField3 = new javax.swing.JTextField();
        jLabel9 = new javax.swing.JLabel();
        jTextField4 = new javax.swing.JTextField();
        jLabel14 = new javax.swing.JLabel();
        jTextField5 = new javax.swing.JTextField();
        jLabel6 = new javax.swing.JLabel();
        jTextField6 = new javax.swing.JTextField();
        jLabel8 = new javax.swing.JLabel();
        jTextField8 = new javax.swing.JTextField();
        jLabel10 = new javax.swing.JLabel();
        jLabel12 = new javax.swing.JLabel();
        jPanel6 = new javax.swing.JPanel();
        jLabel2 = new javax.swing.JLabel();
        jButton2 = new javax.swing.JButton();
        jButton3 = new javax.swing.JButton();
        jButton5 = new javax.swing.JButton();
        jButton6 = new javax.swing.JButton();
        jButton7 = new javax.swing.JButton();
        jButton8 = new javax.swing.JButton();
        jButton9 = new javax.swing.JButton();
        jButton10 = new javax.swing.JButton();
        jButton11 = new javax.swing.JButton();
        jLabel15 = new javax.swing.JLabel();
        jLabel16 = new javax.swing.JLabel();

        datePicker1.setCloseAfterSelected(true);
        datePicker1.setEditor(jFormattedTextField1);
        datePicker1.addPropertyChangeListener(new java.beans.PropertyChangeListener() {
            public void propertyChange(java.beans.PropertyChangeEvent evt) {
                datePicker1PropertyChange(evt);
            }
        });

        jLabel11.setFont(new java.awt.Font("Segoe UI Black", 1, 24)); // NOI18N
        jLabel11.setForeground(new java.awt.Color(0, 0, 102));
        jLabel11.setText("Appointment");

        jPanel2.setBorder(javax.swing.BorderFactory.createEtchedBorder());

        jButton1.setBackground(new java.awt.Color(5, 125, 165));
        jButton1.setFont(new java.awt.Font("Segoe UI Black", 1, 18)); // NOI18N
        jButton1.setForeground(new java.awt.Color(255, 255, 255));
        jButton1.setText("Check Availability");
        jButton1.setPreferredSize(new java.awt.Dimension(91, 33));
        jButton1.addActionListener(new java.awt.event.ActionListener() {
            public void actionPerformed(java.awt.event.ActionEvent evt) {
                jButton1ActionPerformed(evt);
            }
        });

        jLabel13.setFont(new java.awt.Font("Segoe UI", 1, 18)); // NOI18N
        jLabel13.setForeground(new java.awt.Color(0, 0, 102));
        jLabel13.setText("Preferred Dentist");

        jComboBox2.setModel(new javax.swing.DefaultComboBoxModel<>(new String[] { "Item 1", "Item 2", "Item 3", "Item 4" }));

        jLabel1.setFont(new java.awt.Font("Segoe UI", 1, 18)); // NOI18N
        jLabel1.setForeground(new java.awt.Color(5, 125, 165));
        jLabel1.setText("Appointment Date");

        jFormattedTextField1.addActionListener(new java.awt.event.ActionListener() {
            public void actionPerformed(java.awt.event.ActionEvent evt) {
                jFormattedTextField1ActionPerformed(evt);
            }
        });

        jButton4.setFont(new java.awt.Font("Segoe UI Black", 1, 18)); // NOI18N
        jButton4.setText("Cancel");
        jButton4.addActionListener(new java.awt.event.ActionListener() {
            public void actionPerformed(java.awt.event.ActionEvent evt) {
                jButton4ActionPerformed(evt);
            }
        });

        javax.swing.GroupLayout jPanel2Layout = new javax.swing.GroupLayout(jPanel2);
        jPanel2.setLayout(jPanel2Layout);
        jPanel2Layout.setHorizontalGroup(
            jPanel2Layout.createParallelGroup(javax.swing.GroupLayout.Alignment.LEADING)
            .addGroup(jPanel2Layout.createSequentialGroup()
                .addContainerGap()
                .addGroup(jPanel2Layout.createParallelGroup(javax.swing.GroupLayout.Alignment.LEADING)
                    .addComponent(jButton1, javax.swing.GroupLayout.DEFAULT_SIZE, javax.swing.GroupLayout.DEFAULT_SIZE, Short.MAX_VALUE)
                    .addComponent(jLabel13, javax.swing.GroupLayout.Alignment.TRAILING, javax.swing.GroupLayout.DEFAULT_SIZE, javax.swing.GroupLayout.DEFAULT_SIZE, Short.MAX_VALUE)
                    .addComponent(jComboBox2, 0, javax.swing.GroupLayout.DEFAULT_SIZE, Short.MAX_VALUE)
                    .addComponent(jFormattedTextField1)
                    .addComponent(jLabel1, javax.swing.GroupLayout.DEFAULT_SIZE, 302, Short.MAX_VALUE)
                    .addComponent(jButton4, javax.swing.GroupLayout.Alignment.TRAILING, javax.swing.GroupLayout.DEFAULT_SIZE, javax.swing.GroupLayout.DEFAULT_SIZE, Short.MAX_VALUE))
                .addContainerGap())
        );
        jPanel2Layout.setVerticalGroup(
            jPanel2Layout.createParallelGroup(javax.swing.GroupLayout.Alignment.LEADING)
            .addGroup(jPanel2Layout.createSequentialGroup()
                .addGap(17, 17, 17)
                .addComponent(jLabel1)
                .addPreferredGap(javax.swing.LayoutStyle.ComponentPlacement.UNRELATED)
                .addComponent(jFormattedTextField1, javax.swing.GroupLayout.PREFERRED_SIZE, 33, javax.swing.GroupLayout.PREFERRED_SIZE)
                .addGap(18, 18, 18)
                .addComponent(jLabel13)
                .addPreferredGap(javax.swing.LayoutStyle.ComponentPlacement.UNRELATED)
                .addComponent(jComboBox2, javax.swing.GroupLayout.PREFERRED_SIZE, 35, javax.swing.GroupLayout.PREFERRED_SIZE)
                .addPreferredGap(javax.swing.LayoutStyle.ComponentPlacement.RELATED, javax.swing.GroupLayout.DEFAULT_SIZE, Short.MAX_VALUE)
                .addComponent(jButton1, javax.swing.GroupLayout.PREFERRED_SIZE, 46, javax.swing.GroupLayout.PREFERRED_SIZE)
                .addGap(18, 18, 18)
                .addComponent(jButton4, javax.swing.GroupLayout.PREFERRED_SIZE, 46, javax.swing.GroupLayout.PREFERRED_SIZE)
                .addGap(31, 31, 31))
        );

        jPanel3.setBorder(javax.swing.BorderFactory.createEtchedBorder());

        jLabel3.setFont(new java.awt.Font("Segoe UI Black", 1, 24)); // NOI18N
        jLabel3.setHorizontalAlignment(javax.swing.SwingConstants.CENTER);
        jLabel3.setText("Dentist Available");

        jLabel4.setFont(new java.awt.Font("Segoe UI", 0, 18)); // NOI18N
        jLabel4.setText("Dentist Name");

        jTextField1.setEditable(false);

        jLabel5.setFont(new java.awt.Font("Segoe UI", 0, 18)); // NOI18N
        jLabel5.setText("Specialization");

        jTextField2.setEditable(false);

        jLabel7.setFont(new java.awt.Font("Segoe UI", 0, 18)); // NOI18N
        jLabel7.setText("Contact Number");

        jTextField3.setEditable(false);

        jLabel9.setFont(new java.awt.Font("Segoe UI", 0, 18)); // NOI18N
        jLabel9.setText("Available Date");

        jTextField4.setEditable(false);

        jLabel14.setFont(new java.awt.Font("Segoe UI", 0, 18)); // NOI18N
        jLabel14.setText("Available Time");

        jTextField5.setEditable(false);

        javax.swing.GroupLayout jPanel3Layout = new javax.swing.GroupLayout(jPanel3);
        jPanel3.setLayout(jPanel3Layout);
        jPanel3Layout.setHorizontalGroup(
            jPanel3Layout.createParallelGroup(javax.swing.GroupLayout.Alignment.LEADING)
            .addGroup(jPanel3Layout.createSequentialGroup()
                .addContainerGap()
                .addComponent(jLabel3, javax.swing.GroupLayout.DEFAULT_SIZE, javax.swing.GroupLayout.DEFAULT_SIZE, Short.MAX_VALUE)
                .addContainerGap())
            .addGroup(jPanel3Layout.createSequentialGroup()
                .addGap(15, 15, 15)
                .addGroup(jPanel3Layout.createParallelGroup(javax.swing.GroupLayout.Alignment.LEADING, false)
                    .addComponent(jLabel4, javax.swing.GroupLayout.DEFAULT_SIZE, javax.swing.GroupLayout.DEFAULT_SIZE, Short.MAX_VALUE)
                    .addComponent(jLabel5, javax.swing.GroupLayout.Alignment.TRAILING, javax.swing.GroupLayout.DEFAULT_SIZE, javax.swing.GroupLayout.DEFAULT_SIZE, Short.MAX_VALUE)
                    .addComponent(jLabel14, javax.swing.GroupLayout.Alignment.TRAILING, javax.swing.GroupLayout.DEFAULT_SIZE, javax.swing.GroupLayout.DEFAULT_SIZE, Short.MAX_VALUE)
                    .addComponent(jLabel9, javax.swing.GroupLayout.Alignment.TRAILING, javax.swing.GroupLayout.DEFAULT_SIZE, javax.swing.GroupLayout.DEFAULT_SIZE, Short.MAX_VALUE)
                    .addComponent(jLabel7, javax.swing.GroupLayout.Alignment.TRAILING, javax.swing.GroupLayout.PREFERRED_SIZE, 164, javax.swing.GroupLayout.PREFERRED_SIZE))
                .addGap(18, 18, 18)
                .addGroup(jPanel3Layout.createParallelGroup(javax.swing.GroupLayout.Alignment.LEADING)
                    .addGroup(jPanel3Layout.createParallelGroup(javax.swing.GroupLayout.Alignment.LEADING)
                        .addGroup(jPanel3Layout.createParallelGroup(javax.swing.GroupLayout.Alignment.LEADING)
                            .addComponent(jTextField3, javax.swing.GroupLayout.PREFERRED_SIZE, 216, javax.swing.GroupLayout.PREFERRED_SIZE)
                            .addComponent(jTextField4, javax.swing.GroupLayout.Alignment.TRAILING, javax.swing.GroupLayout.PREFERRED_SIZE, 216, javax.swing.GroupLayout.PREFERRED_SIZE))
                        .addComponent(jTextField5, javax.swing.GroupLayout.Alignment.TRAILING, javax.swing.GroupLayout.PREFERRED_SIZE, 216, javax.swing.GroupLayout.PREFERRED_SIZE))
                    .addComponent(jTextField2, javax.swing.GroupLayout.PREFERRED_SIZE, 216, javax.swing.GroupLayout.PREFERRED_SIZE)
                    .addComponent(jTextField1, javax.swing.GroupLayout.PREFERRED_SIZE, 216, javax.swing.GroupLayout.PREFERRED_SIZE))
                .addContainerGap(153, Short.MAX_VALUE))
        );
        jPanel3Layout.setVerticalGroup(
            jPanel3Layout.createParallelGroup(javax.swing.GroupLayout.Alignment.LEADING)
            .addGroup(jPanel3Layout.createSequentialGroup()
                .addContainerGap()
                .addComponent(jLabel3)
                .addPreferredGap(javax.swing.LayoutStyle.ComponentPlacement.UNRELATED, javax.swing.GroupLayout.DEFAULT_SIZE, Short.MAX_VALUE)
                .addGroup(jPanel3Layout.createParallelGroup(javax.swing.GroupLayout.Alignment.BASELINE)
                    .addComponent(jLabel4)
                    .addComponent(jTextField1, javax.swing.GroupLayout.PREFERRED_SIZE, 25, javax.swing.GroupLayout.PREFERRED_SIZE))
                .addPreferredGap(javax.swing.LayoutStyle.ComponentPlacement.UNRELATED)
                .addGroup(jPanel3Layout.createParallelGroup(javax.swing.GroupLayout.Alignment.BASELINE)
                    .addComponent(jLabel5)
                    .addComponent(jTextField2, javax.swing.GroupLayout.PREFERRED_SIZE, 25, javax.swing.GroupLayout.PREFERRED_SIZE))
                .addPreferredGap(javax.swing.LayoutStyle.ComponentPlacement.UNRELATED)
                .addGroup(jPanel3Layout.createParallelGroup(javax.swing.GroupLayout.Alignment.BASELINE)
                    .addComponent(jLabel7)
                    .addComponent(jTextField3, javax.swing.GroupLayout.PREFERRED_SIZE, 25, javax.swing.GroupLayout.PREFERRED_SIZE))
                .addPreferredGap(javax.swing.LayoutStyle.ComponentPlacement.UNRELATED)
                .addGroup(jPanel3Layout.createParallelGroup(javax.swing.GroupLayout.Alignment.BASELINE)
                    .addComponent(jLabel9)
                    .addComponent(jTextField4, javax.swing.GroupLayout.PREFERRED_SIZE, 25, javax.swing.GroupLayout.PREFERRED_SIZE))
                .addPreferredGap(javax.swing.LayoutStyle.ComponentPlacement.UNRELATED)
                .addGroup(jPanel3Layout.createParallelGroup(javax.swing.GroupLayout.Alignment.BASELINE)
                    .addComponent(jLabel14)
                    .addComponent(jTextField5, javax.swing.GroupLayout.PREFERRED_SIZE, 25, javax.swing.GroupLayout.PREFERRED_SIZE))
                .addGap(17, 17, 17))
        );

        jLabel6.setFont(new java.awt.Font("Segoe UI", 1, 18)); // NOI18N
        jLabel6.setForeground(new java.awt.Color(0, 0, 102));
        jLabel6.setText("Patient");

        jTextField6.setEditable(false);
        jTextField6.addActionListener(new java.awt.event.ActionListener() {
            public void actionPerformed(java.awt.event.ActionEvent evt) {
                jTextField6ActionPerformed(evt);
            }
        });

        jLabel8.setFont(new java.awt.Font("Segoe UI", 1, 18)); // NOI18N
        jLabel8.setForeground(new java.awt.Color(0, 0, 102));
        jLabel8.setText("Appointment Number");

        jTextField8.addKeyListener(new java.awt.event.KeyAdapter() {
            public void keyPressed(java.awt.event.KeyEvent evt) {
                jTextField8KeyPressed(evt);
            }
        });

        jLabel10.setFont(new java.awt.Font("Segoe UI", 1, 18)); // NOI18N
        jLabel10.setForeground(new java.awt.Color(5, 125, 165));
        jLabel10.setText("Patient Name");

        jLabel12.setFont(new java.awt.Font("Segoe UI", 1, 18)); // NOI18N
        jLabel12.setForeground(new java.awt.Color(5, 125, 165));
        jLabel12.setText("Patient Contact No");

        jPanel6.setBorder(javax.swing.BorderFactory.createEtchedBorder());

        jLabel2.setFont(new java.awt.Font("Segoe UI", 1, 18)); // NOI18N
        jLabel2.setForeground(new java.awt.Color(5, 125, 165));
        jLabel2.setText("Booked Times");

        jButton2.setText("09:00 AM");

        jButton3.setText("11:00 AM");

        jButton5.setText("10:00 AM");

        jButton6.setText("04:00 PM");

        jButton7.setText("02:00 PM");

        jButton8.setText("12:00 PM");

        jButton9.setText("01:00 PM");
        jButton9.addActionListener(new java.awt.event.ActionListener() {
            public void actionPerformed(java.awt.event.ActionEvent evt) {
                jButton9ActionPerformed(evt);
            }
        });

        jButton10.setText("03:00 PM");

        jButton11.setBackground(new java.awt.Color(5, 125, 165));
        jButton11.setFont(new java.awt.Font("Segoe UI Black", 1, 18)); // NOI18N
        jButton11.setForeground(new java.awt.Color(255, 255, 255));
        jButton11.setText("Book Appointment");
        jButton11.setPreferredSize(new java.awt.Dimension(91, 33));
        jButton11.addActionListener(new java.awt.event.ActionListener() {
            public void actionPerformed(java.awt.event.ActionEvent evt) {
                jButton11ActionPerformed(evt);
            }
        });

        javax.swing.GroupLayout jPanel6Layout = new javax.swing.GroupLayout(jPanel6);
        jPanel6.setLayout(jPanel6Layout);
        jPanel6Layout.setHorizontalGroup(
            jPanel6Layout.createParallelGroup(javax.swing.GroupLayout.Alignment.LEADING)
            .addGroup(jPanel6Layout.createSequentialGroup()
                .addGap(17, 17, 17)
                .addGroup(jPanel6Layout.createParallelGroup(javax.swing.GroupLayout.Alignment.LEADING)
                    .addComponent(jLabel2, javax.swing.GroupLayout.DEFAULT_SIZE, javax.swing.GroupLayout.DEFAULT_SIZE, Short.MAX_VALUE)
                    .addGroup(jPanel6Layout.createSequentialGroup()
                        .addGroup(jPanel6Layout.createParallelGroup(javax.swing.GroupLayout.Alignment.LEADING)
                            .addComponent(jButton5, javax.swing.GroupLayout.DEFAULT_SIZE, javax.swing.GroupLayout.DEFAULT_SIZE, Short.MAX_VALUE)
                            .addComponent(jButton2, javax.swing.GroupLayout.DEFAULT_SIZE, javax.swing.GroupLayout.DEFAULT_SIZE, Short.MAX_VALUE))
                        .addGap(17, 17, 17)
                        .addGroup(jPanel6Layout.createParallelGroup(javax.swing.GroupLayout.Alignment.LEADING)
                            .addComponent(jButton3, javax.swing.GroupLayout.DEFAULT_SIZE, javax.swing.GroupLayout.DEFAULT_SIZE, Short.MAX_VALUE)
                            .addComponent(jButton8, javax.swing.GroupLayout.DEFAULT_SIZE, javax.swing.GroupLayout.DEFAULT_SIZE, Short.MAX_VALUE))
                        .addGap(18, 18, 18)
                        .addGroup(jPanel6Layout.createParallelGroup(javax.swing.GroupLayout.Alignment.LEADING)
                            .addComponent(jButton9, javax.swing.GroupLayout.DEFAULT_SIZE, javax.swing.GroupLayout.DEFAULT_SIZE, Short.MAX_VALUE)
                            .addComponent(jButton7, javax.swing.GroupLayout.DEFAULT_SIZE, javax.swing.GroupLayout.DEFAULT_SIZE, Short.MAX_VALUE))
                        .addGap(18, 18, 18)
                        .addGroup(jPanel6Layout.createParallelGroup(javax.swing.GroupLayout.Alignment.LEADING)
                            .addComponent(jButton10, javax.swing.GroupLayout.DEFAULT_SIZE, javax.swing.GroupLayout.DEFAULT_SIZE, Short.MAX_VALUE)
                            .addComponent(jButton6, javax.swing.GroupLayout.DEFAULT_SIZE, javax.swing.GroupLayout.DEFAULT_SIZE, Short.MAX_VALUE))
                        .addGap(12, 12, 12)))
                .addContainerGap())
            .addGroup(javax.swing.GroupLayout.Alignment.TRAILING, jPanel6Layout.createSequentialGroup()
                .addGap(184, 184, 184)
                .addComponent(jButton11, javax.swing.GroupLayout.DEFAULT_SIZE, javax.swing.GroupLayout.DEFAULT_SIZE, Short.MAX_VALUE)
                .addGap(151, 151, 151))
        );
        jPanel6Layout.setVerticalGroup(
            jPanel6Layout.createParallelGroup(javax.swing.GroupLayout.Alignment.LEADING)
            .addGroup(jPanel6Layout.createSequentialGroup()
                .addContainerGap()
                .addComponent(jLabel2)
                .addGap(19, 19, 19)
                .addGroup(jPanel6Layout.createParallelGroup(javax.swing.GroupLayout.Alignment.BASELINE)
                    .addComponent(jButton3, javax.swing.GroupLayout.PREFERRED_SIZE, 39, javax.swing.GroupLayout.PREFERRED_SIZE)
                    .addComponent(jButton9, javax.swing.GroupLayout.PREFERRED_SIZE, 39, javax.swing.GroupLayout.PREFERRED_SIZE)
                    .addComponent(jButton10, javax.swing.GroupLayout.PREFERRED_SIZE, 38, javax.swing.GroupLayout.PREFERRED_SIZE)
                    .addComponent(jButton2, javax.swing.GroupLayout.PREFERRED_SIZE, 38, javax.swing.GroupLayout.PREFERRED_SIZE))
                .addGap(18, 18, 18)
                .addGroup(jPanel6Layout.createParallelGroup(javax.swing.GroupLayout.Alignment.BASELINE)
                    .addComponent(jButton8, javax.swing.GroupLayout.PREFERRED_SIZE, 39, javax.swing.GroupLayout.PREFERRED_SIZE)
                    .addComponent(jButton7, javax.swing.GroupLayout.PREFERRED_SIZE, 38, javax.swing.GroupLayout.PREFERRED_SIZE)
                    .addComponent(jButton6, javax.swing.GroupLayout.PREFERRED_SIZE, 40, javax.swing.GroupLayout.PREFERRED_SIZE)
                    .addComponent(jButton5, javax.swing.GroupLayout.PREFERRED_SIZE, 39, javax.swing.GroupLayout.PREFERRED_SIZE))
                .addPreferredGap(javax.swing.LayoutStyle.ComponentPlacement.RELATED, 25, Short.MAX_VALUE)
                .addComponent(jButton11, javax.swing.GroupLayout.PREFERRED_SIZE, 46, javax.swing.GroupLayout.PREFERRED_SIZE)
                .addContainerGap())
        );

        jLabel15.setFont(new java.awt.Font("Segoe UI", 1, 18)); // NOI18N
        jLabel15.setForeground(new java.awt.Color(0, 0, 102));
        jLabel15.setText("Patient Name");

        jLabel16.setFont(new java.awt.Font("Segoe UI", 1, 18)); // NOI18N
        jLabel16.setForeground(new java.awt.Color(0, 0, 102));
        jLabel16.setText("Patient Contact No");

        javax.swing.GroupLayout jPanel1Layout = new javax.swing.GroupLayout(jPanel1);
        jPanel1.setLayout(jPanel1Layout);
        jPanel1Layout.setHorizontalGroup(
            jPanel1Layout.createParallelGroup(javax.swing.GroupLayout.Alignment.LEADING)
            .addGroup(jPanel1Layout.createSequentialGroup()
                .addContainerGap()
                .addGroup(jPanel1Layout.createParallelGroup(javax.swing.GroupLayout.Alignment.LEADING)
                    .addComponent(jLabel11, javax.swing.GroupLayout.DEFAULT_SIZE, javax.swing.GroupLayout.DEFAULT_SIZE, Short.MAX_VALUE)
                    .addGroup(jPanel1Layout.createSequentialGroup()
                        .addComponent(jPanel2, javax.swing.GroupLayout.PREFERRED_SIZE, javax.swing.GroupLayout.DEFAULT_SIZE, javax.swing.GroupLayout.PREFERRED_SIZE)
                        .addPreferredGap(javax.swing.LayoutStyle.ComponentPlacement.UNRELATED)
                        .addGroup(jPanel1Layout.createParallelGroup(javax.swing.GroupLayout.Alignment.LEADING)
                            .addComponent(jPanel3, javax.swing.GroupLayout.DEFAULT_SIZE, javax.swing.GroupLayout.DEFAULT_SIZE, Short.MAX_VALUE)
                            .addComponent(jPanel6, javax.swing.GroupLayout.DEFAULT_SIZE, javax.swing.GroupLayout.DEFAULT_SIZE, Short.MAX_VALUE)))
                    .addGroup(jPanel1Layout.createSequentialGroup()
                        .addGroup(jPanel1Layout.createParallelGroup(javax.swing.GroupLayout.Alignment.LEADING, false)
                            .addComponent(jLabel6, javax.swing.GroupLayout.DEFAULT_SIZE, javax.swing.GroupLayout.DEFAULT_SIZE, Short.MAX_VALUE)
                            .addComponent(jLabel8, javax.swing.GroupLayout.DEFAULT_SIZE, 204, Short.MAX_VALUE))
                        .addGap(18, 18, 18)
                        .addGroup(jPanel1Layout.createParallelGroup(javax.swing.GroupLayout.Alignment.LEADING)
                            .addComponent(jTextField8)
                            .addComponent(jTextField6))
                        .addPreferredGap(javax.swing.LayoutStyle.ComponentPlacement.RELATED, javax.swing.GroupLayout.DEFAULT_SIZE, Short.MAX_VALUE)
                        .addGroup(jPanel1Layout.createParallelGroup(javax.swing.GroupLayout.Alignment.LEADING, false)
                            .addComponent(jLabel16, javax.swing.GroupLayout.DEFAULT_SIZE, javax.swing.GroupLayout.DEFAULT_SIZE, Short.MAX_VALUE)
                            .addComponent(jLabel15, javax.swing.GroupLayout.PREFERRED_SIZE, 171, javax.swing.GroupLayout.PREFERRED_SIZE))
                        .addPreferredGap(javax.swing.LayoutStyle.ComponentPlacement.UNRELATED)
                        .addGroup(jPanel1Layout.createParallelGroup(javax.swing.GroupLayout.Alignment.LEADING, false)
                            .addComponent(jLabel10, javax.swing.GroupLayout.DEFAULT_SIZE, javax.swing.GroupLayout.DEFAULT_SIZE, Short.MAX_VALUE)
                            .addComponent(jLabel12, javax.swing.GroupLayout.DEFAULT_SIZE, 174, Short.MAX_VALUE))))
                .addContainerGap())
        );
        jPanel1Layout.setVerticalGroup(
            jPanel1Layout.createParallelGroup(javax.swing.GroupLayout.Alignment.LEADING)
            .addGroup(jPanel1Layout.createSequentialGroup()
                .addContainerGap()
                .addComponent(jLabel11, javax.swing.GroupLayout.PREFERRED_SIZE, 32, javax.swing.GroupLayout.PREFERRED_SIZE)
                .addPreferredGap(javax.swing.LayoutStyle.ComponentPlacement.UNRELATED)
                .addGroup(jPanel1Layout.createParallelGroup(javax.swing.GroupLayout.Alignment.LEADING)
                    .addGroup(jPanel1Layout.createSequentialGroup()
                        .addGroup(jPanel1Layout.createParallelGroup(javax.swing.GroupLayout.Alignment.BASELINE)
                            .addComponent(jLabel6, javax.swing.GroupLayout.PREFERRED_SIZE, 35, javax.swing.GroupLayout.PREFERRED_SIZE)
                            .addComponent(jTextField8, javax.swing.GroupLayout.DEFAULT_SIZE, 34, Short.MAX_VALUE))
                        .addPreferredGap(javax.swing.LayoutStyle.ComponentPlacement.RELATED)
                        .addGroup(jPanel1Layout.createParallelGroup(javax.swing.GroupLayout.Alignment.BASELINE)
                            .addComponent(jLabel8, javax.swing.GroupLayout.PREFERRED_SIZE, 34, javax.swing.GroupLayout.PREFERRED_SIZE)
                            .addComponent(jTextField6, javax.swing.GroupLayout.PREFERRED_SIZE, 35, javax.swing.GroupLayout.PREFERRED_SIZE)))
                    .addGroup(jPanel1Layout.createSequentialGroup()
                        .addGroup(jPanel1Layout.createParallelGroup(javax.swing.GroupLayout.Alignment.TRAILING)
                            .addGroup(jPanel1Layout.createSequentialGroup()
                                .addComponent(jLabel15)
                                .addPreferredGap(javax.swing.LayoutStyle.ComponentPlacement.RELATED)
                                .addComponent(jLabel16))
                            .addGroup(jPanel1Layout.createSequentialGroup()
                                .addComponent(jLabel10)
                                .addPreferredGap(javax.swing.LayoutStyle.ComponentPlacement.RELATED)
                                .addComponent(jLabel12)))
                        .addGap(0, 0, Short.MAX_VALUE)))
                .addGroup(jPanel1Layout.createParallelGroup(javax.swing.GroupLayout.Alignment.LEADING)
                    .addGroup(jPanel1Layout.createSequentialGroup()
                        .addComponent(jPanel3, javax.swing.GroupLayout.PREFERRED_SIZE, javax.swing.GroupLayout.DEFAULT_SIZE, javax.swing.GroupLayout.PREFERRED_SIZE)
                        .addPreferredGap(javax.swing.LayoutStyle.ComponentPlacement.RELATED)
                        .addComponent(jPanel6, javax.swing.GroupLayout.DEFAULT_SIZE, javax.swing.GroupLayout.DEFAULT_SIZE, Short.MAX_VALUE))
                    .addComponent(jPanel2, javax.swing.GroupLayout.DEFAULT_SIZE, javax.swing.GroupLayout.DEFAULT_SIZE, Short.MAX_VALUE))
                .addContainerGap())
        );

        getContentPane().add(jPanel1, java.awt.BorderLayout.CENTER);

        pack();
    }// </editor-fold>//GEN-END:initComponents

    private void jButton1ActionPerformed(java.awt.event.ActionEvent evt) {//GEN-FIRST:event_jButton1ActionPerformed

        String nic = jTextField8.getText().trim();

        if (nic.isEmpty()) {
            JOptionPane.showMessageDialog(
                    this,
                    "Please enter Patient NIC first.",
                    "Warning",
                    JOptionPane.WARNING_MESSAGE
            );
            jTextField8.requestFocus();
            return;
        }

        String dateText = jFormattedTextField1.getText().trim();

        if (dateText.isEmpty()) {
            JOptionPane.showMessageDialog(
                    this,
                    "Please select Appointment Date.",
                    "Warning",
                    JOptionPane.WARNING_MESSAGE
            );
            return;
        }

        if (jComboBox2.getSelectedIndex() <= 0) {
            JOptionPane.showMessageDialog(
                    this,
                    "Please select Preferred Dentist.",
                    "Warning",
                    JOptionPane.WARNING_MESSAGE
            );
            return;
        }

        String dentistName = jComboBox2.getSelectedItem().toString();

        String dentistId = dentistMap.get(dentistName);

        if (dentistId == null) {
            JOptionPane.showMessageDialog(
                    this,
                    "Invalid Dentist selected.",
                    "Warning",
                    JOptionPane.WARNING_MESSAGE
            );
            return;
        }

        checkDentistAvailability(dentistId, dentistName, dateText);


    }//GEN-LAST:event_jButton1ActionPerformed

    private void jTextField6ActionPerformed(java.awt.event.ActionEvent evt) {//GEN-FIRST:event_jTextField6ActionPerformed
        // TODO add your handling code here:
    }//GEN-LAST:event_jTextField6ActionPerformed

    private void jButton4ActionPerformed(java.awt.event.ActionEvent evt) {//GEN-FIRST:event_jButton4ActionPerformed

         reset();

    }//GEN-LAST:event_jButton4ActionPerformed

    private void jButton9ActionPerformed(java.awt.event.ActionEvent evt) {//GEN-FIRST:event_jButton9ActionPerformed
        // TODO add your handling code here:
    }//GEN-LAST:event_jButton9ActionPerformed

    private void jTextField8KeyPressed(java.awt.event.KeyEvent evt) {//GEN-FIRST:event_jTextField8KeyPressed

        if (evt.getKeyCode() == java.awt.event.KeyEvent.VK_ENTER) {

            evt.consume();

            String nic = jTextField8.getText().trim();

            if (nic.isEmpty()) {

                JOptionPane.showMessageDialog(
                        this,
                        "Please enter Patient NIC",
                        "Warning",
                        JOptionPane.WARNING_MESSAGE
                );

                return;
            }

            loadPatient(nic);
        }

    }//GEN-LAST:event_jTextField8KeyPressed

    private void jFormattedTextField1ActionPerformed(java.awt.event.ActionEvent evt) {//GEN-FIRST:event_jFormattedTextField1ActionPerformed

        String dateText = jFormattedTextField1.getText().trim();

        if (dateText.isEmpty()
                || dateText.equals("--/--/----")) {
            return;
        }

        try {

            DateTimeFormatter formatter
                    = DateTimeFormatter.ofPattern("dd/MM/yyyy");

            LocalDate selectedDate
                    = LocalDate.parse(dateText, formatter);

            LocalDate today = LocalDate.now();

            if (selectedDate.isBefore(today)) {

                JOptionPane.showMessageDialog(
                        this,
                        "Please select today or a future date.",
                        "Invalid Date",
                        JOptionPane.WARNING_MESSAGE
                );

                jFormattedTextField1.setText("");

                clearDentistDetails();
                clearSelectedTimeButtons();

                jButton11.setEnabled(false);

                return;
            }

        } catch (Exception e) {

            JOptionPane.showMessageDialog(
                    this,
                    "Please enter a valid date.",
                    "Invalid Date",
                    JOptionPane.WARNING_MESSAGE
            );

            jFormattedTextField1.setText("");

            clearDentistDetails();
            clearSelectedTimeButtons();

            jButton11.setEnabled(false);
        }

    }//GEN-LAST:event_jFormattedTextField1ActionPerformed

    private void datePicker1PropertyChange(java.beans.PropertyChangeEvent evt) {//GEN-FIRST:event_datePicker1PropertyChange

        String dateText = jFormattedTextField1.getText().trim();

        if (dateText.isEmpty()
                || dateText.equals("--/--/----")) {
            return;
        }

        try {

            DateTimeFormatter formatter
                    = DateTimeFormatter.ofPattern("dd/MM/yyyy");

            LocalDate selectedDate
                    = LocalDate.parse(dateText, formatter);

            if (selectedDate.isBefore(LocalDate.now())) {

                JOptionPane.showMessageDialog(
                        this,
                        "Please select today or a future date.",
                        "Invalid Date",
                        JOptionPane.WARNING_MESSAGE
                );

                jFormattedTextField1.setText("");

                clearDentistDetails();
                clearSelectedTimeButtons();

                jButton11.setEnabled(false);

                return;
            }

            clearDentistDetails();
            clearSelectedTimeButtons();

            jButton11.setEnabled(false);

        } catch (Exception e) {

            jFormattedTextField1.setText("");

            clearDentistDetails();
            clearSelectedTimeButtons();

            jButton11.setEnabled(false);
        }

    }//GEN-LAST:event_datePicker1PropertyChange

    private void jButton11ActionPerformed(java.awt.event.ActionEvent evt) {//GEN-FIRST:event_jButton11ActionPerformed
        if (selectedBookingTime == null) {

            JOptionPane.showMessageDialog(
                    this,
                    "Please select a booking time.",
                    "Warning",
                    JOptionPane.WARNING_MESSAGE
            );

            return;
        }

        bookAppointment();
    }//GEN-LAST:event_jButton11ActionPerformed


    // Variables declaration - do not modify//GEN-BEGIN:variables
    private raven.datetime.component.date.DatePicker datePicker1;
    private javax.swing.JButton jButton1;
    private javax.swing.JButton jButton10;
    private javax.swing.JButton jButton11;
    private javax.swing.JButton jButton2;
    private javax.swing.JButton jButton3;
    private javax.swing.JButton jButton4;
    private javax.swing.JButton jButton5;
    private javax.swing.JButton jButton6;
    private javax.swing.JButton jButton7;
    private javax.swing.JButton jButton8;
    private javax.swing.JButton jButton9;
    private javax.swing.JComboBox<String> jComboBox2;
    private javax.swing.JFormattedTextField jFormattedTextField1;
    private javax.swing.JLabel jLabel1;
    private javax.swing.JLabel jLabel10;
    private javax.swing.JLabel jLabel11;
    private javax.swing.JLabel jLabel12;
    private javax.swing.JLabel jLabel13;
    private javax.swing.JLabel jLabel14;
    private javax.swing.JLabel jLabel15;
    private javax.swing.JLabel jLabel16;
    private javax.swing.JLabel jLabel2;
    private javax.swing.JLabel jLabel3;
    private javax.swing.JLabel jLabel4;
    private javax.swing.JLabel jLabel5;
    private javax.swing.JLabel jLabel6;
    private javax.swing.JLabel jLabel7;
    private javax.swing.JLabel jLabel8;
    private javax.swing.JLabel jLabel9;
    private javax.swing.JPanel jPanel1;
    private javax.swing.JPanel jPanel2;
    private javax.swing.JPanel jPanel3;
    private javax.swing.JPanel jPanel6;
    private javax.swing.JTextField jTextField1;
    private javax.swing.JTextField jTextField2;
    private javax.swing.JTextField jTextField3;
    private javax.swing.JTextField jTextField4;
    private javax.swing.JTextField jTextField5;
    private javax.swing.JTextField jTextField6;
    private javax.swing.JTextField jTextField8;
    // End of variables declaration//GEN-END:variables

    private void reset() {

    
    jTextField8.setText("");
    jLabel10.setText("");
    jLabel12.setText("");

    
    jTextField6.setText("");
    jFormattedTextField1.setText("");

    
    jComboBox2.setSelectedIndex(0);

    jTextField1.setText("");
    jTextField2.setText("");
    jTextField3.setText("");
    jTextField4.setText("");
    jTextField5.setText("");

    jLabel3.setText("Dentist Availability");

    
    selectedBookingTime = null;

    
    clearSelectedTimeButtons();

    
    datePicker1.setEnabled(false);
    jFormattedTextField1.setEnabled(false);
    jComboBox2.setEnabled(false);
    jButton1.setEnabled(false);
    jButton11.setEnabled(false);

    
    generateAppointmentNumber();

    
    jTextField8.requestFocus();

    }
}
