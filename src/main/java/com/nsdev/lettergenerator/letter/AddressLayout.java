package com.nsdev.lettergenerator.letter;

import com.nsdev.lettergenerator.model.layout.Bounds;

/**
 * Optional freely positioned sender, recipient and date panel inside body flow. Bounds are relative
 * to the panel, not the physical paper; validation uses actual page width.
 *
 * @param sender sender rectangle, or null to keep sender in ordinary flow
 * @param recipient recipient rectangle, or null to keep recipient in ordinary flow
 * @param date date rectangle, or null to keep date in ordinary flow
 * @param heightMm panel height in millimeters
 */
public record AddressLayout(Bounds sender, Bounds recipient, Bounds date, double heightMm) {
    /** Default window panel height in millimeters. */
    private static final double WINDOW_HEIGHT_MM = 70;

    /** Sender rectangle in millimeters. */
    private static final Bounds SENDER_BOUNDS = new Bounds(0, 0, 85, 20);

    /** Recipient window rectangle in millimeters. */
    private static final Bounds RECIPIENT_BOUNDS = new Bounds(0, 25, 85, 40);

    /** Date rectangle in millimeters. */
    private static final Bounds DATE_BOUNDS = new Bounds(95, 0, 70, 15);

    /**
     * Supplies an editable example window-envelope arrangement without postal compliance claims.
     *
     * @return a sender/recipient/date panel for ordinary A4 letter margins
     */
    public static AddressLayout windowEnvelope() {
        return new AddressLayout(SENDER_BOUNDS, RECIPIENT_BOUNDS, DATE_BOUNDS, WINDOW_HEIGHT_MM);
    }
}
