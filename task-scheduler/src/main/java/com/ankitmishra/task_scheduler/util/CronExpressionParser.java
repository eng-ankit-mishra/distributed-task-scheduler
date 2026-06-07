package com.ankitmishra.task_scheduler.util;

import org.springframework.scheduling.support.CronExpression;

import java.time.LocalDate;
import java.time.LocalDateTime;

public class CronExpressionParser {

    private CronExpressionParser(){}

    public static LocalDateTime getNextRunTime(String cronExpression){
        CronExpression cron = CronExpression.parse(cronExpression);
        LocalDateTime next = cron.next(LocalDateTime.now());

        if(next==null){
            throw new IllegalArgumentException(
                    "Cron expression produced no next execution time: " + cronExpression
            );
        }

        return next;
    }

    public static boolean isValid(String  cronExpression){
        try{
            CronExpression.parse(cronExpression);
            return true;
        }catch(Exception e){
            return false;
        }
    }
}
