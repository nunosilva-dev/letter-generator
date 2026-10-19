package com.nsdev.lettergenerator.model.layout;

/**
 * Custom physical paper dimensions, validated during generation.
 *
 * @param widthMm positive finite edge in millimeters
 * @param heightMm positive finite edge in millimeters
 */
public record PageDimensions(double widthMm, double heightMm) {}
