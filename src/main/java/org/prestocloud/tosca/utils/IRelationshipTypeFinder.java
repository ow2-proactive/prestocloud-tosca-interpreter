package org.prestocloud.tosca.utils;

import org.prestocloud.tosca.model.types.RelationshipType;

@FunctionalInterface
public interface IRelationshipTypeFinder {
    RelationshipType findElement(String id);
}
