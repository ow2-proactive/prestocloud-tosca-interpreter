package org.prestocloud.tosca.model.templates;

import java.util.Map;

import lombok.AllArgsConstructor;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
public class SubstitutionMapping {
    private String substitutionType;

    private Map<String, SubstitutionTarget> capabilities;
    private Map<String, SubstitutionTarget> requirements;
}