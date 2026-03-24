// import java.time.LocalDateTime;
// import java.time.format.DateTimeFormatter;

import java.time.LocalDateTime;

public class Student {
    private String name;
    private int rollNo;
    private String course;
    private int attendanceCount;
    private LocalDateTime checkInTime;
    private LocalDateTime checkOutTime;

    public Student(String name, int rollNo, String course) {
        this.name = name;
        this.rollNo = rollNo;
        this.course = course;
        this.attendanceCount = 0;
        this.checkInTime = null;
        this.checkOutTime = null;
    }

    public String getName() {
        return name;
    }

    public int getRollNo() {
        return rollNo;
    }

    public String getCourse() {
        return course;
    }

    public int getAttendanceCount() {
        return attendanceCount;
    }

    public void increaseAttendance() {
        attendanceCount++; // Attendance increase karne ka method
    }

    public boolean checkIn() {
        if (checkInTime == null) { // Pehle se check-in nahi hai
            checkInTime = LocalDateTime.now();
            return true;
        }
        return false; // Already checked in
    }

    public boolean isCheckedIn() {
        return checkInTime != null;
    }

    public boolean checkOut() {
        if (checkInTime != null && checkOutTime == null) { // Check-out allowed
            checkOutTime = LocalDateTime.now();
            attendanceCount++; // Attendance badhao
            checkInTime = null; // Reset check-in
            checkOutTime = null; // Reset check-out
            return true;
        }
        return false; // Check-out nahi ho sakta
    }

    public void setIsCheckedIn(boolean in) {
        this.checkInTime = in ? LocalDateTime.now() : null;
    }

    public String toFileString() {
        return name + "," + rollNo + "," + course + "," + attendanceCount + "," + isCheckedIn();
    }

}
