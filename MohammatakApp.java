import javax.swing.*;
import javax.swing.table.DefaultTableModel;
import java.awt.*;
import java.awt.event.*;
import java.sql.*;
import java.time.LocalDate;
import java.util.HashMap;
import java.util.Map;

public class MohammatakApp extends JFrame {
    private static final String APP_NAME = "مهماتك";
    private static final Font FONT = new Font("Tahoma", Font.PLAIN, 12);
    private static final Font FONT_BOLD = new Font("Tahoma", Font.BOLD, 13);

    private Connection conn;
    private JList<String> navList;
    private DefaultListModel<String> navModel;
    private JTable taskTable;
    private DefaultTableModel tableModel;
    private JTextField searchField;
    private JLabel headingLabel, statusLabel;

    private String currentKind = "view";
    private String currentKey = "all";
    private final java.util.List<NavEntry> navEntries = new java.util.ArrayList<>();

    private static final Map<Integer, String[]> PRIORITIES = new HashMap<>();
    private static final Map<String, Integer> PRIORITY_BY_NAME = new HashMap<>();

    static {
        PRIORITIES.put(1, new String[]{"عاجل", "#d93025"});
        PRIORITIES.put(2, new String[]{"مهم", "#f29900"});
        PRIORITIES.put(3, new String[]{"عادي", "#1a73e8"});
        PRIORITIES.put(4, new String[]{"منخفض", "#5f6368"});

        for (Map.Entry<Integer, String[]> entry : PRIORITIES.entrySet()) {
            PRIORITY_BY_NAME.put(entry.getValue()[0], entry.getKey());
        }
    }

    private static final String[][] VIEWS = {
        {"today", "اليوم"},
        {"upcoming", "القادمة"},
        {"all", "كل المهام"},
        {"done", "المكتملة"}
    };

    public MohammatakApp() {
        initDatabase();
        initUI();
        refresh();
    }

    private void initDatabase() {
        try {
            String dbPath = "mohammatak.db";
            Class.forName("org.sqlite.JDBC");
            conn = DriverManager.getConnection("jdbc:sqlite:" + dbPath);

            try (Statement stmt = conn.createStatement()) {
                stmt.execute("CREATE TABLE IF NOT EXISTS projects (" +
                        "id INTEGER PRIMARY KEY AUTOINCREMENT, " +
                        "name TEXT NOT NULL)");
                
                stmt.execute("CREATE TABLE IF NOT EXISTS tasks (" +
                        "id INTEGER PRIMARY KEY AUTOINCREMENT, " +
                        "title TEXT NOT NULL, " +
                        "notes TEXT DEFAULT '', " +
                        "project_id INTEGER NOT NULL DEFAULT 1, " +
                        "priority INTEGER NOT NULL DEFAULT 4, " +
                        "due_date TEXT, " +
                        "done INTEGER NOT NULL DEFAULT 0, " +
                        "created_at TEXT NOT NULL DEFAULT CURRENT_TIMESTAMP)");
                
                ResultSet rs = stmt.executeQuery("SELECT COUNT(*) FROM projects");
                if (rs.next() && rs.getInt(1) == 0) {
                    stmt.execute("INSERT INTO projects (id, name) VALUES (1, 'الوارد')");
                }
            }
        } catch (Exception e) {
            JOptionPane.showMessageDialog(this, "خطأ في قاعدة البيانات: " + e.getMessage());
        }
    }

    private void initUI() {
        setTitle("✓ " + APP_NAME);
        setSize(900, 560);
        setMinimumSize(new Dimension(720, 420));
        setDefaultCloseOperation(JFrame.EXIT_ON_CLOSE);
        setLocationRelativeTo(null);

        // جعل الواجهة تدعم الاتجاه من اليمين لليسار (RTL) بشكل أساسي
        applyComponentOrientation(ComponentOrientation.RIGHT_TO_LEFT);

        // الشريط الجانبي (القائمة)
        JPanel sidePanel = new JPanel(new BorderLayout());
        sidePanel.setBorder(BorderFactory.createEmptyBorder(8, 8, 8, 8));

        JLabel titleLabel = new JLabel(APP_NAME, JLabel.RIGHT);
        titleLabel.setFont(new Font("Tahoma", Font.BOLD, 16));
        titleLabel.setForeground(new Color(219, 76, 63));
        sidePanel.add(titleLabel, BorderLayout.NORTH);

        navModel = new DefaultListModel<>();
        navList = new JList<>(navModel);
        navList.setFont(FONT);
        navList.setSelectionMode(ListSelectionModel.SINGLE_SELECTION);
        navList.addListSelectionListener(e -> {
            if (!e.getValueIsAdjusting()) {
                onNavSelect();
            }
        });
        sidePanel.add(new JScrollPane(navList), BorderLayout.CENTER);

        JPanel projBtns = new JPanel(new GridLayout(1, 2, 4, 0));
        JButton addProjBtn = new JButton("+ مشروع");
        JButton delProjBtn = new JButton("حذف المشروع");
        addProjBtn.addActionListener(e -> addProject());
        delProjBtn.addActionListener(e -> deleteProject());
        projBtns.add(delProjBtn);
        projBtns.add(addProjBtn);
        sidePanel.add(projBtns, BorderLayout.SOUTH);

        add(sidePanel, BorderLayout.EAST);

        // المحتوى الرئيسي
        JPanel mainPanel = new JPanel(new BorderLayout(0, 8));
        mainPanel.setBorder(BorderFactory.createEmptyBorder(10, 10, 10, 10));

        JPanel topPanel = new JPanel(new BorderLayout());
        headingLabel = new JLabel("", JLabel.RIGHT);
        headingLabel.setFont(new Font("Tahoma", Font.BOLD, 15));
        topPanel.add(headingLabel, BorderLayout.EAST);

        JPanel searchPanel = new JPanel(new FlowLayout(FlowLayout.LEFT));
        searchField = new JTextField(18);
        searchField.addKeyListener(new KeyAdapter() {
            public void keyReleased(KeyEvent e) {
                refreshTasks();
            }
        });
        searchPanel.add(new JLabel("بحث:"));
        searchPanel.add(searchField);
        topPanel.add(searchPanel, BorderLayout.WEST);
        mainPanel.add(topPanel, BorderLayout.NORTH);

        // شريط الأزرار العلوي للعمليات
        JPanel barPanel = new JPanel(new FlowLayout(FlowLayout.RIGHT));
        JButton addTaskBtn = new JButton("مهمة جديدة");
        JButton editTaskBtn = new JButton("تعديل");
        JButton toggleTaskBtn = new JButton("إنجاز / تراجع");
        JButton delTaskBtn = new JButton("حذف");

        addTaskBtn.addActionListener(e -> addTask());
        editTaskBtn.addActionListener(e -> editTask());
        toggleTaskBtn.addActionListener(e -> toggleTask());
        delTaskBtn.addActionListener(e -> deleteTask());

        barPanel.add(delTaskBtn);
        barPanel.add(toggleTaskBtn);
        barPanel.add(editTaskBtn);
        barPanel.add(addTaskBtn);

        JPanel centerContainer = new JPanel(new BorderLayout());
        centerContainer.add(barPanel, BorderLayout.NORTH);

        // جدول المهام
        String[] columns = {"✓", "المهمة", "الأولوية", "الموعد", "المشروع"};
        tableModel = new DefaultTableModel(columns, 0) {
            public boolean isCellEditable(int row, int column) { return false; }
        };
        taskTable = new JTable(tableModel);
        taskTable.setFont(FONT);
        taskTable.setRowHeight(30);
        taskTable.getTableHeader().setFont(FONT_BOLD);
        taskTable.setSelectionMode(ListSelectionModel.SINGLE_SELECTION);
        taskTable.addMouseListener(new MouseAdapter() {
            public void mouseClicked(MouseEvent e) {
                if (e.getClickCount() == 2) editTask();
            }
        });

        // تخصيص أعمدة الجدول
        taskTable.getColumnModel().getColumn(0.preferredWidth = 40).setMaxWidth(50);
        taskTable.getColumnModel().getColumn(1).setPreferredWidth(300);

        centerContainer.add(new JScrollPane(taskTable), BorderLayout.CENTER);
        mainPanel.add(centerContainer, BorderLayout.CENTER);

        statusLabel = new JLabel(" ", JLabel.RIGHT);
        mainPanel.add(statusLabel, BorderLayout.SOUTH);

        add(mainPanel, BorderLayout.CENTER);
    }

    private void refresh() {
        refreshSidebar();
        refreshTasks();
    }

    private void refreshSidebar() {
        navModel.clear();
        navEntries.clear();

        try {
            String todayStr = LocalDate.now().toString();
            // جلب العدادات
            Map<String, Integer> counts = new HashMap<>();
            counts.put("today", getCount("SELECT COUNT(*) FROM tasks WHERE done = 0 AND due_date <= '" + todayStr + "'"));
            counts.put("upcoming", getCount("SELECT COUNT(*) FROM tasks WHERE done = 0 AND due_date > '" + todayStr + "'"));
            counts.put("all", getCount("SELECT COUNT(*) FROM tasks WHERE done = 0"));
            counts.put("done", getCount("SELECT COUNT(*) FROM tasks WHERE done = 1"));

            for (String[] v : VIEWS) {
                String key = v[0];
                String label = v[1] + " (" + counts.get(key) + ")";
                navModel.addElement(label);
                navEntries.add(new NavEntry("view", key));
            }

            Statement stmt = conn.createStatement();
            ResultSet rs = stmt.executeQuery("SELECT * FROM projects ORDER BY id");
            while (rs.next()) {
                int id = rs.getInt("id");
                String name = rs.getString("name");
                navModel.addElement("📁 " + name);
                navEntries.add(new NavEntry("project", String.valueOf(id)));
            }
            rs.close();
            stmt.close();

            // تحديد العنصر النشط
            for (int i = 0; i < navEntries.size(); i++) {
                NavEntry entry = navEntries.get(i);
                if (entry.kind.equals(currentKind) && entry.key.equals(currentKey)) {
                    navList.setSelectedIndex(i);
                    break;
                }
            }
        } catch (Exception e) {
            e.printStackTrace();
        }
    }

    private int getCount(String sql) {
        try (Statement stmt = conn.createStatement(); ResultSet rs = stmt.executeQuery(sql)) {
            if (rs.next()) return rs.getInt(1);
        } catch (Exception e) {
        }
        return 0;
    }

    private void refreshTasks() {
        tableModel.setRowCount(0);
        String todayStr = LocalDate.now().toString();

        StringBuilder sql = new StringBuilder("SELECT t.*, p.name AS project_name FROM tasks t " +
                "JOIN projects p ON p.id = t.project_id WHERE 1=1");
        
        if (currentKind.equals("project")) {
            sql.append(" AND t.project_id = ").append(currentKey);
        } else {
            switch (currentKey) {
                case "today" -> {
                    sql.append(" AND t.done = 0 AND t.due_date <= '").append(todayStr).append("'");
                    headingLabel.setText("اليوم");
                }
                case "upcoming" -> {
                    sql.append(" AND t.done = 0 AND t.due_date > '").append(todayStr).append("'");
                    headingLabel.setText("القادمة");
                }
                case "done" -> {
                    sql.append(" AND t.done = 1");
                    headingLabel.setText("المكتملة");
                }
                default -> {
                    sql.append(" AND t.done = 0");
                    headingLabel.setText("كل المهام");
                }
            }
        }

        if (currentKind.equals("project")) {
            try (Statement stmt = conn.createStatement();
                 ResultSet rs = stmt.executeQuery("SELECT name FROM projects WHERE id = " + currentKey)) {
                if (rs.next()) headingLabel.setText(rs.getString("name"));
            } catch (Exception e) { e.printStackTrace(); }
        }

        String search = searchField.getText().trim();
        if (!search.isEmpty()) {
            sql.append(" AND (t.title LIKE '%").append(search).append("%' OR t.notes LIKE '%").append(search).append("%')");
        }

        sql.append(" ORDER BY t.priority, t.due_date IS NULL, t.due_date, t.id DESC");

        try (Statement stmt = conn.createStatement(); ResultSet rs = stmt.executeQuery(sql.toString())) {
            int count = 0;
            while (rs.next()) {
                count++;
                int id = rs.getInt("id");
                String title = rs.getString("title");
                String dueDate = rs.getString("due_date");
                int priority = rs.getInt("priority");
                int done = rs.getInt("done");
                String projName = rs.getString("project_name");

                boolean overdue = dueDate != null && dueDate.compareTo(todayStr) < 0 && done == 0;
                String dueText = dueDate == null ? "" : (overdue ? dueDate + " (متأخرة)" : dueDate);
                String prioName = PRIORITIES.get(priority)[0];
                String doneSymbol = done == 1 ? "✓" : "○";

                tableModel.addRow(new Object[]{doneSymbol, title, prioName, dueText, projName});
                // يمكن تخزين الـ ID في طريقة مخصصة للجدول إذا أردت استرجاعه بدقة
            }
            statusLabel.setText("عدد المهام المعروضة: " + count);
        } catch (Exception e) {
            e.printStackTrace();
        }
    }

    private void onNavSelect() {
        int idx = navList.getSelectedIndex();
        if (idx >= 0 && idx < navEntries.size()) {
            NavEntry entry = navEntries.get(idx);
            currentKind = entry.kind;
            currentKey = entry.key;
            refreshTasks();
        }
    }

    private void addTask() {
        TaskDialog dlg = new TaskDialog(this, "مهمة جديدة", conn, currentKind.equals("project") ? Integer.parseInt(currentKey) : 1);
        dlg.setVisible(true);
        refresh();
    }

    private void editTask() {
        int row = taskTable.getSelectedRow();
        if (row == -1) {
            JOptionPane.showMessageDialog(this, "الرجاء اختيار مهمة للتعديل.");
            return;
        }
        // للحصول على الـ ID الحقيقي للمهمة بناءً على مكانها في الجدول
        int taskId = getSelectedTaskId(row);
        if (taskId != -1) {
            TaskDialog dlg = new TaskDialog(this, "تعديل مهمة", conn, taskId);
            dlg.setVisible(true);
            refresh();
        }
    }

    private int getSelectedTaskId(int row) {
        // طريقة مبسطة لجلب الـ ID عبر استعلام مطابق للجدول الحالي
        try {
            String todayStr = LocalDate.now().toString();
            StringBuilder sql = new StringBuilder("SELECT t.id FROM tasks t JOIN projects p ON p.id = t.project_id WHERE 1=1");
            if (currentKind.equals("project")) {
                sql.append(" AND t.project_id = ").append(currentKey);
            } else {
                if (currentKey.equals("today")) sql.append(" AND t.done = 0 AND t.due_date <= '").append(todayStr).append("'");
                else if (currentKey.equals("upcoming")) sql.append(" AND t.done = 0 AND t.due_date > '").append(todayStr).append("'");
                else if (currentKey.equals("done")) sql.append(" AND t.done = 1");
                else sql.append(" AND t.done = 0");
            }
            sql.append(" ORDER BY t.priority, t.due_date IS NULL, t.due_date, t.id DESC");
            
            Statement stmt = conn.createStatement();
            ResultSet rs = stmt.executeQuery(sql.toString());
            int idx = 0;
            while (rs.next()) {
                if (idx == row) return rs.getInt("id");
                idx++;
            }
        } catch (Exception e) { e.printStackTrace(); }
        return -1;
    }

    private void toggleTask() {
        int row = taskTable.getSelectedRow();
        if (row != -1) {
            int taskId = getSelectedTaskId(row);
            try (Statement stmt = conn.createStatement()) {
                stmt.execute("UPDATE tasks SET done = NOT done WHERE id = " + taskId);
                refresh();
            } catch (Exception e) { e.printStackTrace(); }
        }
    }

    private void deleteTask() {
        int row = taskTable.getSelectedRow();
        if (row != -1) {
            int confirm = JOptionPane.showConfirmDialog(this, "حذف هذه المهمة نهائياً؟", APP_NAME, JOptionPane.YES_NO_OPTION);
            if (confirm == JOptionPane.YES_OPTION) {
                int taskId = getSelectedTaskId(row);
                try (Statement stmt = conn.createStatement()) {
                    stmt.execute("DELETE FROM tasks WHERE id = " + taskId);
                    refresh();
                } catch (Exception e) { e.printStackTrace(); }
            }
        }
    }

    private void addProject() {
        String name = JOptionPane.showInputDialog(this, "اسم المشروع الجديد:");
        if (name != null && !name.trim().isEmpty()) {
            try (PreparedStatement pstmt = conn.prepareStatement("INSERT INTO projects (name) VALUES (?)")) {
                pstmt.setString(1, name.trim());
                pstmt.executeUpdate();
                refresh();
            } catch (Exception e) { e.printStackTrace(); }
        }
    }

    private void deleteProject() {
        if (!currentKind.equals("project")) {
            JOptionPane.showMessageDialog(this, "اختر مشروعاً من القائمة أولاً.");
        } else if (currentKey.equals("1")) {
            JOptionPane.showMessageDialog(this, "لا يمكن حذف مشروع الوارد الأساسي.");
        } else {
            int confirm = JOptionPane.showConfirmDialog(this, "حذف المشروع؟ ستنتقل مهامه إلى الوارد.", APP_NAME, JOptionPane.YES_NO_OPTION);
            if (confirm == JOptionPane.YES_OPTION) {
                try (Statement stmt = conn.createStatement()) {
                    stmt.execute("UPDATE tasks SET project_id = 1 WHERE project_id = " + currentKey);
                    stmt.execute("DELETE FROM projects WHERE id = " + currentKey);
                    currentKind = "view";
                    currentKey = "all";
                    refresh();
                } catch (Exception e) { e.printStackTrace(); }
            }
        }
    }

    private static class NavEntry {
        String kind, key;
        NavEntry(String kind, String key) { this.kind = kind; this.key = key; }
    }

    // نافذة إضافة/تعديل مهمة فرعية
    private static class TaskDialog extends JDialog {
        JTextField titleField, dateField;
        JTextArea notesArea;
        JComboBox<String> priorityCombo, projectCombo;
        Connection conn;
        int editTaskId = -1;
        int defaultProjId;
        Map<String, Integer> projectMap = new HashMap<>();

        public TaskDialog(JFrame parent, String title, Connection conn, int paramId) {
            super(parent, title, true);
            this.conn = conn;
            setSize(400, 380);
            setLocationRelativeTo(parent);
            setLayout(new GridLayout(6, 2, 8, 8));

            if (title.equals("تعديل مهمة")) {
                this.editTaskId = paramId;
            } else {
                this.defaultProjId = paramId;
            }

            add(new JLabel("عنوان المهمة *", JLabel.RIGHT));
            titleField = new JTextField();
            add(titleField);

            add(new JLabel("ملاحظات", JLabel.RIGHT));
            notesArea = new JTextArea(3, 20);
            add(new JScrollPane(notesArea));

            add(new JLabel("تاريخ الاستحقاق (YYYY-MM-DD)", JLabel.RIGHT));
            dateField = new JTextField();
            add(dateField);

            add(new JLabel("الأولوية", JLabel.RIGHT));
            priorityCombo = new JComboBox<>(new String[]{"عاجل", "مهم", "عادي", "منخفض"});
            priorityCombo.setSelectedItem("منخفض");
            add(priorityCombo);

            add(new JLabel("المشروع", JLabel.RIGHT));
            projectCombo = new JComboBox<>();
            loadProjects();
            add(projectCombo);

            JButton saveBtn = new JButton("حفظ");
            saveBtn.addActionListener(e -> saveTask());
            add(saveBtn);

            if (editTaskId != -1) {
                loadTaskData();
            }
        }

        private void loadProjects() {
            try (Statement stmt = conn.createStatement();
                 ResultSet rs = stmt.executeQuery("SELECT * FROM projects")) {
                while (rs.next()) {
                    int id = rs.getInt("id");
                    String name = rs.getString("name");
                    projectMap.put(name, id);
                    projectCombo.addItem(name);
                    if (editTaskId == -1 && id == defaultProjId) {
                        projectCombo.setSelectedItem(name);
                    }
                }
            } catch (Exception e) { e.printStackTrace(); }
        }

        private void loadTaskData() {
            try (Statement stmt = conn.createStatement();
                 ResultSet rs = stmt.executeQuery("SELECT * FROM tasks WHERE id = " + editTaskId)) {
                if (rs.next()) {
                    titleField.setText(rs.getString("title"));
                    notesArea.setText(rs.getString("notes"));
                    dateField.setText(rs.getString("due_date"));
                    int prio = rs.getInt("priority");
                    priorityCombo.setSelectedItem(PRIORITIES.get(prio)[0]);
                    int projId = rs.getInt("project_id");
                    for (Map.Entry<String, Integer> entry : projectMap.entrySet()) {
                        if (entry.getValue() == projId) {
                            projectCombo.setSelectedItem(entry.getKey());
                            break;
                        }
                    }
                }
            } catch (Exception e) { e.printStackTrace(); }
        }

        private void saveTask() {
            String title = titleField.getText().trim();
            if (title.isEmpty()) {
                JOptionPane.showMessageDialog(this, "الرجاء كتابة عنوان المهمة");
                return;
            }

            String dueDate = dateField.getText().trim();
            if (!dueDate.isEmpty()) {
                try {
                    LocalDate.parse(dueDate);
                } catch (Exception e) {
                    JOptionPane.showMessageDialog(this, "صيغة التاريخ غير صحيحة، مثال: 2026-12-31");
                    return;
                }
            } else {
                dueDate = null;
            }

            String notes = notesArea.getText().trim();
            int priority = PRIORITY_BY_NAME.get((String) priorityCombo.getSelectedItem());
            int projId = projectMap.get((String) projectCombo.getSelectedItem());

            try {
                if (editTaskId == -1) {
                    PreparedStatement pstmt = conn.prepareStatement(
                            "INSERT INTO tasks (title, notes, project_id, priority, due_date) VALUES (?, ?, ?, ?, ?)");
                    pstmt.setString(1, title);
                    pstmt.setString(2, notes);
                    pstmt.setInt(3, projId);
                    pstmt.setInt(4, priority);
                    pstmt.setString(5, dueDate);
                    pstmt.executeUpdate();
                } else {
                    PreparedStatement pstmt = conn.prepareStatement(
                            "UPDATE tasks SET title = ?, notes = ?, project_id = ?, priority = ?, due_date = ? WHERE id = ?");
                    pstmt.setString(1, title);
                    pstmt.setString(2, notes);
                    pstmt.setInt(3, projId);
                    pstmt.setInt(4, priority);
                    pstmt.setString(5, dueDate);
                    pstmt.setInt(6, editTaskId);
                    pstmt.executeUpdate();
                }
                dispose();
            } catch (Exception e) {
                e.printStackTrace();
            }
        }
    }

    public static void main(String[] args) {
        SwingUtilities.invokeLater(() -> new MohammatakApp().setVisible(true));
    }
}
