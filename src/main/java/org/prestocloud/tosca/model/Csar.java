package org.prestocloud.tosca.model;

import java.util.Date;
import java.util.List;
import java.util.Set;

import lombok.EqualsAndHashCode;
import lombok.Getter;
import lombok.Setter;
import prestocloud.exceptions.IndexingServiceException;
import prestocloud.model.common.IWorkspaceResource;
import prestocloud.model.common.Tag;
import prestocloud.tosca.parser.ParsingContextExecution;
import prestocloud.utils.version.Version;

@Getter
@Setter
@EqualsAndHashCode(of = { "name", "version" })
public class Csar implements IWorkspaceResource {

    private String name;

    private String version;

    private Version nestedVersion;

    private String workspace;

    /** This is the hashcode of all files in the archive. */

    private String hash;

    /** This is the hashcode of the definition file only (yaml). */

    private String definitionHash;

    /** Eventually the id of the application. */
    private String delegateId;

    /** Type of delegate if the archive is an application archive. */
    private String delegateType;

    /** Path of the yaml file in the archive (relative to the root). */
    private String yamlFilePath;

    private String toscaDefinitionsVersion;

    private String toscaDefaultNamespace;

    private String templateAuthor;

    private String description;

    private Set<CSARDependency> dependencies;

    private String license;

    /** Archive metadata. */
    private List<Tag> tags;

    /** Meta-data to know how the archive has been imported. */
    private String importSource;

    /** Date on which the archive has been imported or updated */
    private Date importDate;

    /* Whether or not this archive contains a topology */
    private boolean hasTopology;

    /* Node types count in this archive */
    private long nodeTypesCount;

    /** Default constructor */
    public Csar() {
    }

    /** Argument constructor */
    public Csar(String name, String version) {
        this.name = name;
        this.version = version;
        this.nestedVersion = new Version(version);
    }

    public String getId() {
        return createId(name, version);
    }

    public static String createId(String name, String version) {
        if (name == null) {
            throw new IndexingServiceException("Csar name is mandatory");
        }
        if (version == null) {
            throw new IndexingServiceException("Csar version is mandatory");
        }
        return name + ":" + version;
    }

    public void setId(String id) {
        // Not authorized to set id as it's auto-generated from name and version
    }

    /**
     * Merge the given dependencies with the current ones.
     *
     * @param dependencies
     */
    public void setDependencies(Set<CSARDependency> dependencies) {
        this.dependencies = dependencies;
    }

    /**
     * In the context of parsing you can not override name when already provided (in case of tosca meta).
     * 
     * @param name The new name of the archive.
     */
    public void setName(String name) {
        if (this.name == null || !ParsingContextExecution.exist()) {
            this.name = name;
        }
    }

    /**
     * In the context of parsing you can not override version when already provided (in case of tosca meta).
     *
     * @param version The new version of the archive.
     */
    public void setVersion(String version) {
        if (this.version == null || !ParsingContextExecution.exist()) {
            this.version = version;
            this.nestedVersion = new Version(version);
        }
    }

    /**
     * In the context of parsing you can not override template author when already provided (in case of tosca meta).
     *
     * @param templateAuthor The new template author of the archive.
     */
    public void setTemplateAuthor(String templateAuthor) {
        if (this.templateAuthor == null || !ParsingContextExecution.exist()) {
            this.templateAuthor = templateAuthor;
        }
    }
}