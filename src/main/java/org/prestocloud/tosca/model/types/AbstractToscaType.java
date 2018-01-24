package org.prestocloud.tosca.model.types;

import java.util.Date;
import java.util.List;

import com.fasterxml.jackson.annotation.JsonInclude;
import com.fasterxml.jackson.annotation.JsonInclude.Include;

import lombok.EqualsAndHashCode;
import lombok.Getter;
import lombok.Setter;
import prestocloud.exceptions.IndexingServiceException;
import prestocloud.model.common.IDatableResource;
import prestocloud.model.common.ITaggableResource;
import prestocloud.model.common.IWorkspaceResource;
import prestocloud.model.common.Tag;
import prestocloud.utils.version.Version;

@Getter
@Setter
@EqualsAndHashCode(of = { "elementId", "archiveName", "archiveVersion" })
@JsonInclude(Include.NON_NULL)
public abstract class AbstractToscaType implements IDatableResource, IWorkspaceResource, ITaggableResource {
    private String archiveName;

    private String archiveVersion;

    private Version nestedVersion;

    private String workspace;

    private String elementId;

    private Date creationDate;

    private Date lastUpdateDate;

    /* Normative element */
    private String description;

    /* DSL extension */
    private List<Tag> tags;

    public String getId() {
        if (elementId == null) {
            throw new IndexingServiceException("Element id is mandatory");
        }
        if (archiveVersion == null) {
            throw new IndexingServiceException("Archive version is mandatory");
        }
        return elementId + ":" + archiveVersion;
    }

    public void setId(String id) {
        // Not authorized to set id as it's auto-generated
    }

    public void setArchiveVersion(String version) {
        this.archiveVersion = version;
        this.nestedVersion = new Version(version);
    }
}
