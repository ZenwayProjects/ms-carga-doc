package co.com.bancolombia.r2dbc.documento;

import co.com.bancolombia.model.documento.Documento;
import co.com.bancolombia.model.documento.gateways.DocumentoMetadatosRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Repository;
import reactor.core.publisher.Mono;

import java.time.LocalDateTime;

@Repository
@RequiredArgsConstructor
public class DocumentoMetadatosAdapter implements DocumentoMetadatosRepository {

    private final DocumentoReactiveRepository repository;

    @Override
    public Mono<Void> guardar(Documento documento) {
        return Mono.fromCallable(() -> toData(documento))
                .flatMap(data -> repository.insertarSiNoExiste(
                        data.getNombreAlmacenado(),
                        data.getUsuarioId(),
                        data.getCategoria(),
                        data.getNombreOriginal(),
                        data.getContentType(),
                        data.getTamanio(),
                        data.getRutaAlmacenada(),
                        data.getFechaCarga()))
                .then();
    }

    private DocumentoData toData(Documento documento) {
        if (documento == null) {
            throw new IllegalArgumentException("El documento es obligatorio");
        }
        requerido(documento.getUsuarioId(), "usuarioId");
        requerido(documento.getCategoria(), "categoria");
        requerido(documento.getNombreOriginal(), "nombreOriginal");
        requerido(documento.getNombreAlmacenado(), "nombreAlmacenado");
        requerido(documento.getRutaAlmacenada(), "rutaAlmacenada");

        return DocumentoData.builder()
                .nombreAlmacenado(documento.getNombreAlmacenado())
                .usuarioId(documento.getUsuarioId())
                .categoria(documento.getCategoria())
                .nombreOriginal(documento.getNombreOriginal())
                .contentType(documento.getContentType())
                .tamanio(documento.getTamanio())
                .rutaAlmacenada(documento.getRutaAlmacenada())
                .fechaCarga(documento.getFechaCarga() != null
                        ? documento.getFechaCarga() : LocalDateTime.now())
                .build();
    }

    private void requerido(String valor, String campo) {
        if (valor == null || valor.isBlank()) {
            throw new IllegalArgumentException("El campo '" + campo + "' es obligatorio");
        }
    }
}
