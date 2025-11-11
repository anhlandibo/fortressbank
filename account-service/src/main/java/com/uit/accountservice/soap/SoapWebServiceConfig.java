package com.uit.accountservice.soap;

import org.springframework.boot.web.servlet.ServletRegistrationBean;
import org.springframework.context.ApplicationContext;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.core.io.ClassPathResource;
import org.springframework.ws.config.annotation.EnableWs;
import org.springframework.ws.config.annotation.WsConfigurationSupport;
import org.springframework.ws.server.EndpointInterceptor;
import org.springframework.ws.transport.http.MessageDispatcherServlet;
import org.springframework.ws.wsdl.wsdl11.DefaultWsdl11Definition;
import org.springframework.xml.xsd.SimpleXsdSchema;
import org.springframework.xml.xsd.XsdSchema;

import java.util.List;

/**
 * SOAP Web Services Configuration for FortressBank.
 * 
 * Exposes transfer operations via SOAP endpoints with:
 * - WS-Security (JWT validation via interceptor)
 * - SOAP fault handling
 * - Audit logging integration
 * - WSDL contract-first approach
 * 
 * Following PROJECT_REFERENCE.md guidance:
 * - SOAP for money movement (formal contracts, audit trails)
 * - REST for user-facing operations (modern, fast)
 */
@Configuration
@EnableWs
public class SoapWebServiceConfig extends WsConfigurationSupport {

    private final SoapSecurityInterceptor soapSecurityInterceptor;

    public SoapWebServiceConfig(SoapSecurityInterceptor soapSecurityInterceptor) {
        this.soapSecurityInterceptor = soapSecurityInterceptor;
    }

    /**
     * Register MessageDispatcherServlet for SOAP endpoints.
     * Serves WSDL and handles SOAP requests at /ws/*
     */
    @Bean
    public ServletRegistrationBean<MessageDispatcherServlet> messageDispatcherServlet(
            ApplicationContext applicationContext) {
        MessageDispatcherServlet servlet = new MessageDispatcherServlet();
        servlet.setApplicationContext(applicationContext);
        servlet.setTransformWsdlLocations(true);
        return new ServletRegistrationBean<>(servlet, "/ws/*");
    }

    /**
     * WSDL 1.1 Definition for Transfer Service.
     * Available at: http://localhost:8080/ws/transfer.wsdl
     */
    @Bean(name = "transfer")
    public DefaultWsdl11Definition transferWsdl11Definition(XsdSchema transferSchema) {
        DefaultWsdl11Definition wsdl11Definition = new DefaultWsdl11Definition();
        wsdl11Definition.setPortTypeName("TransferServicePort");
        wsdl11Definition.setLocationUri("/ws/transfer");
        wsdl11Definition.setTargetNamespace("http://fortressbank.com/services/transfer");
        wsdl11Definition.setSchema(transferSchema);
        return wsdl11Definition;
    }

    /**
     * XSD Schema for Transfer Service contract.
     */
    @Bean
    public XsdSchema transferSchema() {
        return new SimpleXsdSchema(new ClassPathResource("xsd/transfer.xsd"));
    }

    /**
     * Register JWT security interceptor for all SOAP endpoints.
     * Validates JWT in SOAP Security header before endpoint execution.
     */
    @Override
    public void addInterceptors(List<EndpointInterceptor> interceptors) {
        interceptors.add(soapSecurityInterceptor);
    }

    /**
     * SOAP Fault Exception Resolver.
     * Converts Java exceptions into standardized SOAP faults.
     */
    @Bean
    public SoapFaultHandler soapFaultHandler() {
        SoapFaultHandler handler = new SoapFaultHandler();
        handler.setOrder(1);
        return handler;
    }
}
