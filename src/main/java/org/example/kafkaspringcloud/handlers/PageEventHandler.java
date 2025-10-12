package org.example.kafkaspringcloud.handlers;

import lombok.AllArgsConstructor;
import org.example.kafkaspringcloud.events.PageEvent;
import org.springframework.cloud.stream.function.StreamBridge;
import org.springframework.context.annotation.Bean;
import org.springframework.stereotype.Component;

import java.util.Date;
import java.util.Random;
import java.util.function.Consumer;
import java.util.function.Supplier;

@AllArgsConstructor
@Component

public class PageEventHandler {
    @Bean
    public Consumer<PageEvent> pageEventConsumer() {
        return (input)->{
            System.out.println("*****************************");
            System.out.println(input.toString());
            System.out.println("*****************************");
        };
    }

    @Bean
    public Supplier<PageEvent> pageEventSupplier() {
        return ()->{
            return new PageEvent(Math.random()>0.5?"P1":"P2",Math.random()>0.5?"U1":"U2",new Date(),10+new Random().nextInt(10000));
        };
    }

}
