import javax.swing.*;
import javax.swing.border.*;
import javax.swing.table.*;
import java.awt.*;
import java.awt.event.*;
import java.util.*;
import java.util.List;

/**
 * StudentManagementSystem — A Swing-based GUI application for a Student Management System.
 *
 * <p>Features:
 * <ul>
 *   <li>Add, update, and view student records</li>
 *   <li>Enroll students in courses via dropdown menus</li>
 *   <li>Assign and view grades for enrolled courses</li>
 *   <li>Dynamic interface updates without manual refresh</li>
 *   <li>Comprehensive input validation and error dialogs</li>
 * </ul>
 *
 * <p>How to compile and run:
 * <pre>
 *   javac StudentManagementSystem.java
 *   java  StudentManagementSystem
 * </pre>
 *
 * @author Student
 * @version 1.0
 */
public class StudentManagementSystem extends JFrame {

    // DATA MODEL

    /**
     * Student — holds all data for a single student record.
     */
    static class Student {
        private static int idCounter = 1000;

        private final int    id;
        private       String name;
        private       String email;
        private       String major;
        private       int    age;

        /** Map of courseCode → grade (null means enrolled but not yet graded). */
        private final Map<String, String> enrollments = new LinkedHashMap<>();

        Student(String name, String email, String major, int age) {
            this.id    = ++idCounter;
            this.name  = name;
            this.email = email;
            this.major = major;
            this.age   = age;
        }

        int    getId()    { return id;    }
        String getName()  { return name;  }
        String getEmail() { return email; }
        String getMajor() { return major; }
        int    getAge()   { return age;   }

        void setName (String v) { name  = v; }
        void setEmail(String v) { email = v; }
        void setMajor(String v) { major = v; }
        void setAge  (int    v) { age   = v; }

        Map<String, String> getEnrollments() { return enrollments; }

        /** Returns display name used in combo boxes: "ID – Name". */
        @Override public String toString() { return id + " – " + name; }
    }

    // Application-wide data stores

    /** Master list of students (in insertion order). */
    private final List<Student> students = new ArrayList<>();

    /** Available courses: code → full title. */
    private final Map<String, String> courses = new LinkedHashMap<>();

    /** Valid grade options presented in dropdowns. */
    private static final String[] GRADE_OPTIONS =
            { "", "A+", "A", "A-", "B+", "B", "B-", "C+", "C", "C-", "D", "F" };

    // UI STATE

    // ── Shared colours & fonts ───────────────────────────────────────────────
    private static final Color PRIMARY   = new Color(25,  84,  166);
    private static final Color SECONDARY = new Color(240, 245, 255);
    // private static final Color ACCENT    = new Color(0,   150, 136);
    private static final Color DANGER    = new Color(211,  47,  47);
    private static final Color LIGHT_BG  = new Color(250, 250, 252);
    private static final Font  HEADER_F  = new Font("Arial", Font.BOLD,  14);
    private static final Font  LABEL_F   = new Font("Arial", Font.PLAIN, 13);
    private static final Font  SMALL_F   = new Font("Arial", Font.PLAIN, 12);

    // ── Tab pane ─────────────────────────────────────────────────────────────
    private JTabbedPane tabbedPane;

    // ── Student-list table (View tab) ────────────────────────────────────────
    private DefaultTableModel studentTableModel;
    private JTable studentTable;

    // ── Course-enrolment tab ─────────────────────────────────────────────────
    private JComboBox<String>  courseCombo;
    private DefaultListModel<Student> eligibleListModel;
    private JList<Student>     eligibleList;
    private DefaultTableModel  enrolledTableModel;
    private JTable             enrolledTable;

    // ── Grade-management tab ─────────────────────────────────────────────────
    private JComboBox<Student> gradeStudentCombo;
    private DefaultTableModel  gradeTableModel;
    private JTable             gradeTable;

    // CONSTRUCTOR

    /**
     * Builds and displays the main application window.
     */
    public StudentManagementSystem() {
        setTitle("Student Management System");
        setDefaultCloseOperation(JFrame.EXIT_ON_CLOSE);
        setSize(1050, 680);
        setMinimumSize(new Dimension(900, 580));
        setLocationRelativeTo(null);
        getContentPane().setBackground(LIGHT_BG);

        initialiseCourses();
        initialiseSampleData();
        buildUI();
        setVisible(true);
    }

    // INITIALISATION HELPERS

    /** Populates the courses catalogue with sample entries. */
    private void initialiseCourses() {
        courses.put("CS101", "Introduction to Computer Science");
        courses.put("CS201", "Data Structures & Algorithms");
        courses.put("CS301", "Database Systems");
        courses.put("MATH101", "Calculus I");
        courses.put("MATH201", "Linear Algebra");
        courses.put("ENG101", "Technical Writing");
        courses.put("PHY101", "Physics I");
    }

    /** Adds a handful of demo students so the app is not empty on first launch. */
    private void initialiseSampleData() {
        Student alice = new Student("Alice Johnson", "alice@uni.edu", "Computer Science", 20);
        alice.getEnrollments().put("CS101", "A");
        alice.getEnrollments().put("MATH101", "B+");
        students.add(alice);

        Student bob = new Student("Bob Smith", "bob@uni.edu", "Mathematics", 22);
        bob.getEnrollments().put("MATH101", "A-");
        bob.getEnrollments().put("MATH201", null);
        students.add(bob);

        Student carol = new Student("Carol White", "carol@uni.edu", "Physics", 21);
        carol.getEnrollments().put("PHY101", "B");
        students.add(carol);
    }

    // UI CONSTRUCTION

    /** Assembles the full UI: menu bar + tabbed pane. */
    private void buildUI() {
        setJMenuBar(buildMenuBar());

        JPanel header = buildHeader();
        tabbedPane = buildTabbedPane();

        getContentPane().setLayout(new BorderLayout(0, 0));
        getContentPane().add(header,     BorderLayout.NORTH);
        getContentPane().add(tabbedPane, BorderLayout.CENTER);
    }

    // ── Header strip ─────────────────────────────────────────────────────────

    private JPanel buildHeader() {
        JPanel p = new JPanel(new BorderLayout());
        p.setBackground(PRIMARY);
        p.setBorder(BorderFactory.createEmptyBorder(14, 20, 14, 20));

        JLabel title = new JLabel("Student Management System");
        title.setFont(new Font("Arial", Font.BOLD, 20));
        title.setForeground(Color.WHITE);

        JLabel subtitle = new JLabel("University Administration Portal");
        subtitle.setFont(new Font("Arial", Font.PLAIN, 12));
        subtitle.setForeground(new Color(200, 220, 255));

        JPanel text = new JPanel(new GridLayout(2, 1, 0, 2));
        text.setOpaque(false);
        text.add(title);
        text.add(subtitle);
        p.add(text, BorderLayout.WEST);
        return p;
    }

    // ── Menu bar ─────────────────────────────────────────────────────────────

    /**
     * Builds the application menu bar with File, Students, Courses, and Help menus.
     * Each menu item has an ActionListener that delegates to the appropriate handler.
     *
     * @return the constructed JMenuBar
     */
    private JMenuBar buildMenuBar() {
        JMenuBar bar = new JMenuBar();
        bar.setBackground(new Color(37, 99, 178));
        bar.setBorder(BorderFactory.createEmptyBorder(2, 0, 2, 0));

        // ── File menu ──────────────────────────────────────────────────────
        JMenu fileMenu = styledMenu("File");
        JMenuItem exitItem = styledMenuItem("Exit");
        exitItem.addActionListener(e -> {
            int confirm = JOptionPane.showConfirmDialog(this,
                "Are you sure you want to exit?", "Exit",
                JOptionPane.YES_NO_OPTION);
            if (confirm == JOptionPane.YES_OPTION) System.exit(0);
        });
        fileMenu.add(exitItem);

        // ── Students menu ──────────────────────────────────────────────────
        JMenu studMenu = styledMenu("Students");
        JMenuItem addItem  = styledMenuItem("Add Student");
        JMenuItem updItem  = styledMenuItem("Update Student");
        JMenuItem viewItem = styledMenuItem("View All Students");
        addItem .addActionListener(e -> showAddStudentDialog());
        updItem .addActionListener(e -> showUpdateStudentDialog());
        viewItem.addActionListener(e -> tabbedPane.setSelectedIndex(0));
        studMenu.add(addItem);
        studMenu.add(updItem);
        studMenu.addSeparator();
        studMenu.add(viewItem);

        // ── Courses menu ───────────────────────────────────────────────────
        JMenu courseMenu = styledMenu("Courses");
        JMenuItem enrolItem = styledMenuItem("Enrol Student");
        JMenuItem gradeItem = styledMenuItem("Manage Grades");
        enrolItem.addActionListener(e -> tabbedPane.setSelectedIndex(1));
        gradeItem.addActionListener(e -> tabbedPane.setSelectedIndex(2));
        courseMenu.add(enrolItem);
        courseMenu.add(gradeItem);

        // ── Help menu ──────────────────────────────────────────────────────
        JMenu helpMenu = styledMenu("Help");
        JMenuItem aboutItem = styledMenuItem("About");
        aboutItem.addActionListener(e ->
            JOptionPane.showMessageDialog(this,
                "Student Management System v1.0\n" +
                "Developed using Java Swing\n\n" +
                "Tabs:\n" +
                "  • Students  — add / update / view records\n" +
                "  • Enrolment — enrol students in courses\n" +
                "  • Grades    — assign and review grades",
                "About", JOptionPane.INFORMATION_MESSAGE));
        helpMenu.add(aboutItem);

        bar.add(fileMenu);
        bar.add(studMenu);
        bar.add(courseMenu);
        bar.add(helpMenu);
        return bar;
    }

    private JMenu styledMenu(String text) {
        JMenu m = new JMenu(text);
        m.setForeground(Color.BLACK);
        m.setFont(new Font("Arial", Font.BOLD, 13));
        return m;
    }

    private JMenuItem styledMenuItem(String text) {
        JMenuItem i = new JMenuItem(text);
        i.setFont(LABEL_F);
        // i.setForeground(PRIMARY);
        return i;
    }

    // ── Tabbed pane ───────────────────────────────────────────────────────────

    private JTabbedPane buildTabbedPane() {
        JTabbedPane tp = new JTabbedPane(JTabbedPane.TOP);
        tp.setFont(HEADER_F);
        tp.setBackground(LIGHT_BG);
        tp.addTab("Students",  buildStudentsTab());
        tp.addTab("Enrolment", buildEnrolmentTab());
        tp.addTab("Grades",    buildGradesTab());
        return tp;
    }

    // TAB 1 — STUDENTS

    /**
     * Builds the Students tab containing:
     * <ul>
     *   <li>A toolbar with Add / Update / Delete buttons</li>
     *   <li>A JTable displaying all student records</li>
     * </ul>
     *
     * @return the Students tab panel
     */
    private JPanel buildStudentsTab() {
        JPanel panel = new JPanel(new BorderLayout(0, 8));
        panel.setBackground(LIGHT_BG);
        panel.setBorder(BorderFactory.createEmptyBorder(12, 14, 12, 14));

        // ── Toolbar ────────────────────────────────────────────────────────
        JPanel toolbar = new JPanel(new FlowLayout(FlowLayout.LEFT, 8, 0));
        toolbar.setOpaque(false);

        JButton addBtn    = primaryButton("Add Student");
        JButton updateBtn = secondaryButton("Update Student");
        JButton deleteBtn = dangerButton("Delete Student");

        addBtn   .addActionListener(e -> showAddStudentDialog());
        updateBtn.addActionListener(e -> showUpdateStudentDialog());
        deleteBtn.addActionListener(e -> deleteSelectedStudent());

        toolbar.add(addBtn);
        toolbar.add(updateBtn);
        toolbar.add(deleteBtn);

        // ── Table ──────────────────────────────────────────────────────────
        String[] cols = { "ID", "Name", "Email", "Major", "Age", "Courses Enrolled" };
        studentTableModel = new DefaultTableModel(cols, 0) {
            @Override public boolean isCellEditable(int r, int c) { return false; }
        };
        studentTable = styledTable(studentTableModel);
        studentTable.setSelectionMode(ListSelectionModel.SINGLE_SELECTION);
        // Double-click → view details dialog
        studentTable.addMouseListener(new MouseAdapter() {
            @Override public void mouseClicked(MouseEvent e) {
                if (e.getClickCount() == 2) showStudentDetailsDialog();
            }
        });

        JScrollPane scroll = new JScrollPane(studentTable);
        scroll.setBorder(BorderFactory.createLineBorder(new Color(210, 220, 240)));

        JLabel hint = new JLabel("  Double-click a row to view full student details.");
        hint.setFont(SMALL_F);
        hint.setForeground(new Color(120, 130, 150));

        panel.add(toolbar, BorderLayout.NORTH);
        panel.add(scroll,  BorderLayout.CENTER);
        panel.add(hint,    BorderLayout.SOUTH);

        refreshStudentTable();
        return panel;
    }

    // TAB 2 — ENROLMENT

    /**
     * Builds the Enrolment tab containing:
     * <ul>
     *   <li>Course dropdown and Enrol button</li>
     *   <li>Eligible-students list (students not yet enrolled in that course)</li>
     *   <li>Current-enrolment table for the selected course</li>
     * </ul>
     *
     * @return the Enrolment tab panel
     */
    private JPanel buildEnrolmentTab() {
        JPanel panel = new JPanel(new BorderLayout(12, 0));
        panel.setBackground(LIGHT_BG);
        panel.setBorder(BorderFactory.createEmptyBorder(12, 14, 12, 14));

        // ── LEFT: course selector + eligible students ──────────────────────
        JPanel left = new JPanel(new BorderLayout(0, 10));
        left.setOpaque(false);
        left.setPreferredSize(new Dimension(300, 0));

        // Course combo
        JPanel courseRow = new JPanel(new BorderLayout(8, 0));
        courseRow.setOpaque(false);
        JLabel courseLbl = new JLabel("Select Course:");
        courseLbl.setFont(LABEL_F);
        courseCombo = new JComboBox<>(courses.entrySet().stream()
                .map(e -> e.getKey() + " – " + e.getValue())
                .toArray(String[]::new));
        courseCombo.setFont(SMALL_F);
        courseCombo.addActionListener(e -> refreshEligibleList());
        courseRow.add(courseLbl,  BorderLayout.NORTH);
        courseRow.add(courseCombo, BorderLayout.CENTER);

        // Eligible list
        eligibleListModel = new DefaultListModel<>();
        eligibleList = new JList<>(eligibleListModel);
        eligibleList.setFont(SMALL_F);
        eligibleList.setSelectionMode(ListSelectionModel.SINGLE_SELECTION);
        eligibleList.setCellRenderer(new StudentCellRenderer());
        JScrollPane eligScroll = new JScrollPane(eligibleList);
        eligScroll.setBorder(titledBorder("Eligible Students (not yet enrolled)"));
        eligScroll.setPreferredSize(new Dimension(0, 200));

        // Enrol button
        JButton enrolBtn = primaryButton("Enrol Selected Student");
        enrolBtn.addActionListener(e -> enrolSelectedStudent());

        left.add(courseRow, BorderLayout.NORTH);
        left.add(eligScroll, BorderLayout.CENTER);
        left.add(enrolBtn, BorderLayout.SOUTH);

        // ── RIGHT: enrolled students for chosen course ─────────────────────
        String[] enrolCols = { "Student ID", "Student Name", "Grade" };
        enrolledTableModel = new DefaultTableModel(enrolCols, 0) {
            @Override public boolean isCellEditable(int r, int c) { return false; }
        };
        enrolledTable = styledTable(enrolledTableModel);
        JScrollPane enrolScroll = new JScrollPane(enrolledTable);
        enrolScroll.setBorder(titledBorder("Currently Enrolled Students"));

        panel.add(left,        BorderLayout.WEST);
        panel.add(enrolScroll, BorderLayout.CENTER);

        refreshEligibleList();
        return panel;
    }

    // TAB 3 — GRADES

    /**
     * Builds the Grades tab containing:
     * <ul>
     *   <li>Student dropdown to select who to grade</li>
     *   <li>Table of that student's enrolled courses and current grades</li>
     *   <li>Inline grade assignment controls</li>
     * </ul>
     *
     * @return the Grades tab panel
     */
    private JPanel buildGradesTab() {
        JPanel panel = new JPanel(new BorderLayout(0, 12));
        panel.setBackground(LIGHT_BG);
        panel.setBorder(BorderFactory.createEmptyBorder(12, 14, 12, 14));

        // ── Student selector row ───────────────────────────────────────────
        JPanel topRow = new JPanel(new FlowLayout(FlowLayout.LEFT, 10, 0));
        topRow.setOpaque(false);

        JLabel lbl = new JLabel("Select Student:");
        lbl.setFont(LABEL_F);
        gradeStudentCombo = new JComboBox<>();
        gradeStudentCombo.setFont(SMALL_F);
        gradeStudentCombo.setPreferredSize(new Dimension(280, 30));
        gradeStudentCombo.addActionListener(e -> refreshGradeTable());

        topRow.add(lbl);
        topRow.add(gradeStudentCombo);

        // ── Grade table ────────────────────────────────────────────────────
        String[] gradeCols = { "Course Code", "Course Title", "Current Grade" };
        gradeTableModel = new DefaultTableModel(gradeCols, 0) {
            @Override public boolean isCellEditable(int r, int c) { return false; }
        };
        gradeTable = styledTable(gradeTableModel);
        gradeTable.setSelectionMode(ListSelectionModel.SINGLE_SELECTION);
        JScrollPane gradeScroll = new JScrollPane(gradeTable);
        gradeScroll.setBorder(titledBorder("Enrolled Courses & Grades"));

        // ── Assign grade row ───────────────────────────────────────────────
        JPanel assignRow = new JPanel(new FlowLayout(FlowLayout.LEFT, 10, 0));
        assignRow.setOpaque(false);

        JLabel assignLbl = new JLabel("Assign Grade:");
        assignLbl.setFont(LABEL_F);
        JComboBox<String> gradeCombo = new JComboBox<>(GRADE_OPTIONS);
        gradeCombo.setFont(SMALL_F);
        gradeCombo.setPreferredSize(new Dimension(100, 30));

        JButton assignBtn = primaryButton("Save Grade");
        assignBtn.addActionListener(e -> assignGrade(gradeCombo));

        assignRow.add(assignLbl);
        assignRow.add(gradeCombo);
        assignRow.add(assignBtn);

        panel.add(topRow,    BorderLayout.NORTH);
        panel.add(gradeScroll, BorderLayout.CENTER);
        panel.add(assignRow, BorderLayout.SOUTH);

        refreshGradeStudentCombo();
        return panel;
    }

    // DIALOGS — ADD STUDENT

    /**
     * Displays a modal dialog for adding a new student.
     * Validates all inputs before creating the Student object.
     * On success, refreshes the student table and grade combo box.
     */
    private void showAddStudentDialog() {
        JDialog dlg = createDialog("Add New Student", 420, 340);

        JPanel form = new JPanel(new GridBagLayout());
        form.setBackground(PRIMARY);
        form.setBorder(BorderFactory.createEmptyBorder(16, 20, 16, 20));

        JTextField nameField  = new JTextField(20);
        JTextField emailField = new JTextField(20);
        JTextField majorField = new JTextField(20);
        JTextField ageField   = new JTextField(5);

        addFormRow(form, "Full Name:",  nameField,  0);
        addFormRow(form, "Email:",      emailField, 1);
        addFormRow(form, "Major:",      majorField, 2);
        addFormRow(form, "Age:",        ageField,   3);

        JButton saveBtn   = primaryButton("Add Student");
        JButton cancelBtn = secondaryButton("Cancel");
        cancelBtn.addActionListener(e -> dlg.dispose());

        saveBtn.addActionListener(e -> {
            // ── Validation ──────────────────────────────────────────────────
            String name  = nameField.getText().trim();
            String email = emailField.getText().trim();
            String major = majorField.getText().trim();
            String ageStr = ageField.getText().trim();

            if (name.isEmpty() || email.isEmpty() || major.isEmpty() || ageStr.isEmpty()) {
                showError(dlg, "All fields are required.");
                return;
            }
            if (!email.matches("^[\\w.+\\-]+@[\\w\\-]+\\.[a-zA-Z]{2,}$")) {
                showError(dlg, "Please enter a valid email address.");
                return;
            }
            int age;
            try {
                age = Integer.parseInt(ageStr);
                if (age < 16 || age > 100) throw new NumberFormatException();
            } catch (NumberFormatException ex) {
                showError(dlg, "Age must be a whole number between 16 and 100.");
                return;
            }

            students.add(new Student(name, email, major, age));
            refreshStudentTable();
            refreshGradeStudentCombo();
            refreshEligibleList();
            dlg.dispose();
            JOptionPane.showMessageDialog(this,
                "Student '" + name + "' added successfully.",
                "Success", JOptionPane.INFORMATION_MESSAGE);
        });

        JPanel btnRow = buttonRow(saveBtn, cancelBtn);
        addButtonRow(form, btnRow, 4);

        dlg.add(form);
        dlg.setVisible(true);
    }

    // DIALOGS — UPDATE STUDENT

    /**
     * Displays a modal dialog for selecting and updating a student's information.
     * Changes are applied immediately and all dependent panels are refreshed.
     */
    private void showUpdateStudentDialog() {
        if (students.isEmpty()) {
            showError(this, "No students available to update.");
            return;
        }

        JDialog dlg = createDialog("Update Student", 440, 380);
        JPanel form = new JPanel(new GridBagLayout());
        form.setBackground(LIGHT_BG);
        form.setBorder(BorderFactory.createEmptyBorder(16, 20, 16, 20));

        // Student selector
        JComboBox<Student> selectCombo = new JComboBox<>(students.toArray(new Student[0]));
        selectCombo.setFont(SMALL_F);
        addFormRow(form, "Select Student:", selectCombo, 0);

        JTextField nameField  = new JTextField(20);
        JTextField emailField = new JTextField(20);
        JTextField majorField = new JTextField(20);
        JTextField ageField   = new JTextField(5);
        addFormRow(form, "Full Name:",  nameField,  1);
        addFormRow(form, "Email:",      emailField, 2);
        addFormRow(form, "Major:",      majorField, 3);
        addFormRow(form, "Age:",        ageField,   4);

        // Populate fields when combo selection changes
        ActionListener populate = e -> {
            Student s = (Student) selectCombo.getSelectedItem();
            if (s != null) {
                nameField .setText(s.getName());
                emailField.setText(s.getEmail());
                majorField.setText(s.getMajor());
                ageField  .setText(String.valueOf(s.getAge()));
            }
        };
        selectCombo.addActionListener(populate);
        populate.actionPerformed(null); // seed with first student

        JButton saveBtn   = primaryButton("Save Changes");
        JButton cancelBtn = secondaryButton("Cancel");
        cancelBtn.addActionListener(e -> dlg.dispose());

        saveBtn.addActionListener(e -> {
            Student s = (Student) selectCombo.getSelectedItem();
            if (s == null) return;

            String name   = nameField.getText().trim();
            String email  = emailField.getText().trim();
            String major  = majorField.getText().trim();
            String ageStr = ageField.getText().trim();

            if (name.isEmpty() || email.isEmpty() || major.isEmpty() || ageStr.isEmpty()) {
                showError(dlg, "All fields are required.");
                return;
            }
            if (!email.matches("^[\\w.+\\-]+@[\\w\\-]+\\.[a-zA-Z]{2,}$")) {
                showError(dlg, "Please enter a valid email address.");
                return;
            }
            int age;
            try {
                age = Integer.parseInt(ageStr);
                if (age < 16 || age > 100) throw new NumberFormatException();
            } catch (NumberFormatException ex) {
                showError(dlg, "Age must be a whole number between 16 and 100.");
                return;
            }

            s.setName(name);
            s.setEmail(email);
            s.setMajor(major);
            s.setAge(age);

            refreshStudentTable();
            refreshGradeStudentCombo();
            dlg.dispose();
            JOptionPane.showMessageDialog(this,
                "Student record updated successfully.", "Success",
                JOptionPane.INFORMATION_MESSAGE);
        });

        addButtonRow(form, buttonRow(saveBtn, cancelBtn), 5);
        dlg.add(form);
        dlg.setVisible(true);
    }

    // DIALOGS — VIEW STUDENT DETAILS

    /**
     * Opens a read-only detail dialog for whichever row is selected in the table.
     */
    private void showStudentDetailsDialog() {
        int row = studentTable.getSelectedRow();
        if (row < 0) return;
        int id = (int) studentTableModel.getValueAt(row, 0);
        Student s = students.stream().filter(x -> x.getId() == id).findFirst().orElse(null);
        if (s == null) return;

        StringBuilder sb = new StringBuilder();
        sb.append("ID     : ").append(s.getId()).append("\n");
        sb.append("Name   : ").append(s.getName()).append("\n");
        sb.append("Email  : ").append(s.getEmail()).append("\n");
        sb.append("Major  : ").append(s.getMajor()).append("\n");
        sb.append("Age    : ").append(s.getAge()).append("\n\n");
        sb.append("Enrolments:\n");
        if (s.getEnrollments().isEmpty()) {
            sb.append("  (none)\n");
        } else {
            s.getEnrollments().forEach((code, grade) ->
                sb.append("  ").append(code).append("  →  ")
                  .append(grade == null || grade.isEmpty() ? "Not graded" : grade).append("\n"));
        }

        JTextArea area = new JTextArea(sb.toString());
        area.setFont(new Font("Courier New", Font.PLAIN, 13));
        area.setEditable(false);
        area.setBackground(PRIMARY);
        area.setBorder(BorderFactory.createEmptyBorder(10, 14, 10, 14));

        JOptionPane.showMessageDialog(this, new JScrollPane(area),
            "Student Details — " + s.getName(), JOptionPane.PLAIN_MESSAGE);
    }

    // ACTIONS — DELETE STUDENT

    /** Deletes the currently selected student after confirming with the user. */
    private void deleteSelectedStudent() {
        int row = studentTable.getSelectedRow();
        if (row < 0) {
            showError(this, "Please select a student to delete.");
            return;
        }
        int id = (int) studentTableModel.getValueAt(row, 0);
        String name = (String) studentTableModel.getValueAt(row, 1);

        int confirm = JOptionPane.showConfirmDialog(this,
            "Delete student '" + name + "' (ID " + id + ")?\nThis cannot be undone.",
            "Confirm Delete", JOptionPane.YES_NO_OPTION, JOptionPane.WARNING_MESSAGE);
        if (confirm != JOptionPane.YES_OPTION) return;

        students.removeIf(s -> s.getId() == id);
        refreshStudentTable();
        refreshGradeStudentCombo();
        refreshEligibleList();
    }

    // ACTIONS — ENROLMENT

    /**
     * Enrols the student selected in the eligible-students list into the
     * course selected in the course combo box. Refreshes both panels on success.
     */
    private void enrolSelectedStudent() {
        Student s = eligibleList.getSelectedValue();
        if (s == null) {
            showError(this, "Please select a student from the eligible list.");
            return;
        }
        String courseEntry = (String) courseCombo.getSelectedItem();
        if (courseEntry == null) return;
        String code = courseEntry.split(" – ")[0].trim();

        s.getEnrollments().put(code, null); // enrolled, no grade yet
        refreshEligibleList();
        refreshStudentTable();
        refreshGradeStudentCombo();
        refreshGradeTable();

        JOptionPane.showMessageDialog(this,
            s.getName() + " successfully enrolled in " + code + ".",
            "Enrolled", JOptionPane.INFORMATION_MESSAGE);
    }

    // ACTIONS — GRADE ASSIGNMENT

    /**
     * Saves the selected grade to the selected course row in the grade table.
     *
     * @param gradeCombo the combo box containing the grade to assign
     */
    private void assignGrade(JComboBox<String> gradeCombo) {
        Student s = (Student) gradeStudentCombo.getSelectedItem();
        if (s == null) { showError(this, "Please select a student."); return; }

        int row = gradeTable.getSelectedRow();
        if (row < 0) { showError(this, "Please select a course row."); return; }

        String grade = (String) gradeCombo.getSelectedItem();
        if (grade == null || grade.isEmpty()) {
            showError(this, "Please select a grade from the dropdown.");
            return;
        }

        String code = (String) gradeTableModel.getValueAt(row, 0);
        s.getEnrollments().put(code, grade);
        refreshGradeTable();
        refreshStudentTable();
        refreshEnrolledTable();
    }

    // REFRESH HELPERS  (Dynamic Interface Updates)

    /**
     * Rebuilds the Students table from the current students list.
     * Called after any add, update, or delete operation.
     */
    private void refreshStudentTable() {
        studentTableModel.setRowCount(0);
        for (Student s : students) {
            studentTableModel.addRow(new Object[]{
                s.getId(), s.getName(), s.getEmail(), s.getMajor(), s.getAge(),
                s.getEnrollments().size()
            });
        }
    }

    /**
     * Refreshes the eligible-students list for the currently selected course.
     * A student is eligible if they are not already enrolled in that course.
     */
    private void refreshEligibleList() {
        String courseEntry = (String) courseCombo.getSelectedItem();
        String code = courseEntry != null ? courseEntry.split(" – ")[0].trim() : "";

        eligibleListModel.clear();
        for (Student s : students) {
            if (!s.getEnrollments().containsKey(code)) {
                eligibleListModel.addElement(s);
            }
        }
        refreshEnrolledTable();
    }

    /**
     * Rebuilds the enrolled-students table for the currently selected course.
     */
    private void refreshEnrolledTable() {
        enrolledTableModel.setRowCount(0);
        String courseEntry = (String) courseCombo.getSelectedItem();
        if (courseEntry == null) return;
        String code = courseEntry.split(" – ")[0].trim();

        for (Student s : students) {
            if (s.getEnrollments().containsKey(code)) {
                String grade = s.getEnrollments().get(code);
                enrolledTableModel.addRow(new Object[]{
                    s.getId(), s.getName(),
                    (grade == null || grade.isEmpty()) ? "—" : grade
                });
            }
        }
    }

    /**
     * Rebuilds the grade-student combo box from the current students list.
     * Preserves the previously selected student if they still exist.
     */
    private void refreshGradeStudentCombo() {
        Student prev = (Student) gradeStudentCombo.getSelectedItem();
        gradeStudentCombo.removeAllItems();
        for (Student s : students) gradeStudentCombo.addItem(s);
        if (prev != null && students.contains(prev)) {
            gradeStudentCombo.setSelectedItem(prev);
        }
        refreshGradeTable();
    }

    /**
     * Rebuilds the grade table for the student currently selected in the grade combo.
     */
    private void refreshGradeTable() {
        gradeTableModel.setRowCount(0);
        Student s = (Student) gradeStudentCombo.getSelectedItem();
        if (s == null) return;
        s.getEnrollments().forEach((code, grade) -> {
            String title = courses.getOrDefault(code, "Unknown Course");
            gradeTableModel.addRow(new Object[]{
                code, title, (grade == null || grade.isEmpty()) ? "—" : grade
            });
        });
    }

    // UI FACTORY HELPERS

    private JButton primaryButton(String text) {
    JButton b = new JButton(text);
        b.setBackground(PRIMARY);
        b.setForeground(Color.WHITE);
        b.setFont(new Font("Arial", Font.BOLD, 12));
        b.setFocusPainted(false);
        b.setContentAreaFilled(true);
        b.setBorderPainted(true);
        b.setCursor(Cursor.getPredefinedCursor(Cursor.HAND_CURSOR));
        return b;
    }

    private JButton secondaryButton(String text) {
        JButton b = new JButton(text);
        Color bg = new Color(200, 212, 230);
        b.setBackground(bg);
        b.setForeground(PRIMARY);
        b.setFont(new Font("Arial", Font.BOLD, 12));
        b.setFocusPainted(false);
        b.setContentAreaFilled(true);
        b.setBorderPainted(true);
        b.setCursor(Cursor.getPredefinedCursor(Cursor.HAND_CURSOR));
        return b;
    }

    private JButton dangerButton(String text) {
        JButton b = new JButton(text);
        b.setBackground(DANGER);
        b.setForeground(Color.WHITE);
        b.setFont(new Font("Arial", Font.BOLD, 12));
        b.setFocusPainted(false);
        b.setContentAreaFilled(true);
        b.setBorderPainted(true);
        b.setCursor(Cursor.getPredefinedCursor(Cursor.HAND_CURSOR));
        return b;
    }

    private JTable styledTable(DefaultTableModel model) {
        JTable t = new JTable(model);
        t.setFont(SMALL_F);
        t.setRowHeight(26);
        t.getTableHeader().setFont(new Font("Arial", Font.BOLD, 13));
        t.getTableHeader().setBackground(PRIMARY);
        // t.getTableHeader().setForeground(Color.WHITE);
        t.setGridColor(new Color(220, 228, 240));
        t.setShowGrid(true);
        t.setIntercellSpacing(new Dimension(1, 1));
        t.setFillsViewportHeight(true);
        // Alternating row colours
        t.setDefaultRenderer(Object.class, new DefaultTableCellRenderer() {
            @Override
            public Component getTableCellRendererComponent(
                    JTable table, Object value, boolean isSelected,
                    boolean hasFocus, int row, int col) {
                super.getTableCellRendererComponent(table, value, isSelected, hasFocus, row, col);
                if (!isSelected) {
                    setBackground(row % 2 == 0 ? Color.WHITE : SECONDARY);
                }
                setBorder(BorderFactory.createEmptyBorder(0, 8, 0, 8));
                return this;
            }
        });
        return t;
    }

    private TitledBorder titledBorder(String title) {
        return BorderFactory.createTitledBorder(
            BorderFactory.createLineBorder(new Color(250, 205, 230)),
            title,
            TitledBorder.LEFT, TitledBorder.TOP,
            new Font("Arial", Font.BOLD, 12),
            PRIMARY
        );
    }

    private JDialog createDialog(String title, int w, int h) {
        JDialog dlg = new JDialog(this, title, true);
        dlg.setSize(w, h);
        dlg.setResizable(false);
        dlg.setLocationRelativeTo(this);
        dlg.getContentPane().setBackground(LIGHT_BG);
        dlg.setLayout(new BorderLayout());
        return dlg;
    }

    private void addFormRow(JPanel panel, String labelText, JComponent field, int row) {
        GridBagConstraints lc = new GridBagConstraints();
        lc.gridx = 0; lc.gridy = row;
        lc.anchor = GridBagConstraints.WEST;
        lc.insets = new Insets(6, 0, 6, 12);
        JLabel lbl = new JLabel(labelText);
        lbl.setFont(LABEL_F);
        panel.add(lbl, lc);

        GridBagConstraints fc = new GridBagConstraints();
        fc.gridx = 1; fc.gridy = row;
        fc.fill = GridBagConstraints.HORIZONTAL;
        fc.weightx = 1.0;
        fc.insets = new Insets(6, 0, 6, 0);
        panel.add(field, fc);
    }

    private void addButtonRow(JPanel panel, JPanel btnRow, int row) {
        GridBagConstraints c = new GridBagConstraints();
        c.gridx = 0; c.gridy = row; c.gridwidth = 2;
        c.fill = GridBagConstraints.HORIZONTAL;
        c.insets = new Insets(16, 0, 0, 0);
        panel.add(btnRow, c);
    }

    private JPanel buttonRow(JButton... buttons) {
        JPanel p = new JPanel(new FlowLayout(FlowLayout.RIGHT, 8, 0));
        p.setOpaque(false);
        for (JButton b : buttons) p.add(b);
        return p;
    }

    private void showError(Component parent, String message) {
        JOptionPane.showMessageDialog(parent, message, "Error", JOptionPane.ERROR_MESSAGE);
    }

    // CUSTOM CELL RENDERER

    /**
     * Renders Student objects in the eligible-students JList with
     * alternating row colours for readability.
     */
    static class StudentCellRenderer extends DefaultListCellRenderer {
        private static final Color ODD  = Color.WHITE;
        private static final Color EVEN = new Color(240, 245, 255);

        @Override
        public Component getListCellRendererComponent(
                JList<?> list, Object value, int index,
                boolean isSelected, boolean cellHasFocus) {
            super.getListCellRendererComponent(list, value, index, isSelected, cellHasFocus);
            if (!isSelected) setBackground(index % 2 == 0 ? ODD : EVEN);
            setBorder(BorderFactory.createEmptyBorder(4, 8, 4, 8));
            setFont(new Font("Arial", Font.PLAIN, 12));
            return this;
        }
    }

    // ENTRY POINT

    /**
     * Application entry point.
     * Uses SwingUtilities.invokeLater to ensure the UI is created on the
     * Event Dispatch Thread, as required by Swing threading rules.
     *
     * @param args command-line arguments (not used)
     */
    public static void main(String[] args) {
        try {
            // Nimbus respects custom background colors and looks modern
            UIManager.setLookAndFeel("javax.swing.plaf.nimbus.NimbusLookAndFeel");
        } catch (Exception e) {
            // Fallback to system L&F if Nimbus is unavailable
            try {
                UIManager.setLookAndFeel(UIManager.getSystemLookAndFeelClassName());
            } catch (Exception ex) { /* ignore */ }
        }
        
        SwingUtilities.invokeLater(StudentManagementSystem::new);
    }
}