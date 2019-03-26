package com.zzq.springboot.config;


import jdk.nashorn.internal.scripts.JO;
import org.quartz.JobDetail;
import org.quartz.Scheduler;
import org.quartz.SchedulerFactory;
import org.quartz.Trigger;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.scheduling.quartz.MethodInvokingJobDetailFactoryBean;
import org.springframework.scheduling.quartz.SchedulerFactoryBean;
import org.springframework.scheduling.quartz.SimpleTriggerFactoryBean;
import org.springframework.stereotype.Component;

/**
 * @author ZZQ
 * @Title: spring-boot-jdbc
 * @Package com.zzq.config
 * @date 2018/7/2 10:47
 */
@Configuration
public class QuartzConfig {

    @Bean
    public MethodInvokingJobDetailFactoryBean jobDetail(App app){
        MethodInvokingJobDetailFactoryBean factoryBean = new MethodInvokingJobDetailFactoryBean();
        factoryBean.setTargetObject(app);
        factoryBean.setTargetMethod("run");
        return  factoryBean;
    }

    // 配置触发器
    @Bean
    public SimpleTriggerFactoryBean myTrigger(JobDetail jobDetail){
        SimpleTriggerFactoryBean bean = new SimpleTriggerFactoryBean();
        bean.setJobDetail(jobDetail);
        // 设置启动延迟
        bean.setStartDelay(0);
        // 设置调用时间
        bean.setRepeatInterval(5000);
        return  bean ;
    }

    // 配置scheduler
    @Bean
    public SchedulerFactoryBean schedulerFactory(Trigger myTrigger){
        SchedulerFactoryBean bean = new SchedulerFactoryBean();
        bean.setStartupDelay(1);
        // 注册触发器
        bean.setTriggers(myTrigger);
        return bean;
    }


}


