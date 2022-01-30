package booking.infrastructure.mercadopago;

import lombok.AllArgsConstructor;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.ToString;

@AllArgsConstructor(staticName = "of")
@NoArgsConstructor
@Getter
@ToString
public class MercadoPagoPaymentNotification {
    private String id;
    private String live_mode;
    private String type;
    private String date_created;
    private String application_id;
    private String user_id;
    private String version;
    private String api_version;
    private String action;
    private Data   data;

    @AllArgsConstructor(staticName = "of")
    @NoArgsConstructor
    @Getter
    @ToString
    public static class Data {
        private String id;
    }
}
