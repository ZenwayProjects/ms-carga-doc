package co.com.bancolombia.api;

import co.com.bancolombia.api.dto.ArchivoResultadoDto;
import co.com.bancolombia.api.dto.CargaMasivaResponse;
import co.com.bancolombia.model.documento.Documento;
import co.com.bancolombia.model.documento.DocumentoResultado;
import co.com.bancolombia.usecase.cargadocumentos.CargarDocumentosMasivamenteUseCase;
import lombok.RequiredArgsConstructor;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestPart;
import org.springframework.web.bind.annotation.RestController;
import org.springframework.web.multipart.MultipartFile;

import java.io.IOException;
import java.util.ArrayList;
import java.util.List;
import java.util.stream.Collectors;

@RestController
@RequestMapping(value = "/api/documentos", produces = MediaType.APPLICATION_JSON_VALUE)
@RequiredArgsConstructor
public class DocumentoController {

    private final CargarDocumentosMasivamenteUseCase cargarDocumentosMasivamenteUseCase;

    @PostMapping(value = "/carga-masiva", consumes = MediaType.MULTIPART_FORM_DATA_VALUE)
    public ResponseEntity<CargaMasivaResponse> cargarMasivamente(@RequestPart("files") List<MultipartFile> files) {
        if (files == null || files.isEmpty()) {
            return ResponseEntity.badRequest().body(CargaMasivaResponse.builder()
                    .total(0)
                    .exitosos(0)
                    .fallidos(0)
                    .resultados(List.of())
                    .build());
        }

        List<Documento> documentos = new ArrayList<>();
        for (MultipartFile file : files) {
            try {
                documentos.add(Documento.builder()
                        .nombreOriginal(file.getOriginalFilename())
                        .contentType(file.getContentType())
                        .tamanio(file.getSize())
                        .contenido(file.getBytes())
                        .build());
            } catch (IOException e) {
                documentos.add(Documento.builder()
                        .nombreOriginal(file.getOriginalFilename())
                        .contentType(file.getContentType())
                        .tamanio(file.getSize())
                        .contenido(null)
                        .build());
            }
        }

        List<DocumentoResultado> resultados = cargarDocumentosMasivamenteUseCase.ejecutar(documentos);

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
    }
}
