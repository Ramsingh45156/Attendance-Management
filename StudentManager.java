import java.io.*;
import java.util.*;
import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;

public class StudentManager {
    private List<Student> students;
    private String currentClass = "Default";
    private final String BASE_DIR = "data/classes/";
    private static final DateTimeFormatter dtf = DateTimeFormatter.ofPattern("yyyy-MM-dd HH:mm:ss");

    public StudentManager() {
        students = new ArrayList<>();
        ensureDirectories();
        loadStudents();
    }

    private void ensureDirectories() {
        File dir = new File(BASE_DIR + currentClass);
        if (!dir.exists()) {
            dir.mkdirs();
        }
    }

    public synchronized void switchClass(String className) {
        this.currentClass = className;
        ensureDirectories();
        loadStudents();
    }

    public List<String> listClasses() {
        File dir = new File(BASE_DIR);
        String[] contents = dir.list((file, name) -> new File(file, name).isDirectory());
        return contents != null ? Arrays.asList(contents) : new ArrayList<>();
    }

    public void createClass(String className) {
        File dir = new File(BASE_DIR + className);
        if (!dir.exists()) {
            dir.mkdirs();
        }
    }

    public synchronized boolean addStudent(String name, String course) {
        int rollNo = 1;
        for (Student s : students) {
            if (s.getRollNo() >= rollNo) {
                rollNo = s.getRollNo() + 1;
            }
        }
        students.add(new Student(name, rollNo, course));
        saveStudents();
        return true;
    }

    public synchronized List<Student> getStudents() {
        return new ArrayList<>(students);
    }

    public synchronized boolean deleteStudent(int rollNo) {
        boolean removed = students.removeIf(s -> s.getRollNo() == rollNo);
        if (removed) {
            saveStudents();
        }
        return removed;
    }

    public synchronized boolean checkInStudent(int rollNo) {
        for (Student s : students) {
            if (s.getRollNo() == rollNo) {
                if (s.checkIn()) {
                    saveStudents();
                    logHistory(s, "IN");
                    return true;
                }
                return false;
            }
        }
        return false;
    }

    public synchronized boolean checkOutStudent(int rollNo) {
        for (Student s : students) {
            if (s.getRollNo() == rollNo) {
                if (s.checkOut()) {
                    saveStudents();
                    logHistory(s, "OUT");
                    return true;
                }
                return false;
            }
        }
        return false;
    }

    private synchronized void logHistory(Student s, String type) {
        String historyFile = BASE_DIR + currentClass + "/history.txt";
        try (BufferedWriter writer = new BufferedWriter(new FileWriter(historyFile, true))) {
            String time = LocalDateTime.now().format(dtf);
            writer.write(String.format("%s,%d,%s,%s,%s", time, s.getRollNo(), s.getName(), type, s.getCourse()));
            writer.newLine();
        } catch (IOException e) {
            System.err.println("Error saving history: " + e.getMessage());
        }
    }

    public List<String> getHistory() {
        List<String> history = new ArrayList<>();
        File file = new File(BASE_DIR + currentClass + "/history.txt");
        if (!file.exists()) return history;

        try (BufferedReader reader = new BufferedReader(new FileReader(file))) {
            String line;
            while ((line = reader.readLine()) != null) {
                history.add(line);
            }
        } catch (IOException e) {
            System.err.println("Error loading history: " + e.getMessage());
        }
        return history;
    }

    public void setClassStatus(String className, String status) {
        String statusFile = BASE_DIR + className + "/status.txt";
        try (BufferedWriter writer = new BufferedWriter(new FileWriter(statusFile))) {
            writer.write(status);
        } catch (IOException e) {
            System.err.println("Error saving class status: " + e.getMessage());
        }
    }

    public String getClassStatus(String className) {
        String statusFile = BASE_DIR + className + "/status.txt";
        File file = new File(statusFile);
        if (!file.exists()) return "Active";

        try (BufferedReader reader = new BufferedReader(new FileReader(file))) {
            return reader.readLine();
        } catch (IOException e) {
            return "Active";
        }
    }

    private void saveStudents() {
        String path = BASE_DIR + currentClass + "/students.txt";
        try (BufferedWriter writer = new BufferedWriter(new FileWriter(path))) {
            for (Student s : students) {
                writer.write(s.toFileString());
                writer.newLine();
            }
        } catch (IOException e) {
            System.err.println("Error saving students: " + e.getMessage());
        }
    }

    private void loadStudents() {
        students.clear();
        String path = BASE_DIR + currentClass + "/students.txt";
        File file = new File(path);
        if (!file.exists()) return;

        try (BufferedReader reader = new BufferedReader(new FileReader(file))) {
            String line;
            while ((line = reader.readLine()) != null) {
                String[] data = line.split(",");
                if (data.length >= 4) {
                    Student s = new Student(data[0], Integer.parseInt(data[1]), data[2]);
                    int count = Integer.parseInt(data[3]);
                    for (int i = 0; i < count; i++) s.increaseAttendance();
                    if (data.length >= 5) s.setIsCheckedIn(Boolean.parseBoolean(data[4]));
                    students.add(s);
                }
            }
        } catch (IOException e) {
            System.err.println("Error loading students: " + e.getMessage());
        }
    }

    public String getCurrentClass() {
        return currentClass;
    }
}
