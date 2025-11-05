package com.simple.pulsejob.admin.common.model.entity;

import com.simple.pulsejob.admin.common.model.enums.RegisterTypeEnum;
import com.simple.pulsejob.common.util.StringUtil;
import lombok.Data;

import jakarta.persistence.*;
import java.io.Serializable;
import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;

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
        if (StringUtil.isNullOrEmpty(address)) {
            return this;
        }

        if (StringUtil.isNullOrEmpty(executorAddress)) {
            this.executorAddress = address;
        } else {
            List<String> addressList = new ArrayList<>(Arrays.asList(this.executorAddress.split(";")));
            if (!addressList.contains(address)) {
                addressList.add(address);
                this.executorAddress = String.join(";", addressList);
            }
        }
        return this;
    }
    public void refreshUpdateTime() {
        this.updateTime = LocalDateTime.now();
    }

}