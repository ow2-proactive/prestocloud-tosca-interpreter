package org.prestocloud.tosca.model.templates;

import java.util.Date;
import java.util.List;
import java.util.Map;
import java.util.Set;

import org.prestocloud.tosca.model.CSARDependency;
import org.prestocloud.tosca.model.Csar;
import org.prestocloud.tosca.model.definitions.DeploymentArtifact;
import org.prestocloud.tosca.model.definitions.PropertyDefinition;

import com.fasterxml.jackson.annotation.JsonIgnoreProperties;
import com.fasterxml.jackson.annotation.JsonInclude;
import com.fasterxml.jackson.annotation.JsonInclude.Include;
import com.google.common.collect.Sets;

import lombok.AllArgsConstructor;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;
import prestocloud.model.common.IDatableResource;
import prestocloud.model.common.IWorkspaceResource;
import prestocloud.model.common.Tag;
import prestocloud.utils.version.Version;

@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@JsonInclude(Include.NON_NULL)
@JsonIgnoreProperties(ignoreUnknown = true)
public class Topology implements IDatableResource, IWorkspaceResource {
    private String archiveName;

    private String archiveVersion;

    private Version nestedVersion;

    private String workspace;

    private String description;

    private Date creationDate;

    private Date lastUpdateDate = new Date();

    /** The list of dependencies of this topology. */
    private Set<CSARDependency> dependencies = Sets.newHashSet();

    private Map<String, NodeTemplate> nodeTemplates;

    private Map<String, PolicyTemplate> policies;

    private Map<String, PropertyDefinition> inputs;

    /**
     * Outputs coming from node properties:
     * <ul>
     * <li>key is the node template name.</li>
     * <li>value is a list of node template property names.</li>
     * </ul>
     */
    private Map<String, Set<String>> outputProperties;

    /**
     * Outputs coming from node template capability properties:
     * <ul>
     * <li>key is the node template name.</li>
     * <li>key is the capability name.</li>
     * <li>value is a list of output property names.</li>
     * </ul>
     */
    private Map<String, Map<String, Set<String>>> outputCapabilityProperties;

    /**
     * Outputs coming from node attributes:
     * <ul>
     * <li>key is the node template name.</li>
     * <li>value is a list of node template attribute names.</li>
     * </ul>
     */
    private Map<String, Set<String>> outputAttributes;

    /**
     * These artifacts will be given at deployment time and can be shared by several nodes.
     */
    private Map<String, DeploymentArtifact> inputArtifacts;

    private Map<String, NodeGroup> groups;

    /**
     * When not null, describe how this topology can be used to substitute a node type in another topology (topology composition).
     */
    private SubstitutionMapping substitutionMapping;

    /* Archive meta-data are also set as topology tags. */
    private List<Tag> tags;

    //@Id
    public String getId() {
        return Csar.createId(archiveName, archiveVersion);
    }

    public void setId(String id) {
        // Not authorized to set id as it's auto-generated from name and version
    }

    public void setArchiveVersion(String version) {
        this.archiveVersion = version;
        this.nestedVersion = new Version(version);
    }

    /**
     * /**
     * Return true if the topology is an empty topology (won't be saved on import).
     *
     * @return True if the topology is empty (doesn't contains any node).
     */
    public boolean isEmpty() {
        return nodeTemplates == null || nodeTemplates.isEmpty();
    }
}