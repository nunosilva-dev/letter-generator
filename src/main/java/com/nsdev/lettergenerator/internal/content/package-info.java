/**
 * Immediate conversion of application text and image bytes into immutable model blocks. {@link
 * com.nsdev.lettergenerator.internal.content.PlainText} normalizes line endings and separates
 * paragraphs/page breaks. {@link com.nsdev.lettergenerator.internal.content.ImageBlocks} decodes
 * supported PNG/JPEG/static SVG bytes and fits their aspect ratio into millimeter bounds. {@link
 * com.nsdev.lettergenerator.internal.content.ImageFormat} owns the supported reader names, exact
 * MIME labels and private encoded-byte signatures shared with generation-time validation.
 *
 * <p>These helpers are shared by Java builders, signature parts, watermark factories and markup
 * readers. They retain no builder state. SVG parsing is secure and bounded; the isolated SVG
 * watermark rasterization helper uses Batik, while native image resources retain vector geometry
 * where supported. Deferred content that needs final region settings belongs to {@link
 * com.nsdev.lettergenerator.internal.builder.content} instead.
 */
package com.nsdev.lettergenerator.internal.content;
