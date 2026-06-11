package org.example.demo1.model.dto;

import java.util.ArrayList;
import java.util.List;

public class ScheduleResponse {

    private String semester = "未知";
    private String week = "未知";
    private List<CourseInfo> courses = new ArrayList<>();

    public String getSemester() {
        return semester;
    }

    public void setSemester(String semester) {
        this.semester = semester;
    }

    public String getWeek() {
        return week;
    }

    public void setWeek(String week) {
        this.week = week;
    }

    public List<CourseInfo> getCourses() {
        return courses;
    }

    public void setCourses(List<CourseInfo> courses) {
        this.courses = courses == null ? new ArrayList<>() : courses;
    }
}
