package com.twilio.raas.sql;

import java.util.Optional;
import java.util.Objects;

/**
 * Message object represents a Message from an active scanner. There are
 * multiple types defined in {@link MessageType} and each consumer is expected
 * handle each type. Types are
 * 1. ROW   -- contains a row
 * 2. CLOSE -- scanner is closing so shouldn't expect any more ROW
 * 3. ERROR -- scanner failed in some manner, contains a failure.
 *
 * When an ERROR message is received, the producer needs to produce a CLOSE
 * message just after. ERROR message is not terminal, only CLOSE is terminal
 *
 * We use this in a Queue as the only synchronization and shared state between a
 * {@link ScannerCallback} and {@link CalciteKuduEnumerable}. A Queue with
 * heterogeneous messages is the only solution we were able to make work. Using
 * a queue and another synchronization object did not work.
 */
public final class CalciteScannerMessage<T> {
    public enum MessageType {
        ROW, CLOSE, ERROR
    }
    public final MessageType type;
    public final Optional<T> row;
    public final Optional<Exception> failure;

    /**
     * Construct a Scanner Message that contains an exception. When this message
     * is consumed the consumer is _expected_ to throw the exception.
     *
     * @param failure  the exception this message represents
     * @throws {@link NullPointerException} when failure is null
     */
    public CalciteScannerMessage(Exception failure) {
        Objects.requireNonNull(failure);
        this.type = MessageType.ERROR;
        this.row = Optional.empty();
        this.failure = Optional.of(failure);
    }

    /**
     * Constructs a Scanner Message that contains a row. Row must by nonNull
     *
     * @param row  the T that should be consumed.
     * @throws {@link NullPointerException} when row is null
     */
    public CalciteScannerMessage(T row) {
        Objects.requireNonNull(row);
        this.type = MessageType.ROW;
        this.row = Optional.of(row);
        this.failure = Optional.empty();
    }

    /**
     * Construct a {@link MessageType.CLOSE} message to represent closing of the
     * scanner. The consumer is excepted to keep track of closed scanners and
     * should not expect more {@link MessageType.ROW} from the scanner.
     */
    public static <T> CalciteScannerMessage<T> createEndMessage() {
        return new CalciteScannerMessage<T>(MessageType.CLOSE);
    }

    private CalciteScannerMessage(MessageType type) {
        this.type = type;
        this.row = Optional.empty();
        this.failure = Optional.empty();
    }
}