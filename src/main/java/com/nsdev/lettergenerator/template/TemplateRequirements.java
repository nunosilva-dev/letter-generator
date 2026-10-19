package com.nsdev.lettergenerator.template;

import java.util.List;

/**
 * Immutable syntactic requirements, including conditional branches and scoped aliases. Dynamic
 * asset/style/component references retain their literal placeholder text. This manifest describes
 * source occurrences; binding decides which conditional requirements are actually needed.
 *
 * @param variables text placeholders and parameter map paths
 * @param flags named boolean conditions
 * @param collections repeated list paths
 * @param assets named image references
 * @param styles named style references
 * @param components reusable component references
 */
public record TemplateRequirements(
        List<TemplateRequirement> variables,
        List<TemplateRequirement> flags,
        List<TemplateRequirement> collections,
        List<TemplateRequirement> assets,
        List<TemplateRequirement> styles,
        List<TemplateRequirement> components) {
    /**
     * Owns all occurrence lists.
     *
     * @param variables text/parameter occurrences
     * @param flags boolean occurrences
     * @param collections list occurrences
     * @param assets image references
     * @param styles style references
     * @param components component references
     */
    public TemplateRequirements {
        variables = List.copyOf(variables);
        flags = List.copyOf(flags);
        collections = List.copyOf(collections);
        assets = List.copyOf(assets);
        styles = List.copyOf(styles);
        components = List.copyOf(components);
    }
}
