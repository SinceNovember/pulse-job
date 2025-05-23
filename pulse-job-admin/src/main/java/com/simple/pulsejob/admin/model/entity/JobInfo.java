package com.simple.pulsejob.admin.model.entity;

import lombok.Data;
import lombok.NoArgsConstructor;

import jakarta.persistence.*;
import java.io.Serializable;

@Data
@NoArgsConstructor
@Entity
@Table(name = "job_info")
public class JobInfo implements Serializable {

    private static final long serialVersionUID = 1L;

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Integer id;

    @Column(name = "job_handler", length = 100)
    private String jobHandler;

    @Column(name = "cron", length = 100)
    private String cron;

    @Column(name = "cron_type")
    private Short cronType;

} 