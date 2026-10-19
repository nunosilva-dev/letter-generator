package com.nsdev.lettergenerator.component;

import com.nsdev.lettergenerator.internal.util.ImmutableLists;
import com.nsdev.lettergenerator.model.block.Block;
import com.nsdev.lettergenerator.model.block.Paragraph;
import com.nsdev.lettergenerator.model.text.TextRun;
import java.util.Arrays;
import java.util.List;

/**
 * Reusable immutable content registered on an individual document. References expand into ordinary
 * groups before validation, retaining the group's soft keep unless explicitly overridden.
 * Components may contain references to other registrations; generation rejects unknown names and
 * cycles. No filesystem, network access or executable inclusion occurs.
 *
 * @param blocks ordered component content, snapshotted while preserving invalid entries
 */
public record LetterComponent(List<Block> blocks) {
    /**
     * Snapshots content without validating it.
     *
     * @param blocks ordered blocks, or null for generation-time validation
     */
    public LetterComponent {
        blocks = ImmutableLists.copyNullable(blocks);
    }

    /**
     * Groups model blocks for document-local registration.
     *
     * @param blocks ordered reusable blocks
     * @return immutable component
     */
    public static LetterComponent of(Block... blocks) {
        return new LetterComponent(blocks == null ? null : Arrays.asList(blocks));
    }

    /**
     * Creates one ordinary paragraph per supplied literal string, suitable for address lines.
     *
     * @param paragraphs ordered literal paragraph text
     * @return immutable component without local style overrides
     */
    public static LetterComponent text(String... paragraphs) {
        return new LetterComponent(
                paragraphs == null
                        ? null
                        : Arrays.stream(paragraphs)
                                .map(
                                        value ->
                                                (Block)
                                                        new Paragraph(
                                                                List.of(
                                                                        new TextRun(
                                                                                value, null, null)),
                                                                null))
                                .toList());
    }
}
