package com.romashka.test;

import org.springframework.amqp.core.*;
import org.springframework.amqp.rabbit.config.SimpleRabbitListenerContainerFactory;
import org.springframework.amqp.rabbit.connection.CachingConnectionFactory;
import org.springframework.amqp.rabbit.connection.ConnectionFactory;
import org.springframework.amqp.rabbit.core.RabbitAdmin;
import org.springframework.amqp.rabbit.core.RabbitTemplate;
import org.springframework.amqp.rabbit.listener.SimpleMessageListenerContainer;
import org.springframework.amqp.rabbit.listener.adapter.MessageListenerAdapter;
import org.springframework.amqp.support.converter.Jackson2JsonMessageConverter;
import org.springframework.amqp.support.converter.MessageConverter;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.retry.backoff.ExponentialBackOffPolicy;
import org.springframework.retry.policy.SimpleRetryPolicy;
import org.springframework.retry.support.RetryTemplate;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Qualifier;
import org.springframework.scheduling.TaskScheduler;
import org.springframework.scheduling.concurrent.ThreadPoolTaskScheduler;

@Configuration
public class TestConfig {
    private static final Logger log = LoggerFactory.getLogger(TestConfig.class);

    public static final String CDR_EXCHANGE = "cdr.exchange";
    public static final String CDR_ROUTING_KEY = "cdr.routingkey";
    public static final String CDR_QUEUE = "cdr.queue";
    //public static final String HRS_EXCHANGE = "hrs.exchange";
    public static final String HRS_ROUTING_KEY = "hrs.routingkey";
   // public static final String HRS_RESPONSE_QUEUE = "hrs.response.queue";
    public static final String BRT_TO_HRS_EXCHANGE = "brt-to-hrs.exchange";
    public static final String BRT_TO_HRS_ROUTING_KEY = "brt-to-hrs.routing.key";
    public static final String BRT_TO_HRS_QUEUE = "brt-to-hrs.queue";
    public static final String HRS_TO_BRT_EXCHANGE = "hrs-to-brt.exchange";
    public static final String HRS_TO_BRT_ROUTING_KEY = "hrs-to-brt.routing.key";
    public static final String HRS_TO_BRT_QUEUE = "hrs-to-brt.queue";

    @Bean
    public ConnectionFactory connectionFactory() {
        CachingConnectionFactory connectionFactory = new CachingConnectionFactory();
        connectionFactory.setHost("localhost");
        connectionFactory.setPort(5672);
        connectionFactory.setUsername("guest");
        connectionFactory.setPassword("guest");
        connectionFactory.setChannelCacheSize(25);
        connectionFactory.setConnectionTimeout(5000);
        connectionFactory.setChannelCheckoutTimeout(5000);
        return connectionFactory;
    }

    @Bean
    public RabbitAdmin rabbitAdmin(ConnectionFactory connectionFactory) {
        RabbitAdmin admin = new RabbitAdmin(connectionFactory);
        admin.setAutoStartup(true);
        return admin;
    }

    @Bean
    public TopicExchange cdrExchange() {
        return new TopicExchange(CDR_EXCHANGE, true, false);
    }



    @Bean
    public DirectExchange brtToHrsExchange() {
        return new DirectExchange(BRT_TO_HRS_EXCHANGE, true, false);
    }

    @Bean
    public DirectExchange hrsToBrtExchange() {
        return new DirectExchange(HRS_TO_BRT_EXCHANGE, true, false);
    }

    @Bean
    public Queue cdrQueue() {
        return new Queue(CDR_QUEUE, true);
    }



    @Bean
    public Queue brtToHrsQueue() {
        return new Queue(BRT_TO_HRS_QUEUE, true);
    }

    @Bean
    public Queue hrsToBrtQueue() {
        return new Queue(HRS_TO_BRT_QUEUE, true);
    }

    @Bean
    public Binding cdrBinding() {
        return BindingBuilder.bind(cdrQueue())
                .to(cdrExchange())
                .with(CDR_ROUTING_KEY);
    }



    @Bean
    public Binding brtToHrsBinding() {
        return BindingBuilder.bind(brtToHrsQueue())
                .to(brtToHrsExchange())
                .with(BRT_TO_HRS_ROUTING_KEY);
    }

    @Bean
    public Binding hrsToBrtBinding() {
        return BindingBuilder.bind(hrsToBrtQueue())
                .to(hrsToBrtExchange())
                .with(HRS_TO_BRT_ROUTING_KEY);
    }

    @Bean
    public MessageConverter jsonMessageConverter() {
        return new Jackson2JsonMessageConverter();
    }

    @Bean
    public RabbitTemplate rabbitTemplate(ConnectionFactory connectionFactory) {
        RabbitTemplate rabbitTemplate = new RabbitTemplate(connectionFactory);
        rabbitTemplate.setMessageConverter(jsonMessageConverter());
        rabbitTemplate.setRetryTemplate(retryTemplate());
        rabbitTemplate.setExchange(CDR_EXCHANGE);
        rabbitTemplate.setRoutingKey(CDR_ROUTING_KEY);
        log.info("Configured RabbitTemplate with exchange: {} and routing key: {}", CDR_EXCHANGE, CDR_ROUTING_KEY);
        return rabbitTemplate;
    }

    @Bean
    @Qualifier("hrsRabbitTemplate")
    public RabbitTemplate hrsRabbitTemplate(ConnectionFactory connectionFactory) {
        RabbitTemplate rabbitTemplate = new RabbitTemplate(connectionFactory);
        rabbitTemplate.setMessageConverter(jsonMessageConverter());
        rabbitTemplate.setRetryTemplate(retryTemplate());
        rabbitTemplate.setExchange(BRT_TO_HRS_EXCHANGE);
        rabbitTemplate.setRoutingKey(BRT_TO_HRS_ROUTING_KEY);
        log.info("Configured HRS RabbitTemplate with exchange: {} and routing key: {}", BRT_TO_HRS_EXCHANGE, BRT_TO_HRS_ROUTING_KEY);
        return rabbitTemplate;
    }

    @Bean
    public RetryTemplate retryTemplate() {
        RetryTemplate retryTemplate = new RetryTemplate();
        
        ExponentialBackOffPolicy backOffPolicy = new ExponentialBackOffPolicy();
        backOffPolicy.setInitialInterval(1000);
        backOffPolicy.setMultiplier(2.0);
        backOffPolicy.setMaxInterval(10000);
        retryTemplate.setBackOffPolicy(backOffPolicy);
        
        SimpleRetryPolicy retryPolicy = new SimpleRetryPolicy();
        retryPolicy.setMaxAttempts(5);
        retryTemplate.setRetryPolicy(retryPolicy);
        
        return retryTemplate;
    }

    @Bean
    public MessageListenerAdapter cdrListenerAdapter(MessageHandler messageHandler) {
        MessageListenerAdapter adapter = new MessageListenerAdapter(messageHandler, "handleCdrMessage");
        adapter.setMessageConverter(jsonMessageConverter());
        return adapter;
    }

    @Bean
    public MessageListenerAdapter hrsListenerAdapter(MessageHandler messageHandler) {
        MessageListenerAdapter adapter = new MessageListenerAdapter(messageHandler, "handleHrsResponse");
        adapter.setMessageConverter(jsonMessageConverter());
        return adapter;
    }

    @Bean
    public SimpleMessageListenerContainer cdrListenerContainer(
            ConnectionFactory connectionFactory,
            MessageListenerAdapter cdrListenerAdapter) {
        SimpleMessageListenerContainer container = new SimpleMessageListenerContainer();
        container.setConnectionFactory(connectionFactory);
        container.setQueueNames(CDR_QUEUE);
        container.setMessageListener(cdrListenerAdapter);
        container.setAutoStartup(true);
        container.setConcurrentConsumers(1);
        container.setDefaultRequeueRejected(false);
        container.setRecoveryInterval((long)5000);
        return container;
    }

    @Bean
    public SimpleMessageListenerContainer hrsListenerContainer(
            ConnectionFactory connectionFactory,
            MessageListenerAdapter hrsListenerAdapter) {
        SimpleMessageListenerContainer container = new SimpleMessageListenerContainer();
        container.setConnectionFactory(connectionFactory);
        container.setQueueNames(HRS_TO_BRT_QUEUE);
        container.setMessageListener(hrsListenerAdapter);
        container.setAutoStartup(true);
        container.setConcurrentConsumers(1);
        container.setDefaultRequeueRejected(false);
        container.setRecoveryInterval((long)5000);
        return container;
    }

    @Bean
    public SimpleRabbitListenerContainerFactory cdrListenerContainerFactory(ConnectionFactory connectionFactory) {
        SimpleRabbitListenerContainerFactory factory = new SimpleRabbitListenerContainerFactory();
        factory.setConnectionFactory(connectionFactory);
        factory.setMessageConverter(jsonMessageConverter());
        factory.setConcurrentConsumers(1);
        factory.setMaxConcurrentConsumers(1);
        factory.setDefaultRequeueRejected(false);
        factory.setRecoveryInterval((long)5000);
        return factory;
    }

    @Bean
    public SimpleRabbitListenerContainerFactory hrsListenerContainerFactory(ConnectionFactory connectionFactory) {
        SimpleRabbitListenerContainerFactory factory = new SimpleRabbitListenerContainerFactory();
        factory.setConnectionFactory(connectionFactory);
        factory.setMessageConverter(jsonMessageConverter());
        factory.setConcurrentConsumers(1);
        factory.setMaxConcurrentConsumers(1);
        factory.setDefaultRequeueRejected(false);
        factory.setRecoveryInterval((long)5000);
        return factory;
    }

    @Bean
    public TaskScheduler taskScheduler() {
        ThreadPoolTaskScheduler scheduler = new ThreadPoolTaskScheduler();
        scheduler.setPoolSize(1);
        scheduler.setThreadNamePrefix("test-scheduler-");
        scheduler.setWaitForTasksToCompleteOnShutdown(true);
        scheduler.setAwaitTerminationSeconds(30);
        scheduler.initialize();
        return scheduler;
    }
} 