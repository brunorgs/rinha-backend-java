package com.rinha;

import com.rinha.model.Payment;
import org.springframework.aot.hint.RuntimeHints;
import org.springframework.aot.hint.RuntimeHintsRegistrar;
import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;
import org.springframework.context.annotation.ImportRuntimeHints;
import org.springframework.scheduling.annotation.EnableScheduling;

@EnableScheduling
@ImportRuntimeHints(Application.RinhaRuntimeHints.class)
@SpringBootApplication
public class Application {

    public static void main(String[] args) {
        SpringApplication.run(Application.class, args);
    }

     static class RinhaRuntimeHints implements RuntimeHintsRegistrar {

         @Override
         public void registerHints(RuntimeHints hints, ClassLoader classLoader) {
            hints.serialization().registerType(Payment.class);
         }
     }
}