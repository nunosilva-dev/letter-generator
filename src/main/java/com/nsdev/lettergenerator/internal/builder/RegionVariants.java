package com.nsdev.lettergenerator.internal.builder;

import com.nsdev.lettergenerator.internal.builder.content.ContentPart;
import com.nsdev.lettergenerator.internal.util.ArgumentChecks;
import com.nsdev.lettergenerator.model.block.Block;
import com.nsdev.lettergenerator.model.block.GroupBlock;
import com.nsdev.lettergenerator.model.layout.Keep;
import com.nsdev.lettergenerator.model.layout.PageSelector;
import com.nsdev.lettergenerator.model.style.Style;
import com.nsdev.lettergenerator.theme.LetterTheme;
import java.util.EnumMap;
import java.util.List;
import java.util.Map;

/**
 * Builder-owned deferred stationery variants, with ALL as a fallback for disjoint page selections.
 */
public final class RegionVariants {
    /** Latest content for each explicitly configured selection. */
    private final Map<PageSelector, ContentPart> content = new EnumMap<>(PageSelector.class);

    /** Explicit appearance overrides for individual selections. */
    private final Map<PageSelector, Style> styles = new EnumMap<>(PageSelector.class);

    /** Creates an empty region without activating any body reservation. */
    public RegionVariants() {}

    /**
     * Replaces one selection, retaining an explicit empty part as a suppression of ALL fallback.
     *
     * @param pages non-null selection
     * @param part non-null deferred content
     */
    public void set(PageSelector pages, ContentPart part) {
        content.put(ArgumentChecks.required(pages, "region pages"), part);
    }

    /**
     * Replaces appearance overrides for a selection without changing its content.
     *
     * @param pages non-null selection
     * @param style non-null appearance overrides
     */
    public void style(PageSelector pages, Style style) {
        styles.put(
                ArgumentChecks.required(pages, "region pages"),
                ArgumentChecks.required(style, "region style"));
    }

    /**
     * Resolves final assets/geometry into either ALL content or disjoint first/following variants.
     *
     * @param assets named image byte snapshots
     * @param width final available width in millimeters
     * @param height final available height in millimeters
     * @param defaults explicit inherited region appearance
     * @param fallback builtin region typography beneath theme roles
     * @return ordered immutable block lists keyed by disjoint page selectors
     */
    public Map<PageSelector, List<Block>> resolve(
            Map<String, byte[]> assets,
            double width,
            double height,
            Style defaults,
            Style fallback) {
        return resolve(assets, width, height, defaults, fallback, null);
    }

    /**
     * Resolves stationery markup against final region geometry and themed table appearance.
     *
     * @param assets named encoded image bytes
     * @param width available region width in millimeters
     * @param height image fitting height basis in millimeters
     * @param defaults explicit inherited region typography
     * @param fallback builtin region typography below theme roles
     * @param theme optional final document theme
     * @return immutable blocks keyed by disjoint physical-page selections
     */
    public Map<PageSelector, List<Block>> resolve(
            Map<String, byte[]> assets,
            double width,
            double height,
            Style defaults,
            Style fallback,
            LetterTheme theme) {
        var result = new EnumMap<PageSelector, List<Block>>(PageSelector.class);
        boolean variants =
                content.keySet().stream().anyMatch(key -> key != PageSelector.ALL)
                        || styles.keySet().stream().anyMatch(key -> key != PageSelector.ALL);
        List<PageSelector> selections =
                variants
                        ? List.of(PageSelector.FIRST, PageSelector.FOLLOWING)
                        : List.of(PageSelector.ALL);
        for (PageSelector pages : selections) {
            ContentPart part =
                    content.containsKey(pages) ? content.get(pages) : content.get(PageSelector.ALL);
            if (part != null) {
                Style style =
                        (defaults == null ? Style.plain() : defaults)
                                .overlay(styles.get(PageSelector.ALL))
                                .overlay(styles.get(pages));
                List<Block> blocks = part.resolve(assets, width, height, false, style, theme);
                result.put(
                        pages,
                        blocks.isEmpty() || fallback == null
                                ? blocks
                                : List.of(
                                        new GroupBlock(
                                                blocks,
                                                Style.builder().keepTogether(Keep.AUTO).build(),
                                                fallback)));
            }
        }
        return result;
    }
}
