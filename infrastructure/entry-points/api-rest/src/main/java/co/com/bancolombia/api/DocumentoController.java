package co.com.bancolombia.api;

import co.com.bancolombia.api.dto.ArchivoResultadoDto;
import co.com.bancolombia.api.dto.CargaMasivaResponse;
import co.com.bancolombia.model.documento.Documento;
import co.com.bancolombia.usecase.cargadocumentos.CargarDocumentosMasivamenteUseCase;
import lombok.RequiredArgsConstructor;
import org.springframework.core.io.buffer.DataBufferUtils;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.http.codec.multipart.FilePart;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestPart;
import org.springframework.web.bind.annotation.RestController;
import reactor.core.publisher.Flux;
import reactor.core.publisher.Mono;
import reactor.core.scheduler.Schedulers;

import java.util.List;
import java.util.stream.Collectors;

@RestController
@RequestMapping(value = "/api/documentos", produces = MediaType.APPLICATION_JSON_VALUE)
@RequiredArgsConstructor
public class DocumentoController {

    private final CargarDocumentosMasivamenteUseCase cargarDocumentosMasivamenteUseCase;

    @PostMapping(value = "/carga-masiva", consumes = MediaType.MULTIPART_FORM_DATA_VALUE)
    public Mono<ResponseEntity<CargaMasivaResponse>> cargarMasivamente(@RequestPart("files") Flux<FilePart> files) {
        Flux<Documento> documentos = files.flatMap(this::toDocumento);
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

    private Mono<Documento> toDocumento(FilePart filePart) {
        return DataBufferUtils.join(filePart.content())
                .publishOn(Schedulers.boundedElastic())
                .map(dataBuffer -> {
                    byte[] bytes = new byte[dataBuffer.readableByteCount()];
                    dataBuffer.read(bytes);
                    DataBufferUtils.release(dataBuffer);
                    return Documento.builder()
                            .nombreOriginal(filePart.filename())
                            .contentType(filePart.headers().getContentType() != null ? filePart.headers().getContentType().toString() : null)
                            .tamanio(bytes.length)
                            .contenido(bytes)
                            .build();
                });
    }
}
