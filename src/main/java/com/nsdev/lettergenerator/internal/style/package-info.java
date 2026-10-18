/**
 * Resolves nullable model styles into effective rendering values. {@link
 * com.nsdev.lettergenerator.internal.style.ResolvedStyle} starts from paragraph or heading defaults
 * and overlays each non-null property of {@link com.nsdev.lettergenerator.model.style.Style}; null
 * means retain the inherited value. Validation and FO rendering share this calculation, including
 * the selected {@link com.nsdev.lettergenerator.font.FontVariant}. Font sizes are points, spacing
 * is millimeters, and line height is a unitless multiplier. This package resolves values;
 * validation of those values belongs to {@link com.nsdev.lettergenerator.internal.validation}.
 */
package com.nsdev.lettergenerator.internal.style;
