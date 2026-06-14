package com.ankitmishra.task_scheduler.config;

import org.springframework.amqp.core.*;
import org.springframework.amqp.rabbit.core.RabbitTemplate;
import org.springframework.amqp.support.converter.Jackson2JsonMessageConverter;
import org.springframework.amqp.support.converter.MessageConverter;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;


@Configuration
public class RabbitMQConfig {

    public static final String TASK_QUEUE="task.queue";
    public static final String TASK_DLQ="task.dlq";
    public static final String TASK_EXCHANGE="task.exchange";
    public static final String DLX_EXCHANGE="task.dlx.exchange";
    public static final String TASK_ROUTING_KEY="task";


    @Bean
    public DirectExchange dlxExchange(){
        return new DirectExchange(DLX_EXCHANGE,true,false);
    }

    @Bean
    public Queue DeadLetterQueue(){
        return QueueBuilder.durable(TASK_DLQ).build();
    }

    @Bean
    public Binding dlqBinding(){
        return BindingBuilder.bind(DeadLetterQueue()).to(dlxExchange()).with(TASK_ROUTING_KEY);
    }

    @Bean
    public Queue taskQueue(){
        return QueueBuilder
                .durable(TASK_QUEUE)
                .withArgument("x-dead-letter-exchange", DLX_EXCHANGE)
                .withArgument("x-dead-letter-routing-key",TASK_ROUTING_KEY)
                .build();
    }

    @Bean
    public DirectExchange taskExchange(){
        return new DirectExchange(TASK_EXCHANGE,true,false);
    }

    @Bean
    public Binding taskBinding(){
        return BindingBuilder.bind(taskQueue()).to(taskExchange()).with(TASK_ROUTING_KEY);
    }

    @Bean
    public MessageConverter jsonMessageConverter(){
        return new Jackson2JsonMessageConverter();
    }

    @Bean
    public AmqpTemplate amqpTemplate(RabbitTemplate rabbitTemplate){
        rabbitTemplate.setMessageConverter(jsonMessageConverter());
        return rabbitTemplate;
    }



}
