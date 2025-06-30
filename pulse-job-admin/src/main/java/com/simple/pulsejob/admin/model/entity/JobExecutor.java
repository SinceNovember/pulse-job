package com.simple.pulsejob.admin.model.entity;

import com.simple.pulsejob.admin.model.enums.RegisterTypeEnum;
import com.simple.pulsejob.common.util.StringUtil;
import com.simple.pulsejob.common.util.Strings;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import jakarta.persistence.*;
import org.springframework.cglib.core.Local;
import java.io.Serializable;
import java.time.LocalDateTime;

@Data
@Entity
@Table(name = "job_executor")
public class JobExecutor implements Serializable {

    private static final long serialVersionUID = 1L;

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Integer id;

    @Column(name = "executor_name", length = 64, nullable = false)
    private String executorName;

    @Column(name = "executor_desc", length = 12)
    private String executorDesc;

    @Column(name = "register_type", nullable = false)
    private RegisterTypeEnum registerType;

    @Column(name = "executor_address", columnDefinition = "TEXT")
    private String executorAddress;

    @Column(name = "update_time")
    private LocalDateTime updateTime;

    public static JobExecutor of(String executorName, String executorAddress) {
        JobExecutor jobExecutor = new JobExecutor();
        jobExecutor.setExecutorName(executorName);
        jobExecutor.setExecutorAddress(executorAddress);
        jobExecutor.setRegisterType(RegisterTypeEnum.AUTO);
        jobExecutor.setUpdateTime(LocalDateTime.now());
        return jobExecutor;
    }

    public JobExecutor updateAddressIfAbsent(String address) {
        if (StringUtil.isBlank(this.executorAddress) || !this.executorAddress.contains(address)) {
            this.executorAddress = this.executorAddress + Strings.SEMICOLON + address;
        }
        return this;
    }

    public void refreshUpdateTime() {
        this.updateTime = LocalDateTime.now();
    }

}