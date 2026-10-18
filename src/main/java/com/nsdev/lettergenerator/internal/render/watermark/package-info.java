/**
 * PDFBox watermark post-processing after accepted FOP layout. {@link
 * com.nsdev.lettergenerator.internal.render.watermark.WatermarkProcessor} consumes validated {@link
 * com.nsdev.lettergenerator.model.watermark.Watermark} settings and prepends content on selected
 * pages, placing text or images behind existing page content. Fonts and images are loaded from
 * supplied memory snapshots; PDF documents, font input streams and page content streams belong to
 * each call. The {@link com.nsdev.lettergenerator.internal.render.DocumentRenderer} coordinator
 * returns bytes only after this optional stage also succeeds.
 */
package com.nsdev.lettergenerator.internal.render.watermark;
