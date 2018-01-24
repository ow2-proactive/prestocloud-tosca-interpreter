package org.prestocloud.tosca.normative.types;

import org.prestocloud.tosca.exceptions.InvalidPropertyValueException;

import prestocloud.utils.VersionUtil;
import prestocloud.utils.version.InvalidVersionException;
import prestocloud.utils.version.Version;

public class VersionType implements IComparablePropertyType<Version> {

    public static final String NAME = "version";

    @Override
    public Version parse(String text) throws InvalidPropertyValueException {
        try {
            return VersionUtil.parseVersion(text);
        } catch (InvalidVersionException e) {
            throw new InvalidPropertyValueException("Could not parse version from value " + text, e);
        }
    }

    @Override
    public String print(Version value) {
        return value.toString();
    }

    @Override
    public String getTypeName() {
        return NAME;
    }
}
