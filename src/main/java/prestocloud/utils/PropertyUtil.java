package prestocloud.utils;

import java.util.Map;

import org.apache.commons.collections4.MapUtils;
import org.prestocloud.tosca.model.definitions.AbstractPropertyValue;
import org.prestocloud.tosca.model.definitions.ComplexPropertyValue;
import org.prestocloud.tosca.model.definitions.PropertyDefinition;
import org.prestocloud.tosca.model.definitions.ScalarPropertyValue;

import com.google.common.collect.Maps;

import prestocloud.paas.exceptions.NotSupportedException;

public final class PropertyUtil {
    private PropertyUtil() {
    }

    /**
     * Convert a map of property definitions to a map of property values based on the default values specified.
     * <p/>
     * Note: This method will have to be removed once the ui manages properties correctly.
     *
     * @param propertyDefinitions The map of {@link PropertyDefinition}s to convert.
     * @return An equivalent map of default {@link ScalarPropertyValue}s, that contains all properties definitions keys (default
     *         value
     *         is null when no default value is specified in the property definition).
     */
    public static Map<String, AbstractPropertyValue> getDefaultPropertyValuesFromPropertyDefinitions(Map<String, PropertyDefinition> propertyDefinitions) {
        if (propertyDefinitions == null) {
            return null;
        }

        Map<String, AbstractPropertyValue> defaultPropertyValues = Maps.newLinkedHashMap();

        for (Map.Entry<String, PropertyDefinition> entry : propertyDefinitions.entrySet()) {
            defaultPropertyValues.put(entry.getKey(), getDefaultPropertyValueFromPropertyDefinition(entry.getValue()));
        }

        return defaultPropertyValues;
    }

    public static AbstractPropertyValue getDefaultPropertyValueFromPropertyDefinition(PropertyDefinition propertyDefinition) {
        if (propertyDefinition == null) {
            return null;
        }
        Object defaultValue = propertyDefinition.getDefault();
        if (defaultValue == null) {
            return null;
        }
        return (AbstractPropertyValue) defaultValue;
    }

    public static boolean setScalarDefaultValueIfNotNull(Map<String, String> properties, String key, AbstractPropertyValue abstractPropertyValue) {
        if (abstractPropertyValue != null && abstractPropertyValue instanceof ScalarPropertyValue) {
            properties.put(key, ((ScalarPropertyValue) abstractPropertyValue).getValue());
            return true;
        }
        return false;
    }

    public static void setScalarDefaultValueOrNull(Map<String, String> properties, String key, AbstractPropertyValue abstractPropertyValue) {
        if (abstractPropertyValue != null && abstractPropertyValue instanceof ScalarPropertyValue) {
            properties.put(key, ((ScalarPropertyValue) abstractPropertyValue).getValue());
        } else {
            properties.put(key, null);
        }
    }

    public static AbstractPropertyValue getDefaultFromPropertyDefinitions(String propertyName, Map<String, PropertyDefinition> propertyDefinitions) {
        if (MapUtils.isNotEmpty(propertyDefinitions) && propertyDefinitions.containsKey(propertyName)) {
            return propertyDefinitions.get(propertyName).getDefault();
        } else {
            return null;
        }
    }

    /**
     * Get the property from a complex path. If the path is simple, this method will return null.
     * A complex path is containing '.'
     *
     * @param propertyPath the complex property path
     * @return the first element of the path (property name)
     */
    public static String getPropertyNameFromComplexPath(String propertyPath) {
        if (propertyPath.contains(".")) {
            String[] paths = propertyPath.split("\\.");
            return paths[0];
        } else {
            return null;
        }
    }

    /**
     * Get the scalar value
     *
     * @param propertyValue the property value
     * @throws NotSupportedException if called on a non ScalarPropertyValue
     * @return the value or null if the propertyValue is null
     */
    public static String getScalarValue(AbstractPropertyValue propertyValue) {
        if (propertyValue == null) {
            return null;
        } else if (propertyValue instanceof ScalarPropertyValue) {
            return ((ScalarPropertyValue) propertyValue).getValue();
        } else {
            throw new NotSupportedException("Property value is not of type scalar");
        }
    }

    /**
     * Returns the object as a <code>Map&lt;String, Object&gt;</code> if it's a {@link ComplexPropertyValue} or already a
     * <code>Map&lt;String, Object&gt;</code>.
     */
    public static Map<String, Object> getMapProperty(Object value) {
        if (value instanceof ComplexPropertyValue) {
            return ((ComplexPropertyValue) value).getValue();
        } else if (value instanceof Map<?, ?>) {
            return (Map<String, Object>) value;
        } else {
            return null;
        }
    }

    /**
     * Crawls a complex and deep structure of <code>ComplexPropertyValue</code> and/or <code>Map&lt;String, Object&gt;</code> using a dot separated path (for
     * example <code>my.deep.property</code> where 'my' and 'deep' are nested maps and 'property' an entry of 'deep').
     * <p>
     * Returns <code>null</code> if any item on the path (except the last one, ie. the property itself) is not a <code>ComplexPropertyValue</code> or
     * <code>Map&lt;String, Object&gt;</code> (ie. doesn't manage nested lists).
     * </p>
     * <p>
     * If the returned object of type {@link NestedPropertyWrapper} is not null, you are sure that the wrapped map contains the entry.
     * </p>
     */
    public static NestedPropertyWrapper getNestedProperty(Object root, String propertyPath) {
        Map<String, Object> map = getMapProperty(root);
        if (map != null) {
            String[] paths = propertyPath.split("\\.");
            for (int i = 0; i < paths.length - 1; i++) {
                Object currentEntry = map.get(paths[i]);
                Map<String, Object> currentEntryMap = getMapProperty(currentEntry);
                if (currentEntryMap == null) {
                    return null;
                } else {
                    map = currentEntryMap;
                }
            }
            String key = paths[paths.length - 1];
            if (map.containsKey(key)) {
                return new NestedPropertyWrapper(map, key);
            } else {
                return null;
            }
        } else {
            return null;
        }
    }

    public static class NestedPropertyWrapper {
        public final Map<String, Object> parent;
        public final String key;

        public NestedPropertyWrapper(Map<String, Object> parent, String key) {
            this.parent = parent;
            this.key = key;
        }

        public Object getValue() {
            return parent.get(key);
        }
    }

    // /**
    // * Get the value of a given property at a given path. Doesn't manage lists (using spel could be usefull to manage lists index or advanced key selectors).
    // */
    // // TODO ALIEN-2589: see alien4cloud.paas.function.FunctionEvaluator.getPropertyValue()
    // public static AbstractPropertyValue _getPropertyValueFromPath(Map<String, AbstractPropertyValue> values, String propertyPath) {
    // if (propertyPath.contains(".")) {
    // String[] paths = propertyPath.split("\\.");
    // AbstractPropertyValue apv = values.get(paths[0]);
    // if (apv instanceof ComplexPropertyValue) {
    // Map<String, Object> currentMap = ((ComplexPropertyValue)apv).getValue();
    // for (int i=1; i<paths.length; i++) {
    // Object currentValue = currentMap.get(paths[i]);
    // if (i == paths.length - 1) {
    // // this is the last one, can be returned
    // if (currentValue instanceof AbstractPropertyValue) {
    // return (AbstractPropertyValue)currentValue;
    // } else {
    // return null;
    // }
    // } else {
    // if (currentValue instanceof ComplexPropertyValue) {
    // ComplexPropertyValue cpv = (ComplexPropertyValue)currentValue;
    // currentMap = cpv.getValue();
    // } else {
    // return null;
    // }
    // }
    // }
    // return null;
    // } else {
    // return null;
    // }
    // } else {
    // return values.get(propertyPath);
    // }
    // }

    /**
     * Get the value of a given property at a given path. For example, if you try to get <code>my.deep.property</code>,
     * and if <code>my</code> and <code>deep</code> are both <code>ComplexPropertyValue</code> or <code>Map&lt;String, Object&gt;</code>,
     * then will return the entry named <code>property</code> of <code>deep</code>, if it's an {@link AbstractPropertyValue}. In any other case, will return
     * <code>null</code>.
     *
     * @return the found {@link AbstractPropertyValue} or <code>null</code> if any item on the path is not a {@link ComplexPropertyValue} or a
     *         <code>Map&lt;String, Object&gt;</code>, or if the property is not a {@link AbstractPropertyValue}.
     */
    public static AbstractPropertyValue getPropertyValueFromPath(Map<String, AbstractPropertyValue> values, String propertyPath) {
        NestedPropertyWrapper npw = getNestedProperty(values, propertyPath);
        if (npw != null) {
            if (npw.getValue() instanceof AbstractPropertyValue) {
                return (AbstractPropertyValue) npw.getValue();
            } else if (npw.getValue() != null) {
                return new ScalarPropertyValue(npw.getValue().toString());
            }
        }
        return null;
    }

    public static Object getPropertyRawValueFromPath(Map<String, AbstractPropertyValue> values, String propertyPath) {
        NestedPropertyWrapper npw = getNestedProperty(values, propertyPath);
        if (npw != null) {
            return npw.getValue();
        }
        return null;
    }

}