package org.prestocloud.tosca.model.types;

import java.util.List;

import lombok.Getter;
import lombok.Setter;

@Getter
@Setter
public class ArtifactType extends AbstractInheritableToscaType {
    private String mimeType;
    private List<String> fileExt;
}