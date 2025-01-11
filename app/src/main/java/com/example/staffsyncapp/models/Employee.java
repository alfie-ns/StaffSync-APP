package com.example.staffsyncapp.models;

// Employee model class: Represents an employee with various attributes.
public class Employee { // Employee model class
    private int id;
    private String firstname;
    private String lastname;
    private String email;
    private String department;
    private double salary;
    private String joiningdate;

    // Constructor: define each field in an employee
    public Employee(int id, String firstname, String lastname, String email,
                    String department, double salary, String joiningdate) {
        this.id = id;
        this.firstname = firstname;
        this.lastname = lastname;
        this.email = email;
        this.department = department;
        this.salary = salary;
        this.joiningdate = joiningdate;
    }

    // Getters
    public int getId() { return id; }
    public String getFirstname() { return firstname; }
    public String getLastname() { return lastname; }
    public String getEmail() { return email; }
    public String getDepartment() { return department; }
    public double getSalary() { return salary; }
    public String getJoiningDate() { return joiningdate; }

    // Helper methods
    public String getName() {
        return String.format("%s %s",
                firstname != null ? firstname : "",
                lastname != null ? lastname : "").trim();
    }
    // ... don't have the time for more methods
}