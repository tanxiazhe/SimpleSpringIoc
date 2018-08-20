package com.maomao2.spring.beans.definition;

import java.util.Collections;
import java.util.LinkedHashMap;
import java.util.LinkedList;
import java.util.List;
import java.util.Map;
import java.util.Set;

import com.maomao2.spring.util.ClassUtils;

public class ConstructorArgumentValues {

    private final Map<Integer, ValueHolder> indexedArgumentValues = new LinkedHashMap<Integer, ValueHolder>(0);

    private final List<ValueHolder> genericArgumentValues = new LinkedList<ValueHolder>();

    /**
     * Create a new empty ConstructorArgumentValues object.
     */
    public ConstructorArgumentValues() {
    }

    /**
     * Add an argument value for the given index in the constructor argument list.
     *
     * @param index
     *            the index in the constructor argument list
     * @param value
     *            the argument value
     */
    public void

            addIndexedArgumentValue(int index, Object value) {
        addIndexedArgumentValue(index, new ValueHolder(value));
    }

    /**
     * Add an argument value for the given index in the constructor argument list.
     * 
     * @param index
     *            the index in the constructor argument list
     * @param newValue
     *            the argument value in the form of a ValueHolder
     */
    public void addIndexedArgumentValue(int index, ValueHolder newValue) {
        addOrMergeIndexedArgumentValue(index, newValue);
    }

    /**
     * Add an argument value for the given index in the constructor argument list,
     * merging the new value (typically a collection) with the current value
     * if demanded: see {@link org.springframework.beans.Mergeable}.
     * 
     * @param key
     *            the index in the constructor argument list
     * @param newValue
     *            the argument value in the form of a ValueHolder
     */
    private void addOrMergeIndexedArgumentValue(Integer key, ValueHolder newValue) {
        this.indexedArgumentValues.put(key, newValue);
    }

    /**
     * Add an argument value for the given index in the constructor argument list.
     *
     * @param index
     *            the index in the constructor argument list
     * @param value
     *            the argument value
     * @param type
     *            the type of the constructor argument
     */
    public void addIndexedArgumentValue(int index, Object value, String type) {
        addIndexedArgumentValue(index, new ValueHolder(value, type));
    }

    /**
     * Check whether an argument value has been registered for the given index.
     *
     * @param index
     *            the index in the constructor argument list
     */
    public boolean hasIndexedArgumentValue(int index) {
        return this.indexedArgumentValues.containsKey(index);
    }

    /**
     * Get argument value for the given index in the constructor argument list.
     *
     * @param index
     *            the index in the constructor argument list
     * @param requiredType
     *            the type to match (can be {@code null} to match untyped values only)
     * @return the ValueHolder for the argument, or {@code null} if none set
     */
    public ValueHolder getIndexedArgumentValue(int index, Class<?> requiredType) {
        return getIndexedArgumentValue(index, requiredType, null);
    }

    /**
     * Get argument value for the given index in the constructor argument list.
     *
     * @param index
     *            the index in the constructor argument list
     * @param requiredType
     *            the type to match (can be {@code null} to match untyped values only)
     * @param requiredName
     *            the type to match (can be {@code null} to match unnamed values only, or empty String to match
     *            any name)
     * @return the ValueHolder for the argument, or {@code null} if none set
     */
    public ValueHolder getIndexedArgumentValue(int index, Class<?> requiredType, String requiredName) {
        ValueHolder valueHolder = this.indexedArgumentValues.get(index);
        if (valueHolder != null &&
                (valueHolder.getType() == null ||
                        (requiredType != null && ClassUtils.matchesTypeName(requiredType, valueHolder.getType())))
                &&
                (valueHolder.getName() == null || "".equals(requiredName) ||
                        (requiredName != null && requiredName.equals(valueHolder.getName())))) {
            return valueHolder;
        }
        return null;
    }

    /**
     * Return the map of indexed argument values.
     *
     * @return unmodifiable Map with Integer index as key and ValueHolder as value
     * @see ValueHolder
     */
    public Map<Integer, ValueHolder> getIndexedArgumentValues() {
        return Collections.unmodifiableMap(this.indexedArgumentValues);
    }

    /**
     * Add a generic argument value to be matched by type.
     * <p>
     * Note: A single generic argument value will just be used once, rather than matched multiple times.
     *
     * @param value
     *            the argument value
     */
    public void addGenericArgumentValue(Object value) {
        this.genericArgumentValues.add(new ValueHolder(value));
    }

    /**
     * Add a generic argument value to be matched by type.
     * <p>
     * Note: A single generic argument value will just be used once, rather than matched multiple times.
     *
     * @param value
     *            the argument value
     * @param type
     *            the type of the constructor argument
     */
    public void addGenericArgumentValue(Object value, String type) {
        this.genericArgumentValues.add(new ValueHolder(value, type));
    }

    /**
     * Add a generic argument value to be matched by type or name (if available).
     * <p>
     * Note: A single generic argument value will just be used once,
     * rather than matched multiple times.
     *
     * @param newValue
     *            the argument value in the form of a ValueHolder
     *            <p>
     *            Note: Identical ValueHolder instances will only be registered once,
     *            to allow for merging and re-merging of argument value definitions. Distinct ValueHolder instances
     *            carrying the same
     *            content are of course allowed.
     */
    public void addGenericArgumentValue(ValueHolder newValue) {
        if (!this.genericArgumentValues.contains(newValue)) {
            addOrMergeGenericArgumentValue(newValue);
        }
    }

    /**
     * Add a generic argument value, merging the new value (typically a collection) with the current value if demanded:
     * see {@link org.springframework.beans.Mergeable}.
     *
     * @param newValue
     *            the argument value in the form of a ValueHolder
     */
    private void addOrMergeGenericArgumentValue(ValueHolder newValue) {

        this.genericArgumentValues.add(newValue);

    }

    /**
     * Look for a generic argument value that matches the given type.
     *
     * @param requiredType
     *            the type to match
     * @return the ValueHolder for the argument, or {@code null} if none set
     */
    public ValueHolder getGenericArgumentValue(Class<?> requiredType) {
        return getGenericArgumentValue(requiredType, null, null);
    }

    /**
     * Look for a generic argument value that matches the given type.
     *
     * @param requiredType
     *            the type to match
     * @param requiredName
     *            the name to match
     * @return the ValueHolder for the argument, or {@code null} if none set
     */
    public ValueHolder getGenericArgumentValue(Class<?> requiredType, String requiredName) {
        return getGenericArgumentValue(requiredType, requiredName, null);
    }

    /**
     * Look for the next generic argument value that matches the given type, ignoring argument values that have already
     * been used in the current resolution process.
     *
     * @param requiredType
     *            the type to match (can be {@code null} to find an arbitrary next generic argument value)
     * @param requiredName
     *            the name to match (can be {@code null} to not match argument values by name, or empty String to
     *            match any name)
     * @param usedValueHolders
     *            a Set of ValueHolder objects that have already been used in the current resolution process
     *            and should therefore not be returned again
     * @return the ValueHolder for the argument, or {@code null} if none found
     */
    public ValueHolder getGenericArgumentValue(Class<?> requiredType, String requiredName,
            Set<ValueHolder> usedValueHolders) {
        for (ValueHolder valueHolder : this.genericArgumentValues) {
            if (usedValueHolders != null && usedValueHolders.contains(valueHolder)) {
                continue;
            }
            if (valueHolder.getName() != null && !"".equals(requiredName) &&
                    (requiredName == null || !valueHolder.getName().equals(requiredName))) {
                continue;
            }
            if (valueHolder.getType() != null &&
                    (requiredType == null || !ClassUtils.matchesTypeName(requiredType, valueHolder.getType()))) {
                continue;
            }
            if (requiredType != null && valueHolder.getType() == null && valueHolder.getName() == null &&
                    !ClassUtils.isAssignableValue(requiredType, valueHolder.getValue())) {
                continue;
            }
            return valueHolder;
        }
        return null;
    }

    /**
     * Return the list of generic argument values.
     *
     * @return unmodifiable List of ValueHolders
     * @see ValueHolder
     */
    public List<ValueHolder> getGenericArgumentValues() {
        return Collections.unmodifiableList(this.genericArgumentValues);
    }

    /**
     * Look for an argument value that either corresponds to the given index in the constructor argument list or
     * generically matches by type.
     *
     * @param index
     *            the index in the constructor argument list
     * @param requiredType
     *            the parameter type to match
     * @return the ValueHolder for the argument, or {@code null} if none set
     */
    public ValueHolder getArgumentValue(int index, Class<?> requiredType) {
        return getArgumentValue(index, requiredType, null, null);
    }

    /**
     * Look for an argument value that either corresponds to the given index in the constructor argument list or
     * generically matches by type.
     *
     * @param index
     *            the index in the constructor argument list
     * @param requiredType
     *            the parameter type to match
     * @param requiredName
     *            the parameter name to match
     * @return the ValueHolder for the argument, or {@code null} if none set
     */
    public ValueHolder getArgumentValue(int index, Class<?> requiredType, String requiredName) {
        return getArgumentValue(index, requiredType, requiredName, null);
    }

    /**
     * Look for an argument value that either corresponds to the given index in the constructor argument list or
     * generically matches by type.
     *
     * @param index
     *            the index in the constructor argument list
     * @param requiredType
     *            the parameter type to match (can be {@code null} to find an untyped argument value)
     * @param requiredName
     *            the parameter name to match (can be {@code null} to find an unnamed argument value, or empty
     *            String to match any name)
     * @param usedValueHolders
     *            a Set of ValueHolder objects that have already been used in the current resolution process
     *            and should therefore not be returned again (allowing to return the next generic argument match in case
     *            of multiple
     *            generic argument values of the same type)
     * @return the ValueHolder for the argument, or {@code null} if none set
     */
    public ValueHolder getArgumentValue(int index, Class<?> requiredType, String requiredName,
            Set<ValueHolder> usedValueHolders) {
        ValueHolder valueHolder = getIndexedArgumentValue(index, requiredType, requiredName);
        if (valueHolder == null) {
            valueHolder = getGenericArgumentValue(requiredType, requiredName, usedValueHolders);
        }
        return valueHolder;
    }

    /**
     * Return the number of argument values held in this instance, counting both indexed and generic argument values.
     */
    public int getArgumentCount() {
        return (this.indexedArgumentValues.size() + this.genericArgumentValues.size());
    }

    /**
     * Return if this holder does not contain any argument values, neither indexed ones nor generic ones.
     */
    public boolean isEmpty() {
        return (this.indexedArgumentValues.isEmpty() && this.genericArgumentValues.isEmpty());
    }

    /**
     * Holder for a constructor argument value, with an optional type attribute indicating the target type of the actual
     * constructor argument.
     */
    public static class ValueHolder {

        private Object value;

        private String type;

        private String name;

        /**
         * Create a new ValueHolder for the given value.
         *
         * @param value
         *            the argument value
         */
        public ValueHolder(Object value) {
            this.value = value;
        }

        /**
         * Create a new ValueHolder for the given value and type.
         *
         * @param value
         *            the argument value
         * @param type
         *            the type of the constructor argument
         */
        public ValueHolder(Object value, String type) {
            this.value = value;
            this.type = type;
        }

        /**
         * Create a new ValueHolder for the given value, type and name.
         *
         * @param value
         *            the argument value
         * @param type
         *            the type of the constructor argument
         * @param name
         *            the name of the constructor argument
         */
        public ValueHolder(Object value, String type, String name) {
            this.value = value;
            this.type = type;
            this.name = name;
        }

        /**
         * Set the value for the constructor argument.
         *
         * @see PropertyPlaceholderConfigurer
         */
        public void setValue(Object value) {
            this.value = value;
        }

        /**
         * Return the value for the constructor argument.
         */
        public Object getValue() {
            return this.value;
        }

        /**
         * Set the type of the constructor argument.
         */
        public void setType(String type) {
            this.type = type;
        }

        /**
         * Return the type of the constructor argument.
         */
        public String getType() {
            return this.type;
        }

        /**
         * Set the name of the constructor argument.
         */
        public void setName(String name) {
            this.name = name;
        }

        /**
         * Return the name of the constructor argument.
         */
        public String getName() {
            return this.name;
        }

    }

}
