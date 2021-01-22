package se.itello.example.payments;

import java.io.IOException;
import java.nio.file.Path;
import java.text.ParseException;
import java.util.List;
import java.util.logging.Logger;

public class MultiTypePaymentFileProcessor implements PaymentFileProcessor {
    private static final Logger log = Logger.getLogger(MultiTypePaymentFileProcessor.class.getName());
    private final List<PaymentFileProcessor> specificProcessors;

    public MultiTypePaymentFileProcessor(List<PaymentFileProcessor> specificProcessors) {
        if (specificProcessors == null) {
            throw new IllegalArgumentException("The specific processor list can not be null.");
        } else if (specificProcessors.size() == 0) {
            log.finer("MultiTypePaymentFileProcessor constructed without any specific processors.");
        }

        this.specificProcessors = specificProcessors;
    }

    @Override
    public boolean processFile(Path file, PaymentReceiver paymentReceiver) throws IOException, ParseException {
        if (file == null) {
            throw new IllegalArgumentException("The file can not be null.");
        } else if (paymentReceiver == null) {
            throw new IllegalArgumentException("The payment receiver can not be null.");
        }

        for (PaymentFileProcessor processor : specificProcessors) {
            if (processor.processFile(file, paymentReceiver)) {
                return true;
            }
        }

        log.finer("No payment file processor available for " + file);
        return false;
    }
}
