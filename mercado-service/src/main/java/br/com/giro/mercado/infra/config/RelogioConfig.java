package br.com.giro.mercado.infra.config;

import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

import java.time.Clock;

@Configuration(proxyBeanMethods = false)
public class RelogioConfig {

    @Bean
    Clock relogio() {
        return Clock.systemUTC();
    }
}
