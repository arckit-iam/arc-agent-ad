package com.instantaccessprovider.arcagentad.infrastructure.kafka;

import com.instantaccessprovider.arcagentad.application.ProvisioningCommandProcessor;
import org.springframework.kafka.annotation.KafkaListener;
import org.springframework.kafka.support.KafkaHeaders;
import org.springframework.messaging.handler.annotation.Header;
import org.springframework.stereotype.Component;

@Component
public class ProvisioningKafkaListener {

    private final ProvisioningCommandProcessor processor;

    public ProvisioningKafkaListener(ProvisioningCommandProcessor processor) {
        this.processor = processor;
    }

    @KafkaListener(topics = {
            "${arc.agent.topics.identity}",
            "${arc.agent.topics.assignment}",
            "${arc.agent.topics.entitlement}"
    })
    public void onMessage(String payload, @Header(KafkaHeaders.RECEIVED_TOPIC) String topic) {
        processor.process(payload, topic);
    }
}
