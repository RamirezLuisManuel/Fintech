package com.proyecto.servicios.model.gestopago;

import lombok.Builder;
import lombok.Data;

import java.math.BigDecimal;

@Data
@Builder
public class GestoPagoTxResponse {
    private String codigo;
    private String texto;
    private String idTx;
    private String numeroAutorizacion;
    private BigDecimal saldo;
    private BigDecimal comision;
    private BigDecimal monto;
    private BigDecimal saldoReferencia;
    private String referencia;
    private String pin;
    private String legend;

    public boolean isSuccessfulSendTx() {
        return "01".equals(codigo);
    }

    public boolean isConfirmedAsApplied() {
        return "06".equals(codigo);
    }

    public boolean isConfirmedAsNotApplied() {
        return "70".equals(codigo);
    }
}
