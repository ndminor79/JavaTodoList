import javax.swing.BorderFactory;
import javax.swing.DefaultListModel;
import javax.swing.JButton;
import javax.swing.JFrame;
import javax.swing.JLabel;
import javax.swing.JList;
import javax.swing.JPanel;
import javax.swing.JScrollPane;
import javax.swing.JTextField;
import javax.swing.ListSelectionModel;
import javax.swing.JOptionPane;
import javax.swing.SwingUtilities;
import javax.swing.UIManager;
import java.awt.BorderLayout;
import java.awt.Dimension;
import java.awt.Font;
import java.awt.GridBagConstraints;
import java.awt.GridBagLayout;
import java.awt.Insets;
import java.awt.event.WindowAdapter;
import java.awt.event.WindowEvent;
import java.io.BufferedReader;
import java.io.BufferedWriter;
import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;

public class Main {
    private static final Path TASKS_FILE = Path.of("tasks.csv");

    private final DefaultListModel<String> taskListModel;
    private final JList<String> taskList;
    private final JTextField taskInput;

    public Main() {
        taskListModel = new DefaultListModel<>();
        taskList = new JList<>(taskListModel);
        taskInput = new JTextField();
    }

    public static void main(String[] args) {
        SwingUtilities.invokeLater(() -> {
            try {
                UIManager.setLookAndFeel(UIManager.getSystemLookAndFeelClassName());
            } catch (Exception ignored) {
                // Use the default Swing look and feel if the system one is unavailable.
            }

            Main app = new Main();
            app.createAndShowUI();
        });
    }

    private void createAndShowUI() {
        JFrame frame = new JFrame("To-Do List");
        frame.setDefaultCloseOperation(JFrame.DO_NOTHING_ON_CLOSE);
        frame.setMinimumSize(new Dimension(420, 360));
        frame.addWindowListener(new WindowAdapter() {
            @Override
            public void windowClosing(WindowEvent event) {
                saveTasksAndExit(frame);
            }
        });

        loadTasks();

        JLabel titleLabel = new JLabel("To-Do List");
        titleLabel.setFont(titleLabel.getFont().deriveFont(Font.BOLD, 24f));

        JButton addButton = new JButton("Add Task");
        JButton deleteButton = new JButton("Delete Selected");

        taskInput.setToolTipText("Enter a task");
        taskList.setSelectionMode(ListSelectionModel.MULTIPLE_INTERVAL_SELECTION);

        addButton.addActionListener(event -> addTask());
        taskInput.addActionListener(event -> addTask());
        deleteButton.addActionListener(event -> deleteSelectedTasks());

        JPanel inputPanel = new JPanel(new BorderLayout(8, 0));
        inputPanel.add(taskInput, BorderLayout.CENTER);
        inputPanel.add(addButton, BorderLayout.EAST);

        JPanel topPanel = new JPanel(new BorderLayout(0, 12));
        topPanel.add(titleLabel, BorderLayout.NORTH);
        topPanel.add(inputPanel, BorderLayout.CENTER);

        JScrollPane scrollPane = new JScrollPane(taskList);

        JPanel buttonPanel = new JPanel(new BorderLayout());
        buttonPanel.add(deleteButton, BorderLayout.EAST);

        JPanel contentPanel = new JPanel(new GridBagLayout());
        contentPanel.setBorder(BorderFactory.createEmptyBorder(18, 18, 18, 18));

        GridBagConstraints constraints = new GridBagConstraints();
        constraints.gridx = 0;
        constraints.gridy = 0;
        constraints.weightx = 1;
        constraints.fill = GridBagConstraints.HORIZONTAL;
        constraints.insets = new Insets(0, 0, 12, 0);
        contentPanel.add(topPanel, constraints);

        constraints.gridy = 1;
        constraints.weighty = 1;
        constraints.fill = GridBagConstraints.BOTH;
        constraints.insets = new Insets(0, 0, 12, 0);
        contentPanel.add(scrollPane, constraints);

        constraints.gridy = 2;
        constraints.weighty = 0;
        constraints.fill = GridBagConstraints.HORIZONTAL;
        constraints.insets = new Insets(0, 0, 0, 0);
        contentPanel.add(buttonPanel, constraints);

        frame.setContentPane(contentPanel);
        frame.setLocationRelativeTo(null);
        frame.setVisible(true);
    }

    private void addTask() {
        String task = taskInput.getText().trim();

        if (!task.isEmpty()) {
            taskListModel.addElement(task);
            taskInput.setText("");
            taskInput.requestFocusInWindow();
        }
    }

    private void deleteSelectedTasks() {
        int[] selectedIndices = taskList.getSelectedIndices();

        for (int i = selectedIndices.length - 1; i >= 0; i--) {
            taskListModel.remove(selectedIndices[i]);
        }
    }

    private void loadTasks() {
        if (!Files.exists(TASKS_FILE)) {
            return;
        }

        try (BufferedReader reader = Files.newBufferedReader(TASKS_FILE)) {
            String line;

            while ((line = reader.readLine()) != null) {
                String task = parseCsvLine(line);

                if (!task.isBlank()) {
                    taskListModel.addElement(task);
                }
            }
        } catch (IOException exception) {
            JOptionPane.showMessageDialog(
                    null,
                    "Could not load saved tasks from " + TASKS_FILE + ".",
                    "Load Error",
                    JOptionPane.ERROR_MESSAGE
            );
        }
    }

    private void saveTasksAndExit(JFrame frame) {
        try {
            saveTasks();
            frame.dispose();
            System.exit(0);
        } catch (IOException exception) {
            JOptionPane.showMessageDialog(
                    frame,
                    "Could not save tasks to " + TASKS_FILE + ".",
                    "Save Error",
                    JOptionPane.ERROR_MESSAGE
            );
        }
    }

    private void saveTasks() throws IOException {
        try (BufferedWriter writer = Files.newBufferedWriter(TASKS_FILE)) {
            for (int i = 0; i < taskListModel.size(); i++) {
                writer.write(toCsvLine(taskListModel.getElementAt(i)));
                writer.newLine();
            }
        }
    }

    private String toCsvLine(String value) {
        String escapedValue = value.replace("\"", "\"\"");
        return "\"" + escapedValue + "\"";
    }

    private String parseCsvLine(String line) {
        if (line.length() >= 2 && line.startsWith("\"") && line.endsWith("\"")) {
            return line.substring(1, line.length() - 1).replace("\"\"", "\"");
        }

        return line;
    }
}
