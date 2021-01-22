package se.itello.example.payments;

import java.io.IOException;
import java.math.BigDecimal;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.text.ParseException;
import java.util.List;

public class InbetalningstjanstenPaymentFileProcessor implements PaymentFileProcessor {
    private static final String VALID_FILENAME_END = "_inbetalningstjansten.txt";
    private static final int VALID_LINE_LENGTH = 80;
    private static final String VALID_STRING_REGEX = "[A-ZÅÄÖ0-9]*\s*";
    private static final String VALID_NUMBER_REGEX = "\\d+";

    @Override
    public boolean processFile(Path file, PaymentReceiver paymentReceiver) throws IOException, ParseException {
        if (file == null) {
            throw new IllegalArgumentException("The file can not be null.");
        } else if (!file.toString().endsWith(VALID_FILENAME_END)) {
            return false;
        } else if (paymentReceiver == null) {
            throw new IllegalArgumentException("The payment receiver can not be null.");
        } else {
            List<String> lines = Files.readAllLines(file, StandardCharsets.ISO_8859_1);
            validateSyntax(lines);
            validateSemantics(lines);
            parseFile(lines, paymentReceiver);
            return true;
        }
    }

    private void validateSyntax(List<String> lines) throws ParseException {
        if (lines.size() < 3) {
            throw new ParseException("The input file does not contain at least 3 lines.", 0);
        }

        String openingPost = lines.get(0);

        if (openingPost.length() != VALID_LINE_LENGTH) {
            throw new ParseException("Opening post has an invalid length: " + openingPost.length(), 0);
        }

        validate(openingPost, 0, 2, "00", "Opening post has invalid type syntax");
        validate(openingPost, 10, 14, VALID_NUMBER_REGEX, "Opening post has invalid clearing number syntax");
        validate(openingPost, 14, 24, VALID_NUMBER_REGEX, "Opening post has invalid account number syntax");

        for (int i = 1; i < lines.size() - 1; i++) {
            String paymentPost = lines.get(i);

            if (paymentPost.length() != VALID_LINE_LENGTH) {
                throw new ParseException("Payment post has an invalid length: " + paymentPost.length(), 0);
            }

            validate(paymentPost, 0, 2, "30", "Payment post has invalid type syntax");
            validate(paymentPost, 2, 22, VALID_NUMBER_REGEX, "Payment post has invalid amount syntax");
            validate(paymentPost, 40, 65, VALID_STRING_REGEX, "Payment post has invalid reference syntax");
        }

        String closingPost = lines.get(lines.size() - 1);

        if (closingPost.length() != VALID_LINE_LENGTH) {
            throw new ParseException("Closing post has an invalid length: " + closingPost.length(), 0);
        }

        validate(closingPost, 0, 2, "99", "Closing post has invalid type syntax");
        validate(closingPost, 2, 22, VALID_NUMBER_REGEX, "Closing post has invalid sum syntax");
        validate(closingPost, 30, 38, VALID_NUMBER_REGEX, "Closing post has invalid count syntax");
    }

    private void validateSemantics(List<String> lines) throws ParseException {
        String closingPost = lines.get(lines.size() - 1);
        int closingPostCount = Integer.parseInt(closingPost.substring(30, 38).trim());

        if (closingPostCount != lines.size() - 2) {
            throw new ParseException("Closing post count does not match number of payment lines: " + closingPostCount, 30);
        }

        BigDecimal paymentPostsSum = BigDecimal.ZERO;

        for (int i = 1; i < lines.size() - 1; i++) {
            String paymentPost = lines.get(i);
            BigDecimal paymentPostAmount = new BigDecimal(paymentPost.substring(1, 22)).movePointLeft(2);
            paymentPostsSum = paymentPostsSum.add(paymentPostAmount);
        }

        BigDecimal closingPostSum = new BigDecimal(closingPost.substring(2, 22)).movePointLeft(2);

        if (closingPostSum.compareTo(paymentPostsSum) != 0) {
            throw new ParseException("Closing post sum does not match payment amounts: " + closingPostSum, 16);
        }
    }

    private void validate(String string, int indexStart, int indexEnd, String regex, String errorMessage) throws ParseException {
        if (!string.substring(indexStart, indexEnd).matches(regex)) {
            throw new ParseException(errorMessage + ": " + string.substring(indexStart, indexEnd), indexStart);
        }
    }

    private void parseFile(List<String> lines, PaymentReceiver paymentReceiver) {
        String openingPost = lines.get(0);
        // Note that this clearing + account number format is assumed and that it retains leading zeroes.
        String accountNumber = openingPost.substring(10, 14) + " " + openingPost.substring(14, 24);
        // Note that this file format doesn't contain date or currency. It's assumed that the receiver can handle null values.
        paymentReceiver.startPaymentBundle(accountNumber, null, null);

        for (int i = 1; i < lines.size() - 1; i++) {
            String paymentPost = lines.get(i);
            BigDecimal amount = new BigDecimal(paymentPost.substring(2, 22)).movePointLeft(2);
            String reference = paymentPost.substring(40, 65);
            paymentReceiver.payment(amount, reference);
        }

        paymentReceiver.endPaymentBundle();
    }
}
