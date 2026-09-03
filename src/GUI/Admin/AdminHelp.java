package GUI.Admin;

import java.awt.Color;
import java.awt.BorderLayout;
import java.awt.Dimension;
import java.awt.FlowLayout;
import java.awt.Font;
import java.awt.Frame;
import java.awt.IllegalComponentStateException;
import java.awt.Point;
import java.awt.Toolkit;
import java.lang.reflect.Field;
import java.util.ArrayList;
import java.util.List;
import javax.swing.BorderFactory;
import javax.swing.JButton;
import javax.swing.JDialog;
import javax.swing.JLabel;
import javax.swing.JOptionPane;
import javax.swing.JPanel;
import javax.swing.JScrollPane;
import javax.swing.JTextArea;
import javax.swing.SwingUtilities;
import javax.swing.UIManager;
import javax.swing.border.Border;

/**
 *
 * @author Lenovo
 */
public class AdminHelp extends javax.swing.JDialog {

    private JLabel titleLabel;
    private JTextArea descriptionArea;

    private JButton backButton;
    private JButton nextButton;
    private JButton skipButton;

    private List<JButton> targetButtons;
    private List<String> titles;
    private List<String> descriptions;

    private int currentStep = 0;

    private Color originalColor = null;
     
    private JButton highlightedButton;

    private java.awt.Frame parentFrame;

    public AdminHelp(java.awt.Frame parent, boolean modal) {
        super(parent, modal);

        this.parentFrame = parent;

        initComponents();

        initializePopup();

        initializeTour();

        setLocationRelativeTo(parent);

        
        SwingUtilities.invokeLater(() -> {
            showStep();
            setVisible(true);
        });
    }

    private void initializePopup() {

        titleLabel = new JLabel("Admin Help");
        titleLabel.setFont(new Font("Segoe UI", Font.BOLD, 18));
        titleLabel.setForeground(new Color(0, 102, 153));

        descriptionArea = new JTextArea();
        descriptionArea.setFont(new Font("Segoe UI", Font.PLAIN, 14));
        descriptionArea.setLineWrap(true);
        descriptionArea.setWrapStyleWord(true);
        descriptionArea.setEditable(false);
        descriptionArea.setOpaque(false);

        backButton = new JButton("← Back");
        nextButton = new JButton("Next →");
        skipButton = new JButton("Skip");

        backButton.addActionListener(e -> previousStep());

        nextButton.addActionListener(e -> nextStep());

        skipButton.addActionListener(e -> skipTour());

        JPanel buttonPanel = new JPanel(new FlowLayout(FlowLayout.RIGHT));
        buttonPanel.setOpaque(false);

        buttonPanel.add(skipButton);
        buttonPanel.add(backButton);
        buttonPanel.add(nextButton);

        JPanel contentPanel = new JPanel(new BorderLayout(10, 10));
        contentPanel.setBorder(
                BorderFactory.createEmptyBorder(15, 15, 15, 15)
        );

        contentPanel.add(titleLabel, BorderLayout.NORTH);
        contentPanel.add(
                new JScrollPane(descriptionArea),
                BorderLayout.CENTER
        );
        contentPanel.add(buttonPanel, BorderLayout.SOUTH);

        getContentPane().setLayout(new BorderLayout());
        getContentPane().add(contentPanel, BorderLayout.CENTER);

        setSize(400, 230);

        setResizable(false);
    }

    private void initializeTour() {

        targetButtons = new ArrayList<>();
        titles = new ArrayList<>();
        descriptions = new ArrayList<>();

        AdminDashboard dashboard = (AdminDashboard) parentFrame;

       
        targetButtons.add(getButton(dashboard, "jButton4"));

        titles.add("Dashboard");

        descriptions.add(
                "Welcome to the Admin Dashboard. "
                + "This section gives you an overview of the "
                + "Sunrise Dental System, including today's appointments, "
                + "total patients, total dentists and appointment statistics."
        );

        
        targetButtons.add(getButton(dashboard, "jButton7"));

        titles.add("Receptionists");

        descriptions.add(
                "Use this section to manage receptionist accounts "
                + "and view receptionist information."
        );

        
        targetButtons.add(getButton(dashboard, "jButton8"));

        titles.add("Dentists");

        descriptions.add(
                "Use this section to manage dentist information "
                + "and view registered dentists."
        );

        
        targetButtons.add(getButton(dashboard, "jButton1"));

        titles.add("Treatments");

        descriptions.add(
                "Use this section to manage the dental treatments "
                + "available in the Sunrise Dental System."
        );

        
        targetButtons.add(getButton(dashboard, "jButton2"));

        titles.add("Payments");

        descriptions.add(
                "Use this section to view and manage patient "
                + "payment information and payment records."
        );

        
        targetButtons.add(getButton(dashboard, "jButton3"));

        titles.add("Reports");

        descriptions.add(
                "Use this section to generate and view reports "
                + "related to appointments and other system activities."
        );

        targetButtons.add(getButton(dashboard, "jButton5"));

        titles.add("Help");

        descriptions.add(
                "You are currently using the Help feature. "
                + "Use this guided tour to learn about the main "
                + "features of the Admin Dashboard."
        );

        
        targetButtons.add(getButton(dashboard, "jButton9"));

        titles.add("Log Out");

        descriptions.add(
                "Click Log Out when you want to safely exit "
                + "the Admin section of the Sunrise Dental System."
        );
    }

    
    private void addTourStep(
            JButton button,
            String title,
            String description) {

        if (button != null) {

            targetButtons.add(button);
            titles.add(title);
            descriptions.add(description);
        }
    }

    
    private JButton getButton(
            AdminDashboard dashboard,
            String fieldName) {

        try {

            Field field
                    = AdminDashboard.class.getDeclaredField(fieldName);

            field.setAccessible(true);

            Object value = field.get(dashboard);

            if (value instanceof JButton) {
                return (JButton) value;
            }

        } catch (Exception e) {

            System.out.println(
                    "Could not find button: " + fieldName
            );
        }

        return null;
    }

    private void showStep() {

        if (currentStep < 0
                || currentStep >= targetButtons.size()) {
            return;
        }

        JButton currentButton
                = targetButtons.get(currentStep);

        String title
                = titles.get(currentStep);

        String description
                = descriptions.get(currentStep);

        
        removeHighlight();

        
        if (currentButton != null) {
            highlightButton(currentButton);
        }

        
        titleLabel.setText(title);
        descriptionArea.setText(description);

        
        backButton.setEnabled(currentStep > 0);

        
        if (currentStep == targetButtons.size() - 1) {
            nextButton.setText("Finish");
        } else {
            nextButton.setText("Next →");
        }

        
        positionPopup(currentButton);

        revalidate();
        repaint();
    }

    
    private void highlightButton(JButton button) {

        if (button == null) {
            return;
        }

        originalColor = button.getBackground();

        button.setBackground(new Color(255, 215, 90));

        button.setBorder(
                BorderFactory.createLineBorder(
                        new Color(255, 140, 0),
                        2
                )
        );

        button.repaint();
    }

    
    private void removeHighlight() {

        if (targetButtons == null) {
            return;
        }

        for (JButton button : targetButtons) {

            if (button != null) {

                button.setBackground(
                        UIManager.getColor(
                                "Button.background"
                        )
                );

                button.setBorder(
                        UIManager.getBorder(
                                "Button.border"
                        )
                );

                button.repaint();
            }
        }
    }

    
    private void positionPopup(JButton button) {

        if (button == null) {
            setLocationRelativeTo(parentFrame);
            return;
        }

        try {

            Point buttonLocation
                    = button.getLocationOnScreen();

            int popupX
                    = buttonLocation.x + button.getWidth() + 15;

            int popupY
                    = buttonLocation.y;

            Dimension screenSize
                    = Toolkit.getDefaultToolkit().getScreenSize();

            // Keep popup inside screen
            if (popupX + getWidth() > screenSize.width) {
                popupX
                        = buttonLocation.x - getWidth() - 15;
            }

            if (popupY + getHeight() > screenSize.height) {
                popupY
                        = screenSize.height - getHeight() - 20;
            }

            if (popupY < 20) {
                popupY = 20;
            }

            setLocation(popupX, popupY);

        } catch (IllegalComponentStateException e) {

           
            setLocationRelativeTo(parentFrame);
        }
    }

    private void nextStep() {

        if (currentStep
                == targetButtons.size() - 1) {

            finishTour();

        } else {

            currentStep++;

            showStep();
        }
    }

    private void previousStep() {

        if (currentStep > 0) {

            currentStep--;

            showStep();
        }
    }

    
    private void skipTour() {

        removeHighlight();

        dispose();
    }

    
    private void finishTour() {

        removeHighlight();
        dispose();
    }

    
    @SuppressWarnings("unchecked")
    // <editor-fold defaultstate="collapsed" desc="Generated Code">//GEN-BEGIN:initComponents
    private void initComponents() {

        setDefaultCloseOperation(javax.swing.WindowConstants.DISPOSE_ON_CLOSE);

        javax.swing.GroupLayout layout = new javax.swing.GroupLayout(getContentPane());
        getContentPane().setLayout(layout);
        layout.setHorizontalGroup(
            layout.createParallelGroup(javax.swing.GroupLayout.Alignment.LEADING)
            .addGap(0, 400, Short.MAX_VALUE)
        );
        layout.setVerticalGroup(
            layout.createParallelGroup(javax.swing.GroupLayout.Alignment.LEADING)
            .addGap(0, 300, Short.MAX_VALUE)
        );

        pack();
    }// </editor-fold>//GEN-END:initComponents

    /**
     * @param args the command line arguments
     */

    // Variables declaration - do not modify//GEN-BEGIN:variables
    // End of variables declaration//GEN-END:variables
}
