package com.nsdev.lettergenerator.internal.render.fop;

/** Diagnostic classification allows only the known last-page body-overflow recovery. */
public record RenderProblem(String message, boolean bodyOverflow) {}
