package com.simple.pulsejob.admin.model.entity;

import com.simple.pulsejob.admin.model.enums.ScheduleTypeEnum;
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

    @Column(name = "schedule_rate", length = 100)
    private String scheduleRate;

    @Column(name = "schedule_type")
    private ScheduleTypeEnum scheduleType;

    @Column(name = "executor_id")
    private Integer executorId;

} 