package io.github.bluething.playground.java.ingestionservice.config;

import jakarta.annotation.PreDestroy;
import org.apache.coyote.AbstractProtocol;
import org.apache.coyote.ProtocolHandler;
import org.springframework.boot.web.embedded.tomcat.TomcatServletWebServerFactory;
import org.springframework.boot.web.server.WebServerFactoryCustomizer;
import org.springframework.context.annotation.Configuration;

import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;

@Configuration
class VirtualThreadTomcatConfig implements WebServerFactoryCustomizer<TomcatServletWebServerFactory> {
    private ExecutorService virtualThreadExecutor;

    @Override
    public void customize(TomcatServletWebServerFactory factory) {
        factory.addConnectorCustomizers(connector -> {
            ProtocolHandler protocolHandler = connector.getProtocolHandler();
            if (protocolHandler instanceof AbstractProtocol<?> protocol) {
                virtualThreadExecutor = Executors.newVirtualThreadPerTaskExecutor();
                protocolHandler.setExecutor(virtualThreadExecutor);
            }
        });
    }

    @PreDestroy
    public void shutdownExecutor() {
        if (virtualThreadExecutor != null) {
            virtualThreadExecutor.shutdown();
        }
    }
}
