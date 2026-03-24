import java.io.*;
import java.util.*;

public class StudentManager {
    private List<Student> students;
    private final String FILE_NAME = "students.txt";

    public StudentManager() {
        students = new ArrayList<>();
        loadStudents();  // Auto-load students at start
    }

    public synchronized void addStudent(String name, int rollNo, String course) {
        students.add(new Student(name, rollNo, course));
        saveStudents();
        System.out.println("Student Added Successfully!");
    }

    public synchronized List<Student> getStudents() {
        return new ArrayList<>(students);
    }

    public synchronized boolean deleteStudent(int rollNo) {
        boolean removed = students.removeIf(s -> s.getRollNo() == rollNo);
        if (removed) {
            saveStudents();
            System.out.println("Student Deleted Successfully!");
        }
        return removed;
    }

    public void showAttendance() {
        if (students.isEmpty()) {
            System.out.println("No students found!");
            return;
        }
        for (Student s : students) {
            System.out.println(s.getName() + " | Roll No: " + s.getRollNo() + " | Course: " + s.getCourse() + " | Attendance: " + s.getAttendanceCount());
        }
    }

    public synchronized boolean checkInStudent(int rollNo) {
        for (Student s : students) {
            if (s.getRollNo() == rollNo) {
                if (s.checkIn()) {
                    saveStudents();
                    System.out.println("Check-In Successful for " + s.getName());
                    return true;
                } else {
                    System.out.println("Already Checked-In!");
                    return false;
                }
            }
        }
        System.out.println("Student not found!");
        return false;
    }

    public synchronized boolean checkOutStudent(int rollNo) {
        for (Student s : students) {
            if (s.getRollNo() == rollNo) {
                if (s.checkOut()) {
                    saveStudents();
                    System.out.println("Check-Out Successful for " + s.getName());
                    return true;
                } else {
                    System.out.println("Check-In First!");
                    return false;
                }
            }
        }
        System.out.println("Student not found!");
        return false;
    }

    private void saveStudents() {
        try (BufferedWriter writer = new BufferedWriter(new FileWriter(FILE_NAME))) {
            for (Student s : students) {
                writer.write(s.toFileString());
                writer.newLine();
            }
        } catch (IOException e) {
            System.out.println("Error Saving Data: " + e.getMessage());
        }
    }

    private void loadStudents() {
        File file = new File(FILE_NAME);
        if (!file.exists()) return;

        try (BufferedReader reader = new BufferedReader(new FileReader(FILE_NAME))) {
            String line;
            while ((line = reader.readLine()) != null) {
                String[] data = line.split(",");
                if (data.length >= 4) {
                    String name = data[0];
                    int rollNo = Integer.parseInt(data[1]);
                    String course = data[2];
                    int attendance = Integer.parseInt(data[3]);

                    Student student = new Student(name, rollNo, course);
                    for (int i = 0; i < attendance; i++) {
                        student.increaseAttendance();
                    }
                    if (data.length >= 5) {
                        student.setIsCheckedIn(Boolean.parseBoolean(data[4]));
                    }
                    students.add(student);
                } else {
                    System.out.println("Data Corrupted: " + line);
                }
            }
        } catch (IOException e) {
            System.out.println("Error Loading Data: " + e.getMessage());
        }
    }
}
