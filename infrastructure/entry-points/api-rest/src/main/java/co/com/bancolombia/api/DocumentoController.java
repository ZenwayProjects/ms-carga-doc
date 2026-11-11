package co.com.bancolombia.api;

import co.com.bancolombia.api.dto.ArchivoResultadoDto;
import co.com.bancolombia.api.dto.CargaMasivaResponse;
import co.com.bancolombia.api.dto.DocumentoDto;
import co.com.bancolombia.api.dto.DocumentosResponse;
import co.com.bancolombia.model.documento.Documento;
import co.com.bancolombia.usecase.cargadocumentos.CargarDocumentosMasivamenteUseCase;
import co.com.bancolombia.usecase.consultardocumentos.ListarDocumentosUseCase;
import co.com.bancolombia.usecase.consultardocumentos.ObtenerDocumentoUseCase;
import lombok.RequiredArgsConstructor;
import org.springframework.core.io.buffer.DataBufferUtils;
import org.springframework.http.ContentDisposition;
import org.springframework.http.HttpHeaders;
import org.springframework.http.MediaType;
import org.springframework.http.MediaTypeFactory;
import org.springframework.http.ResponseEntity;
import org.springframework.http.codec.multipart.FilePart;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestPart;
import org.springframework.web.bind.annotation.RestController;
import reactor.core.publisher.Flux;
import reactor.core.publisher.Mono;
import reactor.core.scheduler.Schedulers;

import java.nio.charset.StandardCharsets;
import java.util.List;
import java.util.stream.Collectors;

@RestController
@RequestMapping(value = "/api/documentos")
@RequiredArgsConstructor
public class DocumentoController {

    private final CargarDocumentosMasivamenteUseCase cargarDocumentosMasivamenteUseCase;
    private final ObtenerDocumentoUseCase obtenerDocumentoUseCase;
    private final ListarDocumentosUseCase listarDocumentosUseCase;

    @PostMapping(value = "/usuarios/{usuarioId}/{categoria}/carga-masiva",
            consumes = MediaType.MULTIPART_FORM_DATA_VALUE,
            produces = MediaType.APPLICATION_JSON_VALUE)
    public Mono<ResponseEntity<CargaMasivaResponse>> cargarMasivamente(
            @RequestPart("files") Flux<FilePart> files,
            @PathVariable("usuarioId") String usuarioId,
            @PathVariable("categoria") String categoria) {
        Flux<Documento> documentos = files.flatMap(filePart -> toDocumento(filePart, usuarioId, categoria));
        return cargarDocumentosMasivamenteUseCase.ejecutar(documentos)
                .collectList()
                .map(resultados -> {
                    List<ArchivoResultadoDto> dtos = resultados.stream()
                            .map(r -> ArchivoResultadoDto.builder()
                                    .nombreOriginal(r.getNombreOriginal())
                                    .exitoso(r.isExitoso())
                                    .rutaAlmacenada(r.getRutaAlmacenada())
                                    .mensajeError(r.getMensajeError())
                                    .build())
                            .collect(Collectors.toList());

                    long exitosos = dtos.stream().filter(ArchivoResultadoDto::isExitoso).count();
                    long fallidos = dtos.size() - exitosos;

                    return ResponseEntity.ok(CargaMasivaResponse.builder()
                            .total(dtos.size())
                            .exitosos((int) exitosos)
                            .fallidos((int) fallidos)
                            .resultados(dtos)
                            .build());
                });
    }

    @GetMapping(value = "/usuarios/{usuarioId}/{categoria}",
            produces = MediaType.APPLICATION_JSON_VALUE)
    public Mono<ResponseEntity<DocumentosResponse>> listar(
            @PathVariable("usuarioId") String usuarioId,
            @PathVariable("categoria") String categoria) {
        return listarDocumentosUseCase.ejecutar(usuarioId, categoria)
                .map(documento -> toDto(usuarioId, categoria, documento))
                .collectList()
                .map(archivos -> ResponseEntity.ok(DocumentosResponse.builder()
                        .usuarioId(usuarioId)
                        .categoria(categoria)
                        .total(archivos.size())
                        .archivos(archivos)
                        .build()));
    }

    @GetMapping("/usuarios/{usuarioId}/{categoria}/{nombreArchivo}")
    public Mono<ResponseEntity<byte[]>> descargar(
            @PathVariable("usuarioId") String usuarioId,
            @PathVariable("categoria") String categoria,
            @PathVariable("nombreArchivo") String nombreArchivo) {
        return obtenerDocumentoUseCase.ejecutar(usuarioId, categoria, nombreArchivo)
                .map(documento -> {
                    byte[] contenido = documento.getContenido() != null ? documento.getContenido() : new byte[0];
                    String nombreOriginal = documento.getNombreOriginal() != null
                            ? documento.getNombreOriginal() : nombreArchivo;
                    return ResponseEntity.ok()
                            .contentType(resolverContentType(documento))
                            .header(HttpHeaders.CONTENT_DISPOSITION, ContentDisposition.attachment()
                                    .filename(nombreOriginal, StandardCharsets.UTF_8)
                                    .build()
                                    .toString())
                            .contentLength(contenido.length)
                            .body(contenido);
                });
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
        if (documento.getContentType() != null && !documento.getContentType().isBlank()) {
            try {
                return MediaType.parseMediaType(documento.getContentType());
            } catch (IllegalArgumentException ignored) {
                // Se resuelve por extensión más abajo.
            }
        }
        String nombre = documento.getNombreOriginal() != null
                ? documento.getNombreOriginal() : documento.getNombreAlmacenado();
        return MediaTypeFactory.getMediaType(nombre).orElse(MediaType.APPLICATION_OCTET_STREAM);
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
                            .contentType(filePart.headers().getContentType() != null ? filePart.headers().getContentType().toString() : null)
                            .tamanio(bytes.length)
                            .contenido(bytes)
                            .build();
                });
    }
}
