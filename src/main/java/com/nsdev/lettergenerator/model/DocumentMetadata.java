package com.nsdev.lettergenerator.model;

/**
 * Optional descriptive PDF metadata; null fields omit the corresponding document information.
 *
 * @param title document title
 * @param author document author
 * @param subject document subject
 * @param keywords application-supplied keyword text
 */
public record DocumentMetadata(String title, String author, String subject, String keywords) {}
