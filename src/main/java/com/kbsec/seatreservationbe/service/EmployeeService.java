package com.kbsec.seatreservationbe.service;

import com.kbsec.seatreservationbe.entity.Employee;
import com.kbsec.seatreservationbe.repository.EmployeeRepository;
import org.springframework.stereotype.Service;

@Service
public class EmployeeService {

    private final EmployeeRepository employeeRepository;

    public EmployeeService(EmployeeRepository employeeRepository) {
        this.employeeRepository = employeeRepository;
    }

    public void saveEmployee(String employeeId, String name, String email){
        Employee employee = Employee.builder()
                .employeeId(employeeId)
                .name(name)
                .email(email)
                .build();

        employeeRepository.save(employee);
    }

    public Employee getEmployee(String employeeId){
        return employeeRepository.findById(employeeId).orElse(null);
    }
}
