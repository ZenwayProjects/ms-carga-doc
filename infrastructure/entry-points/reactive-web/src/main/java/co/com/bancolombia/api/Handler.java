package co.com.bancolombia.api;

import co.com.bancolombia.api.dto.ArchivoResultadoDto;
import co.com.bancolombia.api.dto.CargaMasivaResponse;
import co.com.bancolombia.api.dto.DocumentoDto;
import co.com.bancolombia.api.dto.DocumentosResponse;
import co.com.bancolombia.api.dto.ErrorResponseDto;
import co.com.bancolombia.model.documento.Documento;
import co.com.bancolombia.model.documento.DocumentoNoEncontradoException;
import co.com.bancolombia.model.documento.DocumentoResultado;
import co.com.bancolombia.usecase.cargadocumentos.CargarDocumentosMasivamenteUseCase;
import co.com.bancolombia.usecase.consultardocumentos.ListarDocumentosUseCase;
import co.com.bancolombia.usecase.consultardocumentos.ObtenerDocumentoUseCase;
import lombok.RequiredArgsConstructor;
import org.springframework.core.io.ByteArrayResource;
import org.springframework.core.io.Resource;
import org.springframework.core.io.buffer.DataBufferUtils;
import org.springframework.http.ContentDisposition;
import org.springframework.http.HttpHeaders;
import org.springframework.http.HttpStatus;
import org.springframework.http.MediaType;
import org.springframework.http.MediaTypeFactory;
import org.springframework.http.codec.multipart.FilePart;
import org.springframework.http.codec.multipart.Part;
import org.springframework.stereotype.Component;
import org.springframework.web.reactive.function.server.ServerRequest;
import org.springframework.web.reactive.function.server.ServerResponse;
import reactor.core.publisher.Flux;
import reactor.core.publisher.Mono;
import reactor.core.scheduler.Schedulers;

import java.nio.charset.StandardCharsets;
import java.time.LocalDateTime;
import java.util.List;
import java.util.stream.Collectors;

@Component
@RequiredArgsConstructor
public class Handler {

    private final CargarDocumentosMasivamenteUseCase cargarDocumentosMasivamenteUseCase;
    private final ObtenerDocumentoUseCase obtenerDocumentoUseCase;
    private final ListarDocumentosUseCase listarDocumentosUseCase;

    public Mono<ServerResponse> listenPOSTCargarDocumentos(ServerRequest serverRequest) {
        String usuarioId = serverRequest.pathVariable("usuarioId");
        String categoria = serverRequest.pathVariable("categoria");

        Flux<Documento> documentos = serverRequest.multipartData()
                .flatMapMany(parts -> {
                    List<FilePart> archivos = parts.getOrDefault("files", List.of()).stream()
                            .filter(FilePart.class::isInstance)
                            .map(FilePart.class::cast)
                            .toList();
                    if (archivos.isEmpty()) {
                        return Flux.error(new IllegalArgumentException(
                                "Debe enviar al menos un archivo en el campo 'files'"));
                    }
                    return Flux.fromIterable(archivos)
                            .flatMap(filePart -> toDocumento(filePart, usuarioId, categoria));
                });

        return Flux.defer(() -> cargarDocumentosMasivamenteUseCase.ejecutar(documentos))
                .collectList()
                .map(this::toCargaMasivaResponse)
                .flatMap(response -> ServerResponse.ok()
                        .contentType(MediaType.APPLICATION_JSON)
                        .bodyValue(response))
                .onErrorResume(error -> manejarError(error, serverRequest));
    }

    public Mono<ServerResponse> listenGETListarDocumentos(ServerRequest serverRequest) {
        String usuarioId = serverRequest.pathVariable("usuarioId");
        String categoria = serverRequest.pathVariable("categoria");

        return Flux.defer(() -> listarDocumentosUseCase.ejecutar(usuarioId, categoria))
                .map(documento -> toDto(usuarioId, categoria, documento))
                .collectList()
                .map(archivos -> DocumentosResponse.builder()
                        .usuarioId(usuarioId)
                        .categoria(categoria)
                        .total(archivos.size())
                        .archivos(archivos)
                        .build())
                .flatMap(response -> ServerResponse.ok()
                        .contentType(MediaType.APPLICATION_JSON)
                        .bodyValue(response))
                .onErrorResume(error -> manejarError(error, serverRequest));
    }

    public Mono<ServerResponse> listenGETDescargarDocumento(ServerRequest serverRequest) {
        String usuarioId = serverRequest.pathVariable("usuarioId");
        String categoria = serverRequest.pathVariable("categoria");
        String nombreArchivo = serverRequest.pathVariable("nombreArchivo");

        return Mono.defer(() -> obtenerDocumentoUseCase.ejecutar(usuarioId, categoria, nombreArchivo))
                .flatMap(documento -> {
                    byte[] contenido = documento.getContenido() != null ? documento.getContenido() : new byte[0];
                    String nombreOriginal = documento.getNombreOriginal() != null
                            ? documento.getNombreOriginal() : nombreArchivo;
                    Resource recurso = new ByteArrayResource(contenido);
                    return ServerResponse.ok()
                            .contentType(resolverContentType(documento))
                            .header(HttpHeaders.CONTENT_DISPOSITION, ContentDisposition.attachment()
                                    .filename(nombreOriginal, StandardCharsets.UTF_8)
                                    .build()
                                    .toString())
                            .contentLength(contenido.length)
                            .bodyValue(recurso);
                })
                .onErrorResume(error -> manejarError(error, serverRequest));
    }

    private CargaMasivaResponse toCargaMasivaResponse(List<DocumentoResultado> resultados) {
        List<ArchivoResultadoDto> dtos = resultados.stream()
                .map(resultado -> ArchivoResultadoDto.builder()
                        .nombreOriginal(resultado.getNombreOriginal())
                        .exitoso(resultado.isExitoso())
                        .rutaAlmacenada(resultado.getRutaAlmacenada())
                        .mensajeError(resultado.getMensajeError())
                        .build())
                .collect(Collectors.toList());

        long exitosos = dtos.stream().filter(ArchivoResultadoDto::isExitoso).count();

        return CargaMasivaResponse.builder()
                .total(dtos.size())
                .exitosos((int) exitosos)
                .fallidos(dtos.size() - (int) exitosos)
                .resultados(dtos)
                .build();
    }

    private DocumentoDto toDto(String usuarioId, String categoria, Documento documento) {
        return DocumentoDto.builder()
                .nombreArchivo(documento.getNombreAlmacenado())
                .nombreOriginal(documento.getNombreOriginal())
                .contentType(documento.getContentType())
                .tamanio(documento.getTamanio())
                .rutaAlmacenada(documento.getRutaAlmacenada())
                .fechaCarga(documento.getFechaCarga())
                .urlDescarga("/api/documentos/usuarios/" + usuarioId + "/" + categoria + "/"
                        + documento.getNombreAlmacenado())
                .build();
    }

    private MediaType resolverContentType(Documento documento) {
        String nombre = documento.getNombreOriginal() != null
                ? documento.getNombreOriginal() : documento.getNombreAlmacenado();
        MediaType porExtension = MediaTypeFactory.getMediaType(nombre).orElse(null);

        MediaType contentType = null;
        if (documento.getContentType() != null && !documento.getContentType().isBlank()) {
            try {
                contentType = MediaType.parseMediaType(documento.getContentType());
            } catch (IllegalArgumentException ignored) {
                contentType = null;
            }
        }

        if (contentType == null || MediaType.APPLICATION_OCTET_STREAM.equals(contentType)) {
            contentType = porExtension != null ? porExtension : MediaType.APPLICATION_OCTET_STREAM;
        }
        if (MediaType.TEXT_PLAIN.equals(contentType)) {
            return new MediaType(MediaType.TEXT_PLAIN, StandardCharsets.UTF_8);
        }
        return contentType;
    }

    private Mono<Documento> toDocumento(FilePart filePart, String usuarioId, String categoria) {
        return DataBufferUtils.join(filePart.content())
                .publishOn(Schedulers.boundedElastic())
                .map(dataBuffer -> {
                    byte[] bytes = new byte[dataBuffer.readableByteCount()];
                    dataBuffer.read(bytes);
                    DataBufferUtils.release(dataBuffer);
                    return Documento.builder()
                            .nombreOriginal(filePart.filename())
                            .usuarioId(usuarioId)
                            .categoria(categoria)
                            .contentType(filePart.headers().getContentType() != null
                                    ? filePart.headers().getContentType().toString() : null)
                            .tamanio(bytes.length)
                            .contenido(bytes)
                            .build();
                });
    }

    private Mono<ServerResponse> manejarError(Throwable error, ServerRequest serverRequest) {
        HttpStatus status;
        if (error instanceof DocumentoNoEncontradoException) {
            status = HttpStatus.NOT_FOUND;
        } else if (error instanceof IllegalArgumentException) {
            status = HttpStatus.BAD_REQUEST;
        } else {
            return Mono.error(error);
        }

        ErrorResponseDto response = ErrorResponseDto.builder()
                .timestamp(LocalDateTime.now())
                .status(status.value())
                .error(status.getReasonPhrase())
                .message(error.getMessage())
                .path(serverRequest.path())
                .build();

        return ServerResponse.status(status)
                .contentType(MediaType.APPLICATION_JSON)
                .bodyValue(response);
    }
}
