package com.kbsec.seatreservationbe.entity;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.Id;
import jakarta.persistence.Table;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Getter;
import lombok.NoArgsConstructor;
import org.springframework.data.annotation.CreatedDate;

import java.time.LocalDateTime;

@Entity
@Getter
@Table(name = "Reservations")
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class Reservation {

    @Id
    @Column(name = "employee_id")
    private String employeeId;

    @Column(name = "seat_num")
    private int seat;

    // TODO : Enum
    private String status;

    @CreatedDate
    @Column(name = "datetime")
    private LocalDateTime reservedDateTime;
}
