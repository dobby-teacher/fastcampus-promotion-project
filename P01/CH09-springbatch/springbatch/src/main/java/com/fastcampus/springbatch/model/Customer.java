package com.fastcampus.springbatch.model;

import lombok.Data;
import lombok.NoArgsConstructor;

import java.time.LocalDateTime;

@Data
@NoArgsConstructor
public class Customer {
    private String id;
    private String name;
    private String email;
    private LocalDateTime registeredDate;
}