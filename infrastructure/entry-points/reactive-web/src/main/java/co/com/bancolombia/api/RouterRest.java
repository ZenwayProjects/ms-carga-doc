package co.com.bancolombia.api;

import org.springdoc.core.annotations.RouterOperation;
import org.springdoc.core.annotations.RouterOperations;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.web.reactive.function.server.RouterFunction;
import org.springframework.web.reactive.function.server.ServerResponse;

import static org.springframework.web.reactive.function.server.RequestPredicates.GET;
import static org.springframework.web.reactive.function.server.RequestPredicates.POST;
import static org.springframework.web.reactive.function.server.RouterFunctions.route;

@Configuration
public class RouterRest {
    @RouterOperations({
            @RouterOperation(path = "/api/documentos/usuarios/{usuarioId}/{categoria}/carga-masiva",
                    beanClass = Handler.class, beanMethod = "listenPOSTCargarDocumentos"),
            @RouterOperation(path = "/api/documentos/usuarios/{usuarioId}/{categoria}",
                    beanClass = Handler.class, beanMethod = "listenGETListarDocumentos"),
            @RouterOperation(path = "/api/documentos/usuarios/{usuarioId}/{categoria}/{nombreArchivo}",
                    beanClass = Handler.class, beanMethod = "listenGETDescargarDocumento")
    })
    @Bean
    public RouterFunction<ServerResponse> routerFunction(Handler handler) {
        return route(POST("/api/documentos/usuarios/{usuarioId}/{categoria}/carga-masiva"),
                handler::listenPOSTCargarDocumentos)
                .andRoute(GET("/api/documentos/usuarios/{usuarioId}/{categoria}"),
                        handler::listenGETListarDocumentos)
                .andRoute(GET("/api/documentos/usuarios/{usuarioId}/{categoria}/{nombreArchivo}"),
                        handler::listenGETDescargarDocumento);
    }
}
