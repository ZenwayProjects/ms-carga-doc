package co.com.bancolombia.config;

import co.com.bancolombia.model.documento.gateways.AlmacenamientoGateway;
import co.com.bancolombia.usecase.cargadocumentos.CargarDocumentosMasivamenteUseCase;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

@Configuration
public class UseCaseConfig {
    @Bean
    public CargarDocumentosMasivamenteUseCase cargarDocumentosMasivamenteUseCase(AlmacenamientoGateway almacenamientoGateway) {
        return new CargarDocumentosMasivamenteUseCase(almacenamientoGateway);
    }
}
