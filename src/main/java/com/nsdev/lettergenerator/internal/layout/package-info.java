/**
 * Shared physical-page arithmetic and coordinate conversion. {@link
 * com.nsdev.lettergenerator.internal.layout.PageGeometry} derives inner dimensions from page size
 * and margins, subtracts top/bottom reservations for body height, and subtracts the signature
 * reservation for final-page body height. Calculation does not establish validity; input validation
 * checks that the derived usable dimensions remain positive.
 *
 * <p>{@link com.nsdev.lettergenerator.internal.layout.Units} connects millimeter model dimensions,
 * PDF points, CSS reference pixels, FOP millipoints, and normalized 1000-unit font metrics. These
 * scalar conversions apply no input validation or rounding. The shared geometry tolerance applies
 * only to floating-point edge comparisons.
 */
package com.nsdev.lettergenerator.internal.layout;
