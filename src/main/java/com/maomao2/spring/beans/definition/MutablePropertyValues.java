package com.maomao2.spring.beans.definition;

import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.Set;

public class MutablePropertyValues implements PropertyValues {

    private final List<PropertyValue> propertyValueList;

    private Set<String> processedProperties;

    /**
     * Creates a new empty MutablePropertyValues object.
     * <p>
     * Property values can be added with the {@code add} method.
     * 
     * @see #add(String, Object)
     */
    public MutablePropertyValues() {
        this.propertyValueList = new ArrayList<PropertyValue>(0);
    }

    /**
     * Deep copy constructor. Guarantees PropertyValue references
     * are independent, although it can't deep copy objects currently
     * referenced by individual PropertyValue objects.
     * 
     * @param original
     *            the PropertyValues to copy
     * @see #addPropertyValues(PropertyValues)
     */
    public MutablePropertyValues(PropertyValues original) {
        // We can optimize this because it's all new:
        // There is no replacement of existing property values.
        if (original != null) {
            PropertyValue[] pvs = original.getPropertyValues();
            this.propertyValueList = new ArrayList<PropertyValue>(pvs.length);
            for (PropertyValue pv : pvs) {
                this.propertyValueList.add(new PropertyValue(pv));
            }
        } else {
            this.propertyValueList = new ArrayList<PropertyValue>(0);
        }
    }

    /**
     * Return the underlying List of PropertyValue objects in its raw form.
     * The returned List can be modified directly, although this is not recommended.
     * <p>
     * This is an accessor for optimized access to all PropertyValue objects.
     * It is not intended for typical programmatic use.
     */
    public List<PropertyValue> getPropertyValueList() {
        return this.propertyValueList;
    }

    /**
     * Return the number of PropertyValue entries in the list.
     */
    public int size() {
        return this.propertyValueList.size();
    }

    /**
     * Add all property values from the given Map.
     * 
     * @param other
     *            Map with property values keyed by property name,
     *            which must be a String
     * @return this in order to allow for adding multiple property values in a chain
     */
    public MutablePropertyValues addPropertyValues(Map<?, ?> other) {
        if (other != null) {
            for (Map.Entry<?, ?> entry : other.entrySet()) {
                addPropertyValue(new PropertyValue(entry.getKey().toString(), entry.getValue()));
            }
        }
        return this;
    }



    /**
     * Modify a PropertyValue object held in this object.
     * Indexed from 0.
     */
    public void setPropertyValueAt(PropertyValue pv, int i) {
        this.propertyValueList.set(i, pv);
    }

    /**
     * Remove the given PropertyValue, if contained.
     * 
     * @param pv
     *            the PropertyValue to remove
     */
    public void removePropertyValue(PropertyValue pv) {
        this.propertyValueList.remove(pv);
    }

    /**
     * Overloaded version of {@code removePropertyValue} that takes a property name.
     * 
     * @param propertyName
     *            name of the property
     * @see #removePropertyValue(PropertyValue)
     */
    public void removePropertyValue(String propertyName) {
        this.propertyValueList.remove(getPropertyValue(propertyName));
    }

    public PropertyValue[] getPropertyValues() {
        return this.propertyValueList.toArray(new PropertyValue[this.propertyValueList.size()]);
    }

    public PropertyValue getPropertyValue(String propertyName) {
        for (PropertyValue pv : this.propertyValueList) {
            if (pv.getName().equals(propertyName)) {
                return pv;
            }
        }
        return null;
    }

    /**
     * Get the raw property value, if any.
     * 
     * @param propertyName
     *            the name to search for
     * @return the raw property value, or {@code null}
     * @since 4.0
     * @see #getPropertyValue(String)
     * @see PropertyValue#getValue()
     */
    public Object get(String propertyName) {
        PropertyValue pv = getPropertyValue(propertyName);
        return (pv != null ? pv.getValue() : null);
    }

    public boolean contains(String propertyName) {
        return (getPropertyValue(propertyName) != null ||
                (this.processedProperties != null && this.processedProperties.contains(propertyName)));
    }

    public boolean isEmpty() {
        return this.propertyValueList.isEmpty();
    }
    /**
     * Add a PropertyValue object, replacing any existing one for the
     * corresponding property or getting merged with it (if applicable).
     * @param pv PropertyValue object to add
     * @return this in order to allow for adding multiple property values in a chain
     */
    public MutablePropertyValues addPropertyValue(PropertyValue pv) {
        for (int i = 0; i < this.propertyValueList.size(); i++) {
            PropertyValue currentPv = this.propertyValueList.get(i);
            if (currentPv.getName().equals(pv.getName())) {
                setPropertyValueAt(pv, i);
                return this;
            }
        }
        this.propertyValueList.add(pv);
        return this;
    }
}
