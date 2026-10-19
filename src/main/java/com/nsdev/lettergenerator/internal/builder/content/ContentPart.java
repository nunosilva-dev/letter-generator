package com.nsdev.lettergenerator.internal.builder.content;

import com.nsdev.lettergenerator.model.block.Block;
import com.nsdev.lettergenerator.model.style.Style;
import com.nsdev.lettergenerator.theme.LetterTheme;
import java.util.List;
import java.util.Map;

/**
 * Deferred builder content resolved against the final region geometry, asset map and style. This
 * lets HTML reference assets registered later and lets region configuration be changed before
 * building. Resolution creates blocks for the immutable document snapshot.
 *
 * <p>The permitted package-private records each own one resolution strategy. Shared region-style
 * adaptation lives in {@link ContentPartStyles}, so this interface contains only the contract.
 * Instances are created through {@link ContentParts}. This type is public for use across library
 * packages and is not a supported application API.
 */
public sealed interface ContentPart
        permits ImageContentPart, BlockContentPart, TextContentPart, HtmlContentPart {
    /**
     * Resolves one stored content part for the destination region.
     *
     * @param assets final named image assets; implementations only read them
     * @param width available region width in millimeters
     * @param height available region height in millimeters
     * @param body true for body content, where explicit page breaks are allowed
     * @param defaults inherited region style, or null to preserve block styles unchanged
     * @return immutable ordered blocks for the region
     */
    List<Block> resolve(
            Map<String, byte[]> assets, double width, double height, boolean body, Style defaults);

    /**
     * Resolves content with final theme-aware geometry. Strategies that do not parse deferred
     * markup retain their original behavior through the existing resolution method.
     *
     * @param assets final named image bytes
     * @param width available region width in millimeters
     * @param height image fitting height basis in millimeters
     * @param body whether body page breaks are allowed
     * @param defaults nullable explicit inherited typography
     * @param theme optional final document theme
     * @return immutable ordered blocks
     */
    default List<Block> resolve(
            Map<String, byte[]> assets,
            double width,
            double height,
            boolean body,
            Style defaults,
            LetterTheme theme) {
        return resolve(assets, width, height, body, defaults);
    }
}
