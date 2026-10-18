package com.nsdev.lettergenerator.signature;

import com.nsdev.lettergenerator.model.block.Block;
import java.util.List;

/**
 * Immutable visual signature content accepted by {@link
 * com.nsdev.lettergenerator.builder.LetterBuilder#signature(SignatureContent...)}. Create text with
 * {@link TextSignature#of(String)} and PNG/JPEG artwork with {@link ImageSignature#of(byte[])}. The
 * letter builder stacks parts in argument order; placement and reserved height are configured on
 * the builder separately.
 */
public sealed interface SignatureContent permits TextSignature, ImageSignature {
    /**
     * Exposes the ordered immutable blocks represented by this signature part. Text parts produce
     * paragraphs; image parts produce one fitted image block.
     *
     * @return immutable signature blocks, in display order
     */
    List<Block> blocks();
}
