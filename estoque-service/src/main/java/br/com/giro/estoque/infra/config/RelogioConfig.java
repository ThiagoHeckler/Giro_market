package br.com.giro.estoque.infra.config;

import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

import java.time.Clock;
import java.time.ZoneId;

@Configuration(proxyBeanMethods = false)
public class RelogioConfig {

    /** Instantes são sempre UTC; o fuso só pesa em datas civis, como a validade do lote. */
    @Bean
    Clock relogio() {
        return Clock.system(ZoneId.of("America/Sao_Paulo"));
    }
}
