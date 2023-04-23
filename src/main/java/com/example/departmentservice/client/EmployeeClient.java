package com.example.departmentservice.client;

import com.example.departmentservice.model.Employee;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.service.annotation.GetExchange;
import org.springframework.web.service.annotation.HttpExchange;

import java.util.List;

@HttpExchange("/employees")
public interface EmployeeClient {
    @GetExchange("/departments/{departmentId}")
    List<Employee> findByDepartmentId(@PathVariable Long departmentId);
}
