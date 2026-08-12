package com.yigit.requestms.common.config;

import com.vaadin.flow.server.ServiceInitEvent;
import com.vaadin.flow.server.VaadinServiceInitListener;
import com.yigit.requestms.common.exception.GlobalErrorHandler;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

@Configuration
public class ErrorHandlerConfig {

    @Bean
    public VaadinServiceInitListener errorHandlerInitializer() {
        return (ServiceInitEvent event) -> event.getSource()
                .addSessionInitListener(sessionInit ->
                        sessionInit.getSession().setErrorHandler(new GlobalErrorHandler()));
    }
}