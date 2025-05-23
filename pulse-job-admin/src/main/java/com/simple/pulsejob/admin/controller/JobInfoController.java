package com.simple.pulsejob.admin.controller;

import com.simple.pulsejob.admin.model.entity.JobInfo;
import com.simple.pulsejob.admin.service.IJobInfoService;
import lombok.RequiredArgsConstructor;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.List;
import java.util.Optional;

@RestController
@RequiredArgsConstructor
@RequestMapping("/api/jobInfo")
public class JobInfoController {

    private final IJobInfoService jobInfoService;

    @PostMapping
    public ResponseEntity<JobInfo> createJobInfo(@RequestBody JobInfo jobInfo) {
        JobInfo savedJobInfo = jobInfoService.saveJobInfo(jobInfo);
        return new ResponseEntity<>(savedJobInfo, HttpStatus.CREATED);
    }

    @GetMapping("/{id}")
    public ResponseEntity<JobInfo> getJobInfoById(@PathVariable Integer id) {
        Optional<JobInfo> jobInfo = jobInfoService.getJobInfoById(id);
        return jobInfo.map(value -> new ResponseEntity<>(value, HttpStatus.OK))
                .orElseGet(() -> new ResponseEntity<>(HttpStatus.NOT_FOUND));
    }

    @GetMapping
    public ResponseEntity<List<JobInfo>> getAllJobInfos() {
        List<JobInfo> jobInfos = jobInfoService.getAllJobInfos();
        return new ResponseEntity<>(jobInfos, HttpStatus.OK);
    }

    @GetMapping("/handler/{jobHandler}")
    public ResponseEntity<List<JobInfo>> getJobInfosByHandler(@PathVariable String jobHandler) {
        List<JobInfo> jobInfos = jobInfoService.getJobInfosByHandler(jobHandler);
        return new ResponseEntity<>(jobInfos, HttpStatus.OK);
    }

    @GetMapping("/cron-type/{cronType}")
    public ResponseEntity<List<JobInfo>> getJobInfosByCronType(@PathVariable Short cronType) {
        List<JobInfo> jobInfos = jobInfoService.getJobInfosByCronType(cronType);
        return new ResponseEntity<>(jobInfos, HttpStatus.OK);
    }

    @PutMapping("/{id}")
    public ResponseEntity<JobInfo> updateJobInfo(@PathVariable Integer id, @RequestBody JobInfo jobInfo) {
        if (!id.equals(jobInfo.getId())) {
            return new ResponseEntity<>(HttpStatus.BAD_REQUEST);
        }
        JobInfo updatedJobInfo = jobInfoService.updateJobInfo(jobInfo);
        return new ResponseEntity<>(updatedJobInfo, HttpStatus.OK);
    }

    @DeleteMapping("/{id}")
    public ResponseEntity<Void> deleteJobInfo(@PathVariable Integer id) {
        jobInfoService.deleteJobInfo(id);
        return new ResponseEntity<>(HttpStatus.NO_CONTENT);
    }

    @PostMapping("/batch")
    public ResponseEntity<List<JobInfo>> batchCreateJobInfos(@RequestBody List<JobInfo> jobInfos) {
        try {
            List<JobInfo> savedJobInfos = jobInfoService.batchSaveJobInfos(jobInfos);
            return new ResponseEntity<>(savedJobInfos, HttpStatus.CREATED);
        } catch (IllegalArgumentException e) {
            return new ResponseEntity<>(HttpStatus.BAD_REQUEST);
        } catch (Exception e) {
            return new ResponseEntity<>(HttpStatus.INTERNAL_SERVER_ERROR);
        }
    }
}