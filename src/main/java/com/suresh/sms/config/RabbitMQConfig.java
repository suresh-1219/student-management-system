package com.suresh.sms.config;

import org.springframework.amqp.core.Binding;
import org.springframework.amqp.core.BindingBuilder;
import org.springframework.amqp.core.Queue;
import org.springframework.amqp.core.QueueBuilder;
import org.springframework.amqp.core.TopicExchange;
import org.springframework.amqp.rabbit.config.SimpleRabbitListenerContainerFactory;
import org.springframework.amqp.rabbit.connection.ConnectionFactory;
import org.springframework.amqp.rabbit.core.RabbitTemplate;
import org.springframework.amqp.support.converter.Jackson2JsonMessageConverter;
import org.springframework.amqp.support.converter.MessageConverter;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.context.annotation.Profile;

/**
 * Declares the topic exchange, queue and dead-letter queue that {@code
 * EventPublisher} publishes {@code GradeRecordedEvent}/{@code
 * AttendanceRecordedEvent} messages to, and that {@code
 * NotificationListener} consumes from.
 *
 * <p>The main queue is set up with {@code x-dead-letter-exchange}/{@code
 * x-dead-letter-routing-key} pointing at the DLQ: a message the listener
 * rejects (see {@code NotificationListener} and the container factory's
 * {@code defaultRequeueRejected(false)} below) is routed there automatically
 * by RabbitMQ rather than requeued forever or silently dropped. Nothing
 * currently reads the DLQ - in a real deployment an operator would be
 * alerted on it filling up - but it exists so a malformed or permanently
 * failing message does not loop.
 *
 * <p>These are just declarations: Spring AMQP creates them idempotently on
 * startup if they do not already exist, and does nothing if they do.
 */
@Configuration
public class RabbitMQConfig {

    public static final String EVENTS_EXCHANGE = "sms.events.exchange";
    public static final String NOTIFICATIONS_QUEUE = "sms.notifications.queue";
    public static final String NOTIFICATIONS_DLQ = "sms.notifications.queue.dlq";

    public static final String GRADE_RECORDED_ROUTING_KEY = "grade.recorded";
    public static final String ATTENDANCE_RECORDED_ROUTING_KEY = "attendance.recorded";

    // The four @Bean methods below are Declarable (Queue/Exchange/Binding):
    // Spring AMQP's auto-configured RabbitAdmin declares every Declarable
    // bean against the broker on context refresh, REGARDLESS of the
    // listener's auto-startup setting - that declaration is what was
    // actually causing "Connection refused: localhost:5672" noise during
    // `mvnw test`, not the listener itself. None of the many @SpringBootTest
    // classes in this project need these declared (every test that touches
    // messaging does so against mocks - see EventPublisherTest and
    // NotificationListenerTest), so they are skipped entirely under the
    // "test" profile rather than left to fail noisily against a broker that
    // was never meant to be running there.

    @Bean
    @Profile("!test")
    public TopicExchange eventsExchange() {
        return new TopicExchange(EVENTS_EXCHANGE);
    }

    @Bean
    @Profile("!test")
    public Queue notificationsDeadLetterQueue() {
        return QueueBuilder.durable(NOTIFICATIONS_DLQ).build();
    }

    @Bean
    @Profile("!test")
    public Queue notificationsQueue() {
        return QueueBuilder.durable(NOTIFICATIONS_QUEUE)
                // The default exchange ("") routes directly to a queue named
                // after the routing key, so this sends a dead-lettered
                // message straight to NOTIFICATIONS_DLQ without a separate
                // dead-letter exchange to declare.
                .withArgument("x-dead-letter-exchange", "")
                .withArgument("x-dead-letter-routing-key", NOTIFICATIONS_DLQ)
                .build();
    }

    @Bean
    @Profile("!test")
    public Binding gradeRecordedBinding(Queue notificationsQueue, TopicExchange eventsExchange) {
        return BindingBuilder.bind(notificationsQueue)
                .to(eventsExchange)
                .with(GRADE_RECORDED_ROUTING_KEY);
    }

    @Bean
    @Profile("!test")
    public Binding attendanceRecordedBinding(Queue notificationsQueue, TopicExchange eventsExchange) {
        return BindingBuilder.bind(notificationsQueue)
                .to(eventsExchange)
                .with(ATTENDANCE_RECORDED_ROUTING_KEY);
    }

    @Bean
    public MessageConverter jsonMessageConverter() {
        return new Jackson2JsonMessageConverter();
    }

    @Bean
    public RabbitTemplate rabbitTemplate(ConnectionFactory connectionFactory,
            MessageConverter jsonMessageConverter) {
        RabbitTemplate template = new RabbitTemplate(connectionFactory);
        template.setMessageConverter(jsonMessageConverter);
        return template;
    }

    @Bean
    public SimpleRabbitListenerContainerFactory rabbitListenerContainerFactory(
            ConnectionFactory connectionFactory, MessageConverter jsonMessageConverter) {

        SimpleRabbitListenerContainerFactory factory = new SimpleRabbitListenerContainerFactory();
        factory.setConnectionFactory(connectionFactory);
        factory.setMessageConverter(jsonMessageConverter);
        // A listener that throws rejects the message instead of requeuing
        // it forever; combined with the queue's dead-letter arguments
        // above, a bad message ends up in the DLQ after one failed attempt
        // rather than blocking the queue in an infinite redelivery loop.
        factory.setDefaultRequeueRejected(false);
        return factory;
    }
}
