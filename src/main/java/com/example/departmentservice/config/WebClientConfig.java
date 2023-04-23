package com.example.departmentservice.config;

import com.example.departmentservice.client.EmployeeClient;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.cloud.client.loadbalancer.reactive.LoadBalancedExchangeFilterFunction;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.web.reactive.function.client.WebClient;
import org.springframework.web.reactive.function.client.support.WebClientAdapter;
import org.springframework.web.service.invoker.HttpServiceProxyFactory;

@Configuration
public class WebClientConfig {

    /**
     * Load balanced http exchange
     */
    @Autowired
    private LoadBalancedExchangeFilterFunction filterFunction;

    @Bean
    public EmployeeClient employeeClient() {
        /**
         * To create a proxy using the provided factory, besides the HTTP interface,
         * we'll also require an instance of a reactive web client
         */
        WebClient webClient = WebClient.builder()
                .baseUrl("http://employee-service")
                .filter(filterFunction)
                .build();

        /**
         * Spring framework provides us with a HttpServiceProxyFactory that
         * we can use to generate a client proxy for our HTTP interface:
         */
        return HttpServiceProxyFactory.builder(WebClientAdapter.forClient(webClient))
                .build()
                .createClient(EmployeeClient.class);
    }
}
