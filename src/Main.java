import javax.swing.BorderFactory;
import javax.swing.DefaultListModel;
import javax.swing.JButton;
import javax.swing.JFrame;
import javax.swing.JLabel;
import javax.swing.JList;
import javax.swing.JOptionPane;
import javax.swing.JPanel;
import javax.swing.JScrollPane;
import javax.swing.JTextField;
import javax.swing.ListSelectionModel;
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
import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

public class Main {
    private static final Path TODO_LISTS_FILE = Path.of("todo-lists.csv");
    private static final Path OLD_TASKS_FILE = Path.of("tasks.csv");
    private static final String DEFAULT_LIST_NAME = "Default";

    private final Map<String, List<String>> todoLists;
    private final DefaultListModel<String> todoListNameModel;
    private final DefaultListModel<String> taskListModel;
    private final JList<String> todoListNames;
    private final JList<String> taskList;
    private final JTextField taskInput;
    private final JLabel currentListLabel;
    private final JButton addTaskButton;
    private final JButton deleteTaskButton;
    private String currentListName;
    private boolean changingListSelection;

    public Main() {
        todoLists = new LinkedHashMap<>();
        todoListNameModel = new DefaultListModel<>();
        taskListModel = new DefaultListModel<>();
        todoListNames = new JList<>(todoListNameModel);
        taskList = new JList<>(taskListModel);
        taskInput = new JTextField();
        currentListLabel = new JLabel("To-Do List");
        addTaskButton = new JButton("Add Task");
        deleteTaskButton = new JButton("Delete Selected");
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
        loadTodoLists();

        JFrame frame = new JFrame("To-Do Lists");
        frame.setDefaultCloseOperation(JFrame.DO_NOTHING_ON_CLOSE);
        frame.setMinimumSize(new Dimension(760, 420));
        frame.addWindowListener(new WindowAdapter() {
            @Override
            public void windowClosing(WindowEvent event) {
                saveTodoListsAndExit(frame);
            }
        });

        currentListLabel.setFont(currentListLabel.getFont().deriveFont(Font.BOLD, 24f));

        JButton addTodoListButton = new JButton("New List");
        JButton deleteTodoListButton = new JButton("Delete List");

        todoListNames.setSelectionMode(ListSelectionModel.SINGLE_SELECTION);
        taskList.setSelectionMode(ListSelectionModel.MULTIPLE_INTERVAL_SELECTION);
        taskInput.setToolTipText("Enter a task");

        addTodoListButton.addActionListener(event -> addTodoList(frame));
        deleteTodoListButton.addActionListener(event -> deleteSelectedTodoList(frame));
        todoListNames.addListSelectionListener(event -> {
            if (!event.getValueIsAdjusting() && !changingListSelection) {
                switchTodoList(todoListNames.getSelectedValue());
            }
        });

        addTaskButton.addActionListener(event -> addTask());
        taskInput.addActionListener(event -> addTask());
        deleteTaskButton.addActionListener(event -> deleteSelectedTasks());

        JPanel mainPanel = new JPanel(new BorderLayout(16, 0));
        mainPanel.setBorder(BorderFactory.createEmptyBorder(18, 18, 18, 18));
        mainPanel.add(createTodoListsPanel(addTodoListButton, deleteTodoListButton), BorderLayout.WEST);
        mainPanel.add(createTasksPanel(), BorderLayout.CENTER);

        frame.setContentPane(mainPanel);
        frame.setLocationRelativeTo(null);
        frame.setVisible(true);

        if (!todoListNameModel.isEmpty()) {
            todoListNames.setSelectedIndex(0);
        } else {
            updateTaskControls();
        }
    }

    private JPanel createTodoListsPanel(JButton addTodoListButton, JButton deleteTodoListButton) {
        JLabel listLabel = new JLabel("Lists");
        listLabel.setFont(listLabel.getFont().deriveFont(Font.BOLD, 18f));

        JPanel buttonPanel = new JPanel(new GridBagLayout());
        GridBagConstraints buttonConstraints = new GridBagConstraints();
        buttonConstraints.gridx = 0;
        buttonConstraints.gridy = 0;
        buttonConstraints.weightx = 1;
        buttonConstraints.fill = GridBagConstraints.HORIZONTAL;
        buttonConstraints.insets = new Insets(0, 0, 8, 0);
        buttonPanel.add(addTodoListButton, buttonConstraints);

        buttonConstraints.gridy = 1;
        buttonConstraints.insets = new Insets(0, 0, 0, 0);
        buttonPanel.add(deleteTodoListButton, buttonConstraints);

        JPanel panel = new JPanel(new BorderLayout(0, 10));
        panel.setPreferredSize(new Dimension(190, 0));
        panel.add(listLabel, BorderLayout.NORTH);
        panel.add(new JScrollPane(todoListNames), BorderLayout.CENTER);
        panel.add(buttonPanel, BorderLayout.SOUTH);
        return panel;
    }

    private JPanel createTasksPanel() {
        JPanel inputPanel = new JPanel(new BorderLayout(8, 0));
        inputPanel.add(taskInput, BorderLayout.CENTER);
        inputPanel.add(addTaskButton, BorderLayout.EAST);

        JPanel topPanel = new JPanel(new BorderLayout(0, 12));
        topPanel.add(currentListLabel, BorderLayout.NORTH);
        topPanel.add(inputPanel, BorderLayout.CENTER);

        JPanel buttonPanel = new JPanel(new BorderLayout());
        buttonPanel.add(deleteTaskButton, BorderLayout.EAST);

        JPanel panel = new JPanel(new GridBagLayout());
        GridBagConstraints constraints = new GridBagConstraints();
        constraints.gridx = 0;
        constraints.gridy = 0;
        constraints.weightx = 1;
        constraints.fill = GridBagConstraints.HORIZONTAL;
        constraints.insets = new Insets(0, 0, 12, 0);
        panel.add(topPanel, constraints);

        constraints.gridy = 1;
        constraints.weighty = 1;
        constraints.fill = GridBagConstraints.BOTH;
        panel.add(new JScrollPane(taskList), constraints);

        constraints.gridy = 2;
        constraints.weighty = 0;
        constraints.fill = GridBagConstraints.HORIZONTAL;
        constraints.insets = new Insets(0, 0, 0, 0);
        panel.add(buttonPanel, constraints);
        return panel;
    }

    private void addTodoList(JFrame frame) {
        String name = JOptionPane.showInputDialog(frame, "List name:", "New To-Do List", JOptionPane.PLAIN_MESSAGE);

        if (name == null) {
            return;
        }

        name = name.trim();
        if (name.isEmpty()) {
            return;
        }

        if (todoLists.containsKey(name)) {
            JOptionPane.showMessageDialog(frame, "A list with that name already exists.", "Duplicate List", JOptionPane.WARNING_MESSAGE);
            return;
        }

        saveCurrentTasksToMemory();
        todoLists.put(name, new ArrayList<>());
        todoListNameModel.addElement(name);
        todoListNames.setSelectedValue(name, true);
    }

    private void deleteSelectedTodoList(JFrame frame) {
        String selectedList = todoListNames.getSelectedValue();

        if (selectedList == null) {
            return;
        }

        int result = JOptionPane.showConfirmDialog(
                frame,
                "Delete \"" + selectedList + "\" and all of its tasks?",
                "Delete To-Do List",
                JOptionPane.YES_NO_OPTION
        );

        if (result != JOptionPane.YES_OPTION) {
            return;
        }

        changingListSelection = true;
        int selectedIndex = todoListNames.getSelectedIndex();
        todoLists.remove(selectedList);
        todoListNameModel.removeElement(selectedList);
        currentListName = null;
        taskListModel.clear();
        changingListSelection = false;

        if (!todoListNameModel.isEmpty()) {
            int nextIndex = Math.min(selectedIndex, todoListNameModel.size() - 1);
            todoListNames.setSelectedIndex(nextIndex);
        } else {
            updateTaskControls();
        }
    }

    private void switchTodoList(String nextListName) {
        if (nextListName == null || nextListName.equals(currentListName)) {
            return;
        }

        saveCurrentTasksToMemory();
        currentListName = nextListName;
        loadCurrentTasksIntoEditor();
        updateTaskControls();
    }

    private void loadCurrentTasksIntoEditor() {
        taskListModel.clear();

        for (String task : todoLists.getOrDefault(currentListName, new ArrayList<>())) {
            taskListModel.addElement(task);
        }

        currentListLabel.setText(currentListName);
    }

    private void addTask() {
        String task = taskInput.getText().trim();

        if (!task.isEmpty() && currentListName != null) {
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

    private void saveCurrentTasksToMemory() {
        if (currentListName == null) {
            return;
        }

        List<String> tasks = new ArrayList<>();
        for (int i = 0; i < taskListModel.size(); i++) {
            tasks.add(taskListModel.getElementAt(i));
        }

        todoLists.put(currentListName, tasks);
    }

    private void updateTaskControls() {
        boolean hasSelectedList = currentListName != null;
        currentListLabel.setText(hasSelectedList ? currentListName : "No List Selected");
        taskInput.setEnabled(hasSelectedList);
        addTaskButton.setEnabled(hasSelectedList);
        deleteTaskButton.setEnabled(hasSelectedList);
    }

    private void loadTodoLists() {
        todoLists.clear();
        todoListNameModel.clear();

        if (Files.exists(TODO_LISTS_FILE)) {
            loadTodoListsFromCurrentFile();
        } else if (Files.exists(OLD_TASKS_FILE)) {
            loadOldTasksFile();
        }

        if (todoLists.isEmpty()) {
            todoLists.put(DEFAULT_LIST_NAME, new ArrayList<>());
        }

        for (String listName : todoLists.keySet()) {
            todoListNameModel.addElement(listName);
        }
    }

    private void loadTodoListsFromCurrentFile() {
        try (BufferedReader reader = Files.newBufferedReader(TODO_LISTS_FILE)) {
            String line;

            while ((line = reader.readLine()) != null) {
                List<String> fields = parseCsvLine(line);

                if (fields.isEmpty() || fields.get(0).isBlank()) {
                    continue;
                }

                String listName = fields.get(0);
                todoLists.putIfAbsent(listName, new ArrayList<>());

                if (fields.size() > 1 && !fields.get(1).isBlank()) {
                    todoLists.get(listName).add(fields.get(1));
                }
            }
        } catch (IOException exception) {
            JOptionPane.showMessageDialog(
                    null,
                    "Could not load saved lists from " + TODO_LISTS_FILE + ".",
                    "Load Error",
                    JOptionPane.ERROR_MESSAGE
            );
        }
    }

    private void loadOldTasksFile() {
        List<String> defaultTasks = new ArrayList<>();

        try (BufferedReader reader = Files.newBufferedReader(OLD_TASKS_FILE)) {
            String line;

            while ((line = reader.readLine()) != null) {
                List<String> fields = parseCsvLine(line);

                if (!fields.isEmpty() && !fields.get(0).isBlank()) {
                    defaultTasks.add(fields.get(0));
                }
            }
        } catch (IOException exception) {
            JOptionPane.showMessageDialog(
                    null,
                    "Could not load saved tasks from " + OLD_TASKS_FILE + ".",
                    "Load Error",
                    JOptionPane.ERROR_MESSAGE
            );
        }

        todoLists.put(DEFAULT_LIST_NAME, defaultTasks);
    }

    private void saveTodoListsAndExit(JFrame frame) {
        try {
            saveCurrentTasksToMemory();
            saveTodoLists();
            frame.dispose();
            System.exit(0);
        } catch (IOException exception) {
            JOptionPane.showMessageDialog(
                    frame,
                    "Could not save lists to " + TODO_LISTS_FILE + ".",
                    "Save Error",
                    JOptionPane.ERROR_MESSAGE
            );
        }
    }

    private void saveTodoLists() throws IOException {
        try (BufferedWriter writer = Files.newBufferedWriter(TODO_LISTS_FILE)) {
            for (Map.Entry<String, List<String>> entry : todoLists.entrySet()) {
                String listName = entry.getKey();
                List<String> tasks = entry.getValue();

                if (tasks.isEmpty()) {
                    writer.write(toCsvLine(listName, ""));
                    writer.newLine();
                    continue;
                }

                for (String task : tasks) {
                    writer.write(toCsvLine(listName, task));
                    writer.newLine();
                }
            }
        }
    }

    private String toCsvLine(String... values) {
        List<String> escapedValues = new ArrayList<>();

        for (String value : values) {
            escapedValues.add("\"" + value.replace("\"", "\"\"") + "\"");
        }

        return String.join(",", escapedValues);
    }

    private List<String> parseCsvLine(String line) {
        List<String> fields = new ArrayList<>();
        StringBuilder currentField = new StringBuilder();
        boolean insideQuotes = false;

        for (int i = 0; i < line.length(); i++) {
            char character = line.charAt(i);

            if (character == '"') {
                if (insideQuotes && i + 1 < line.length() && line.charAt(i + 1) == '"') {
                    currentField.append('"');
                    i++;
                } else {
                    insideQuotes = !insideQuotes;
                }
            } else if (character == ',' && !insideQuotes) {
                fields.add(currentField.toString());
                currentField.setLength(0);
            } else {
                currentField.append(character);
            }
        }

        fields.add(currentField.toString());
        return fields;
    }
}
