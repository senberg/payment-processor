package se.itello.example.payments;

import java.io.IOException;
import java.nio.file.Path;
import java.text.ParseException;

public interface PaymentFileProcessor {
    /**
     * Validates and parses payments from a file and sends them to a payment receiver.
     *
     * @param file            the file to process.
     * @param paymentReceiver a receiver for payments.
     * @return returns true if this processor was suitable and processed the file, otherwise false.
     * @throws IOException    if the file contents could not be read.
     * @throws ParseException if the file did not validate.
     */
    boolean processFile(Path file, PaymentReceiver paymentReceiver) throws IOException, ParseException;
}
