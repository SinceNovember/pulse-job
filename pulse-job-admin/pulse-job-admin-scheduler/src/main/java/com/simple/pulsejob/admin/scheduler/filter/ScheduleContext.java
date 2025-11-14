package com.simple.pulsejob.admin.scheduler.filter;

import com.simple.pulsejob.transport.channel.JChannel;
import lombok.AllArgsConstructor;
import lombok.Data;

@Data
@AllArgsConstructor
public class ScheduleContext implements ScheduleFilterContext {

    private JChannel channel;


}
