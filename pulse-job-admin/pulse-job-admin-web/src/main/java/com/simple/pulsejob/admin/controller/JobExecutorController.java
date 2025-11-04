package com.simple.pulsejob.admin.controller;

import com.simple.pulsejob.admin.business.service.IJobExecutorService;
import com.simple.pulsejob.admin.common.model.entity.JobExecutor;
import com.simple.pulsejob.admin.common.model.enums.RegisterTypeEnum;
import com.simple.pulsejob.admin.common.model.param.JobExecutorParam;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.List;
import java.util.Optional;

@RestController
@RequiredArgsConstructor
@RequestMapping("/api/jobExecutor")
public class JobExecutorController {

    private final IJobExecutorService jobExecutorService;

    @PostMapping
    public ResponseEntity<Void> createJobExecutor(@RequestBody JobExecutorParam jobExecutorParam) {
        try {
            jobExecutorService.addJobExecutor(jobExecutorParam);
            return new ResponseEntity<>(HttpStatus.CREATED);
        } catch (Exception e) {
            return new ResponseEntity<>(HttpStatus.INTERNAL_SERVER_ERROR);
        }
    }

    @GetMapping("/{id}")
    public ResponseEntity<JobExecutor> getJobExecutorById(@PathVariable Integer id) {
        Optional<JobExecutor> jobExecutor = jobExecutorService.getJobExecutorById(id);
        return jobExecutor.map(value -> new ResponseEntity<>(value, HttpStatus.OK))
                .orElseGet(() -> new ResponseEntity<>(HttpStatus.NOT_FOUND));
    }

    @GetMapping("/name/{executorName}")
    public ResponseEntity<JobExecutor> getJobExecutorByName(@PathVariable String executorName) {
        Optional<JobExecutor> jobExecutor = jobExecutorService.getJobExecutorByName(executorName);
        return jobExecutor.map(value -> new ResponseEntity<>(value, HttpStatus.OK))
                .orElseGet(() -> new ResponseEntity<>(HttpStatus.NOT_FOUND));
    }

    @GetMapping
    public ResponseEntity<List<JobExecutor>> getAllJobExecutors() {
        List<JobExecutor> jobExecutors = jobExecutorService.getAllJobExecutors();
        return new ResponseEntity<>(jobExecutors, HttpStatus.OK);
    }

    @GetMapping("/register-type/{registerType}")
    public ResponseEntity<List<JobExecutor>> getJobExecutorsByRegisterType(@PathVariable RegisterTypeEnum registerType) {
        List<JobExecutor> jobExecutors = jobExecutorService.getJobExecutorsByRegisterType(registerType);
        return new ResponseEntity<>(jobExecutors, HttpStatus.OK);
    }

    @PutMapping("/{id}")
    public ResponseEntity<JobExecutor> updateJobExecutor(@PathVariable Integer id, @RequestBody JobExecutor jobExecutor) {
        if (!id.equals(jobExecutor.getId())) {
            return new ResponseEntity<>(HttpStatus.BAD_REQUEST);
        }
        try {
            JobExecutor updatedJobExecutor = jobExecutorService.updateJobExecutor(jobExecutor);
            return new ResponseEntity<>(updatedJobExecutor, HttpStatus.OK);
        } catch (IllegalArgumentException e) {
            return new ResponseEntity<>(HttpStatus.BAD_REQUEST);
        } catch (Exception e) {
            return new ResponseEntity<>(HttpStatus.INTERNAL_SERVER_ERROR);
        }
    }

    @DeleteMapping("/{id}")
    public ResponseEntity<Void> deleteJobExecutor(@PathVariable Integer id) {
        try {
            jobExecutorService.deleteJobExecutor(id);
            return new ResponseEntity<>(HttpStatus.NO_CONTENT);
        } catch (Exception e) {
            return new ResponseEntity<>(HttpStatus.INTERNAL_SERVER_ERROR);
        }
    }

    @PostMapping("/batch")
    public ResponseEntity<Void> batchCreateJobExecutors(@RequestBody List<JobExecutorParam> jobExecutorParams) {
        try {
            jobExecutorService.batchAddJobExecutors(jobExecutorParams);
            return new ResponseEntity<>(HttpStatus.CREATED);
        } catch (IllegalArgumentException e) {
            return new ResponseEntity<>(HttpStatus.BAD_REQUEST);
        } catch (Exception e) {
            return new ResponseEntity<>(HttpStatus.INTERNAL_SERVER_ERROR);
        }
    }
} 