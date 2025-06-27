package com.simple.pulsejob.admin.controller;

import java.util.List;
import java.util.Optional;
import com.simple.pulsejob.admin.model.base.ResponseResult;
import com.simple.pulsejob.admin.model.entity.JobInfo;
import com.simple.pulsejob.admin.model.param.JobInfoParam;
import com.simple.pulsejob.admin.service.IJobInfoService;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.DeleteMapping;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.PutMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequiredArgsConstructor
@RequestMapping("/api/jobInfo")
public class JobInfoController {

    private final IJobInfoService jobInfoService;

    @PostMapping
    public ResponseResult<JobInfo> addJobInfo(@RequestBody JobInfoParam jobInfoParam) {
        jobInfoService.addJobInfo(jobInfoParam);
        return ResponseResult.ok();
    }

    @PostMapping("/batch")
    public ResponseResult<JobInfo> batchAddJobInfos(@RequestBody List<JobInfoParam> jobInfoParams) {
        jobInfoService.batchAddJobInfos(jobInfoParams);
        return ResponseResult.ok();
    }

    @GetMapping("/{id}")
    public ResponseEntity<JobInfo> getJobInfoById(@PathVariable("id")  Integer id) {
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


}