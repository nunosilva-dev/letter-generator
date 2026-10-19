package com.nsdev.lettergenerator.builder;

import com.nsdev.lettergenerator.model.block.Block;
import com.nsdev.lettergenerator.model.layout.Bounds;
import com.nsdev.lettergenerator.model.layout.FixedElement;
import com.nsdev.lettergenerator.model.layout.Margins;
import com.nsdev.lettergenerator.model.layout.PageSelector;
import com.nsdev.lettergenerator.model.layout.PageSize;
import com.nsdev.lettergenerator.model.style.Style;
import com.nsdev.lettergenerator.model.style.TextAlign;
import com.nsdev.lettergenerator.theme.Stationery;

/**
 * Convenience authoring of reusable stationery using the same geometry as a letter builder. Build
 * with the intended paper and margins; inherited rectangles remain explicit millimeters.
 */
public final class StationeryBuilder {
    /** Shared ordinary-region implementation, preventing divergent fitting rules. */
    private final LetterBuilder delegate =
            new LetterBuilder()
                    .theme(com.nsdev.lettergenerator.theme.LetterTheme.builder().build())
                    .body(" ");

    /** Creates an empty stationery builder. */
    public StationeryBuilder() {}

    /**
     * Configures header text.
     *
     * @param value header text
     * @return this builder
     */
    public StationeryBuilder header(String value) {
        delegate.header(value);
        return this;
    }

    /**
     * Configures footer text.
     *
     * @param value footer text
     * @return this builder
     */
    public StationeryBuilder footer(String value) {
        delegate.footer(value);
        return this;
    }

    /**
     * Configures header markup.
     *
     * @param value header markup
     * @return this builder
     */
    public StationeryBuilder headerHtml(String value) {
        delegate.headerHtml(value);
        return this;
    }

    /**
     * Configures footer markup.
     *
     * @param value footer markup
     * @return this builder
     */
    public StationeryBuilder footerHtml(String value) {
        delegate.footerHtml(value);
        return this;
    }

    /**
     * Configures header height in millimeters.
     *
     * @param value header height in millimeters
     * @return this builder
     */
    public StationeryBuilder headerHeightMm(double value) {
        delegate.headerHeightMm(value);
        return this;
    }

    /**
     * Configures footer height in millimeters.
     *
     * @param value footer height in millimeters
     * @return this builder
     */
    public StationeryBuilder footerHeightMm(double value) {
        delegate.footerHeightMm(value);
        return this;
    }

    /**
     * Configures header alignment.
     *
     * @param value header alignment
     * @return this builder
     */
    public StationeryBuilder headerAlignment(TextAlign value) {
        delegate.headerAlignment(value);
        return this;
    }

    /**
     * Configures exact inner-page header rectangle.
     *
     * @param value exact inner-page header rectangle
     * @return this builder
     */
    public StationeryBuilder headerBounds(Bounds value) {
        delegate.headerBoundsMm(value.x(), value.y(), value.width(), value.height());
        return this;
    }

    /**
     * Configures authoring paper size.
     *
     * @param value authoring paper size
     * @return this builder
     */
    public StationeryBuilder page(PageSize value) {
        delegate.page(value);
        return this;
    }

    /**
     * Configures authoring margins.
     *
     * @param value authoring margins
     * @return this builder
     */
    public StationeryBuilder margins(Margins value) {
        delegate.margins(value);
        return this;
    }

    /**
     * Configures encoded image bytes.
     *
     * @param value encoded image bytes
     * @return this builder
     */
    public StationeryBuilder logo(byte[] value) {
        delegate.logo(value);
        return this;
    }

    /**
     * Supplies a named image for deferred header/footer markup.
     *
     * @param name local asset key
     * @param bytes encoded image snapshot
     * @return this builder
     */
    public StationeryBuilder asset(String name, byte[] bytes) {
        delegate.asset(name, bytes);
        return this;
    }

    /**
     * Sets common header typography.
     *
     * @param value style overrides
     * @return this builder
     */
    public StationeryBuilder headerStyle(Style value) {
        delegate.headerStyle(value);
        return this;
    }

    /**
     * Sets common footer typography.
     *
     * @param value style overrides
     * @return this builder
     */
    public StationeryBuilder footerStyle(Style value) {
        delegate.footerStyle(value);
        return this;
    }

    /**
     * Supplies explicit fixed content, with its own identifier and page selector.
     *
     * @param value fixed region
     * @return this builder
     */
    public StationeryBuilder fixedElement(FixedElement value) {
        delegate.fixedElement(value);
        return this;
    }

    /**
     * Replaces one header variant with immutable rich blocks.
     *
     * @param pages selected pages
     * @param blocks content in reading order
     * @return this builder
     */
    public StationeryBuilder header(PageSelector pages, Block... blocks) {
        delegate.headerBlocks(pages, blocks);
        return this;
    }

    /**
     * Replaces one footer variant with immutable rich blocks.
     *
     * @param pages selected pages
     * @param blocks content in reading order
     * @return this builder
     */
    public StationeryBuilder footer(PageSelector pages, Block... blocks) {
        delegate.footerBlocks(pages, blocks);
        return this;
    }

    /**
     * Resolves regions and owned assets into an immutable reusable snapshot.
     *
     * @return stationery containing no dummy body content
     */
    public Stationery build() {
        var model = delegate.build();
        return new Stationery(
                model.fixedElements(), model.page().topZoneMm(), model.page().bottomZoneMm());
    }
}
