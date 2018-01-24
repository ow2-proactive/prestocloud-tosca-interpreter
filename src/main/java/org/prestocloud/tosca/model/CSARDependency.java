package org.prestocloud.tosca.model;

import lombok.AllArgsConstructor;
import lombok.EqualsAndHashCode;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.NonNull;
import lombok.RequiredArgsConstructor;
import lombok.Setter;
import lombok.ToString;

/**
 * Defines a dependency on a CloudServiceArchive.
 */
@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@RequiredArgsConstructor
@EqualsAndHashCode(of = { "name", "version" })
@ToString(exclude = "hash")
public class CSARDependency {
    @NonNull
    private String name;

    @NonNull
    private String version;

    /**
     * Hash of the main yaml file included in the csar.
     * This is used here to spot when a dependency has changed to provide update of templates as types may have changed.
     */
    private String hash;
}