package com.nsdev.lettergenerator.internal.render.fop;

/**
 * Typed engine diagnostic separating body pagination overflow from terminal failures.
 *
 * @param message human-readable explanation, never used to decide retry eligibility
 * @param bodyOverflow whether this problem is exclusively recoverable body-region overflow
 */
public record RenderProblem(String message, boolean bodyOverflow) {}
