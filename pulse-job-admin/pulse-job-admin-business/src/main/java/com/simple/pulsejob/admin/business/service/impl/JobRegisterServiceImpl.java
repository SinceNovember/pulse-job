package com.simple.pulsejob.admin.business.service.impl;

import java.util.HashMap;
import java.util.List;
import java.util.Map;
import com.simple.plusejob.serialization.Serializer;
import com.simple.plusejob.serialization.SerializerType;
import com.simple.plusejob.serialization.io.OutputBuf;
import com.simple.pulsejob.admin.business.service.IJobRegisterService;
import com.simple.pulsejob.admin.common.model.dto.JobRegisterDTO;
import com.simple.pulsejob.admin.common.model.param.JobInfoParam;
import com.simple.pulsejob.admin.scheduler.processor.JobExecutorAcceptorProcessor;
import com.simple.pulsejob.transport.JProtocolHeader;
import com.simple.pulsejob.transport.JRequest;
import com.simple.pulsejob.transport.channel.JChannel;
import com.simple.pulsejob.transport.channel.JFutureListener;
import com.simple.pulsejob.transport.metadata.ExecutorKey;
import com.simple.pulsejob.transport.metadata.MessageWrapper;
import com.simple.pulsejob.transport.payload.JRequestPayload;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;

@Service
@RequiredArgsConstructor
public class JobRegisterServiceImpl implements IJobRegisterService {

    private final Map<Byte, Serializer> serializerMap;

    private static final Map<String, JobRegisterDTO> JOB_REGISTER_MAP = new HashMap<>();

    @Override
    public void registerJob(List<JobInfoParam> jobRegisterParams) {
        for (JobInfoParam jobRegisterParam : jobRegisterParams) {
            JOB_REGISTER_MAP.put(jobRegisterParam.getJobHandler(), toJobRegisterDTO(jobRegisterParam));
        }
    }

    @Override
    public void triggerJob() {
//        JRequest jRequest = new JRequest();
//        MessageWrapper message = new MessageWrapper("test");
//        message.setArgs(new Object[]{});
//        jRequest.setMessage(message);
//        Serializer serializer = serializerMap.get(SerializerType.JAVA.value());
//        ExecutorKey executorWrapper = new ExecutorKey("my-executor");
//        JChannel jChannel =
//            JobExecutorAcceptorProcessor.channelGroupManager().find(executorWrapper).next();
//        JRequestPayload jRequestPayload = new JRequestPayload();
//        OutputBuf outputBuf = serializer.writeObject(jChannel.allocOutputBuf(), message);
//        jRequestPayload.outputBuf(SerializerType.JAVA.value(), JProtocolHeader.RESPONSE, outputBuf);
//        jChannel.write(jRequestPayload, new JFutureListener<JChannel>() {
//            @Override
//            public void operationSuccess(JChannel channel) throws Exception {
//                System.out.println(channel);
//            }
//
//            @Override
//            public void operationFailure(JChannel channel, Throwable cause) throws Exception {
//                System.out.println(cause);
//            }
//        });
    }

    public JobRegisterDTO toJobRegisterDTO(JobInfoParam jobRegisterParam) {
        JobRegisterDTO jobRegisterDTO = new JobRegisterDTO();
        jobRegisterDTO.setJobHandler(jobRegisterParam.getJobHandler());
        jobRegisterDTO.setScheduleRate(jobRegisterParam.getScheduleRate());
        jobRegisterDTO.setScheduleType(jobRegisterParam.getScheduleType());
        return jobRegisterDTO;
    }
}
