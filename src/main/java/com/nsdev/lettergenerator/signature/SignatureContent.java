package com.nsdev.lettergenerator.signature;

import com.nsdev.lettergenerator.model.block.Block;
import java.util.List;

/** Immutable visual signature content, stacked in the order supplied to the letter builder. */
public sealed interface SignatureContent permits TextSignature, ImageSignature {
    List<Block> blocks();
}
