package com.nsdev.lettergenerator.template;

/**
 * One source occurrence in a template's input contract.
 *
 * @param name literal name or map path
 * @param path source selector and optional attribute
 */
public record TemplateRequirement(String name, String path) {}
