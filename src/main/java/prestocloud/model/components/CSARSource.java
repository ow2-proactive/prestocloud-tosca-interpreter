package prestocloud.model.components;

/**
 * Enumeration of csars sources.
 */
public enum CSARSource {
    /** Out of the box archives. */
    ARCHIVE,
    /** Orchestrator archives. */
    ORCHESTRATOR,
    /** Generated from topology substitution. */
    TOPOLOGY_SUBSTITUTION,
    /** Manual upload. */
    UPLOAD,
    /** Git import. */
    GIT,
    /** Archive embedded in plugin. */
    PLUGIN,
    /** Other source. */
    OTHER
}
