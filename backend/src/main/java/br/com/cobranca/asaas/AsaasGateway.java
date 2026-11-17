package br.com.cobranca.asaas;

import java.time.LocalDate;

/**
 * Única porta de contato com o Asaas. O resto do sistema só conhece esta interface:
 * se um dia trocarmos de gateway (ou quisermos um falso nos testes), só esta fronteira muda.
 */
public interface AsaasGateway {

    record Cobranca(String id, String status, String invoiceUrl) {
    }

    record QrPix(String copiaECola, String imagemBase64) {
    }

    String criarCliente(String nome, String cpfCnpj);

    Cobranca criarCobrancaPix(String clienteId, long valorCentavos, LocalDate vencimento, String descricao,
                              String referenciaExterna);

    QrPix buscarQrPix(String cobrancaId);

    /** Somente sandbox: simula o recebimento do Pix (o Asaas então dispara o webhook de verdade). */
    void confirmarPagamentoNoSandbox(String cobrancaId);

    /** Status no Asaas: PENDING, RECEIVED, CONFIRMED, OVERDUE... */
    String consultarStatus(String cobrancaId);
}
