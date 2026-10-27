package co.com.bancolombia.model.documento.gateways;

import co.com.bancolombia.model.documento.Documento;
import co.com.bancolombia.model.documento.DocumentoResultado;

import java.util.List;

public interface AlmacenamientoGateway {
    List<DocumentoResultado> almacenar(List<Documento> documentos);
}
