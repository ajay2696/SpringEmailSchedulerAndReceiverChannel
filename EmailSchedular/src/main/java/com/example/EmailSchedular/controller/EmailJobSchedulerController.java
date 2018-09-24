package com.example.EmailSchedular.controller;

import java.sql.Date;
import java.time.ZonedDateTime;
import java.util.UUID;

import javax.validation.Valid;

import org.quartz.JobBuilder;
import org.quartz.JobDataMap;
import org.quartz.JobDetail;
import org.quartz.Scheduler;
import org.quartz.SimpleScheduleBuilder;
import org.quartz.Trigger;
import org.quartz.TriggerBuilder;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RestController;

import com.example.EmailSchedular.job.EmailJob;
import com.example.EmailSchedular.payload.ScheduleEmailRequest;
import com.example.EmailSchedular.payload.ScheduleEmailResponse;

@RestController
public class EmailJobSchedulerController {
	
	private static final Logger logger = LoggerFactory.getLogger(EmailJobSchedulerController.class);
	
	@Autowired
	private Scheduler scheduler;
	
	@PostMapping("scheduleEmail")
	public ResponseEntity<ScheduleEmailResponse> scheduleEmail(@Valid @RequestBody ScheduleEmailRequest scheduleEmailRequest){
		try {
			ZonedDateTime dateTime = ZonedDateTime.of(scheduleEmailRequest.getDateTime(),scheduleEmailRequest.getTimeZone());
			
			if(dateTime.isBefore(ZonedDateTime.now())) {
				ScheduleEmailResponse scheduleEmailResponse = new ScheduleEmailResponse(false,"dateTime Muste be later than current TimeStamp");
				return ResponseEntity.badRequest().body(scheduleEmailResponse);
			}
			
			JobDetail jobDetail = buildJobDetail(scheduleEmailRequest);
			Trigger trigger = buildJobTrigger(jobDetail, dateTime);
			scheduler.scheduleJob(jobDetail,trigger);
			ScheduleEmailResponse scheduleEmailResponse = new ScheduleEmailResponse(true, jobDetail.getKey().getName(),
					jobDetail.getKey().getGroup(),"Email Scheduled Successfully");
			
			
			
			
		} catch(Exception ex) {
			logger.error("Error Scheduling Email",ex);
			ScheduleEmailResponse scheduleEmailResponse = new ScheduleEmailResponse(false,"Error Scheduling Email");
			return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR).body(scheduleEmailResponse);		
		}
		return null;
		
	}
	
	private JobDetail buildJobDetail(ScheduleEmailRequest scheduleEmailRequest) {
		JobDataMap jobDataMap = new JobDataMap();
		
		jobDataMap.put("email", scheduleEmailRequest.getEmail());
		jobDataMap.put("subject", scheduleEmailRequest.getSubject());
		jobDataMap.put("body", scheduleEmailRequest.getBody());
		
		return JobBuilder.newJob(EmailJob.class)
				.withIdentity(UUID.randomUUID().toString(),"email-jobs")
				.withDescription("Send Email Job")
				.usingJobData(jobDataMap)
				.storeDurably()
				.build();
				
	}
	
	private Trigger buildJobTrigger(JobDetail jobDetail,ZonedDateTime startAt) {
		return TriggerBuilder.newTrigger()
				.forJob(jobDetail)
				.withIdentity(jobDetail.getKey().getName(),"email-triggers")
				.withDescription("Send Email Trigger")
				.startAt(Date.from(startAt.toInstant()))
				.withSchedule(SimpleScheduleBuilder.simpleSchedule().withMisfireHandlingInstructionFireNow())
				.build();
	}
	

}
