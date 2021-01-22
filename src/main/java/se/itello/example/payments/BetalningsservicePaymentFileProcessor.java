package se.itello.example.payments;

import java.io.IOException;
import java.math.BigDecimal;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.text.DateFormat;
import java.text.ParseException;
import java.text.SimpleDateFormat;
import java.util.Date;
import java.util.List;

public class BetalningsservicePaymentFileProcessor implements PaymentFileProcessor {
    private static final String VALID_FILENAME_END = "_betalningsservice.txt";
    private static final int VALID_OPENING_LINE_LENGTH = 51;
    private static final int VALID_PAYMENT_LINE_LENGTH = 50;
    private static final String VALID_STRING_REGEX = "[A-ZÅÄÖ0-9]*\s*";
    private static final String VALID_INTEGER_REGEX = "\s*\\d+";
    private static final String VALID_DECIMAL_REGEX = "\s*\\d+(,\\d+)?";
    private static final String VALID_DATE_REGEX = "\\d{8}";
    private static final String VALID_ACCOUNT_NUMBER_REGEX = "\\d+\\s\\d+\\s*";
    private static final DateFormat DATE_FORMAT = new SimpleDateFormat("yyyyMMdd");

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
        if (lines.size() < 2) {
            throw new ParseException("The input file does not contain at least 2 lines.", 0);
        }

        String openingPost = lines.get(0);

        if (openingPost.length() != VALID_OPENING_LINE_LENGTH) {
            throw new ParseException("Opening post has an invalid length: " + openingPost.length(), 0);
        }

        validate(openingPost, 0, 1, "O", "Opening post has invalid type syntax");
        validate(openingPost, 1, 16, VALID_ACCOUNT_NUMBER_REGEX, "Opening post has invalid account number syntax");
        validate(openingPost, 16, 30, VALID_DECIMAL_REGEX, "Opening post has invalid sum syntax");
        validate(openingPost, 30, 40, VALID_INTEGER_REGEX, "Opening post has invalid count syntax");
        validate(openingPost, 40, 48, VALID_DATE_REGEX, "Opening post has invalid date syntax");
        validate(openingPost, 48, 51, VALID_STRING_REGEX, "Opening post has invalid currency syntax");

        for (int i = 1; i < lines.size(); i++) {
            String paymentPost = lines.get(i);

            if (paymentPost.length() != VALID_PAYMENT_LINE_LENGTH) {
                throw new ParseException("Payment post has an invalid length: " + paymentPost.length(), 0);
            }

            validate(paymentPost, 0, 1, "B", "Payment post has invalid type syntax");
            validate(paymentPost, 1, 15, VALID_DECIMAL_REGEX, "Payment post has invalid amount syntax");
            validate(paymentPost, 15, 50, VALID_STRING_REGEX, "Payment post has invalid reference syntax");
        }
    }

    private void validateSemantics(List<String> lines) throws ParseException {
        String openingPost = lines.get(0);
        int openingPostCount = Integer.parseInt(openingPost.substring(30, 40).trim());

        if (openingPostCount != lines.size() - 1) {
            throw new ParseException("Opening post count does not match number of payment lines: " + openingPostCount, 30);
        }

        String openingPostDate = openingPost.substring(40, 48);

        try {
            DATE_FORMAT.parse(openingPostDate);
        } catch (ParseException e) {
            throw new ParseException("Opening post date could not be parsed: " + openingPostDate, 40);
        }

        BigDecimal paymentPostsSum = BigDecimal.ZERO;

        for (int i = 1; i < lines.size(); i++) {
            String paymentPost = lines.get(i);
            BigDecimal paymentPostAmount = new BigDecimal(paymentPost.substring(1, 15).trim().replace(',', '.'));
            paymentPostsSum = paymentPostsSum.add(paymentPostAmount);
        }

        BigDecimal openingPostSum = new BigDecimal(openingPost.substring(16, 30).trim().replace(',', '.'));

        if (openingPostSum.compareTo(paymentPostsSum) != 0) {
            throw new ParseException("Opening post sum does not match payment amounts: " + openingPostSum, 16);
        }

        if (openingPost.substring(48, 51).isBlank()) {
            throw new ParseException("Opening post currency is empty.", 48);
        }
    }

    private void validate(String string, int indexStart, int indexEnd, String regex, String errorMessage) throws ParseException {
        if (!string.substring(indexStart, indexEnd).matches(regex)) {
            throw new ParseException(errorMessage + ": " + string.substring(indexStart, indexEnd), indexStart);
        }
    }

    private void parseFile(List<String> lines, PaymentReceiver paymentReceiver) throws ParseException {
        String openingPost = lines.get(0);
        String accountNumber = openingPost.substring(1, 16).trim();
        Date paymentDate = DATE_FORMAT.parse(openingPost.substring(40, 48));
        String currency = openingPost.substring(48, 51).trim();
        paymentReceiver.startPaymentBundle(accountNumber, paymentDate, currency);

        for (int i = 1; i < lines.size(); i++) {
            String paymentPost = lines.get(i);
            BigDecimal amount = new BigDecimal(paymentPost.substring(1, 15).trim().replace(',', '.'));
            String reference = paymentPost.substring(15, 50);
            paymentReceiver.payment(amount, reference);
        }

        paymentReceiver.endPaymentBundle();
    }
}
