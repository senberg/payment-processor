package se.itello.example.payments;

import java.io.IOException;
import java.net.URISyntaxException;
import java.nio.file.Path;
import java.text.ParseException;
import java.util.ArrayList;
import java.util.List;

public class TestApplication {
    public static void main(String[] args) throws IOException, ParseException, URISyntaxException {
        PaymentReceiver paymentReceiver = new LoggingPaymentReceiver();
        List<PaymentFileProcessor> specificProcessors = new ArrayList<>();
        specificProcessors.add(new BetalningsservicePaymentFileProcessor());
        specificProcessors.add(new InbetalningstjanstenPaymentFileProcessor());
        MultiTypePaymentFileProcessor generalPaymentFileProcessor = new MultiTypePaymentFileProcessor(specificProcessors);

        Path file1 = Path.of(ClassLoader.getSystemResource("Exempelfil_betalningsservice.txt").toURI());
        generalPaymentFileProcessor.processFile(file1, paymentReceiver);

        Path file2 = Path.of(ClassLoader.getSystemResource("Exempelfil_inbetalningstjansten.txt").toURI());
        generalPaymentFileProcessor.processFile(file2, paymentReceiver);
    }
}
