package com.nsdev.lettergenerator.letter;

/**
 * A sender or recipient with optional organization and address.
 *
 * @param name display name, nullable when organization supplies the display identity
 * @param organization optional organization
 * @param address optional postal address
 */
public record Party(String name, String organization, Address address) {
    /**
     * Creates an individual party.
     *
     * @param name display name
     * @param address optional address
     * @return immutable party
     */
    public static Party of(String name, Address address) {
        return new Party(name, null, address);
    }

    /**
     * Creates an organization party.
     *
     * @param name organization name
     * @param address optional address
     * @return immutable party
     */
    public static Party company(String name, Address address) {
        return new Party(null, name, address);
    }
}
