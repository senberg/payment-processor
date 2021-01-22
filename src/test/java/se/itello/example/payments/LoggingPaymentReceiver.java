package se.itello.example.payments;


import java.math.BigDecimal;
import java.util.Date;
import java.util.logging.Logger;

public class LoggingPaymentReceiver implements PaymentReceiver {
    private static final Logger log = Logger.getLogger(LoggingPaymentReceiver.class.getName());

    @Override
    public void startPaymentBundle(String accountNumber, Date paymentDate, String currency) {
        log.info("LoggingPaymentReceiver startPaymentBundle");
        log.info("\taccountNumber " + accountNumber);
        log.info("\tpaymentDate " + paymentDate);
        log.info("\tcurrency " + currency);
    }

    @Override
    public void payment(BigDecimal amount, String reference) {
        log.info("LoggingPaymentReceiver payment");
        log.info("\tamount " + amount);
        log.info("\treference " + reference);
    }

    @Override
    public void endPaymentBundle() {
        log.info("LoggingPaymentReceiver endPaymentBundle");
    }
}
