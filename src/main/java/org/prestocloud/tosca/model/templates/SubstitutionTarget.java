package org.prestocloud.tosca.model.templates;

import lombok.AllArgsConstructor;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
public class SubstitutionTarget {
    private String nodeTemplateName;
    private String targetId;
    private String serviceRelationshipType;

    public SubstitutionTarget(String nodeTemplateName, String targetId) {
        this.nodeTemplateName = nodeTemplateName;
        this.targetId = targetId;
    }
}