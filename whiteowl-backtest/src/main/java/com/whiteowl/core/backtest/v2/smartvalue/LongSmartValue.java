package com.whiteowl.core.backtest.v2.smartvalue;

import groovy.lang.GroovyObject;
import groovy.lang.MetaClass;
import org.codehaus.groovy.runtime.InvokerHelper;

public final class LongSmartValue implements GroovyObject {

    private final CircularLongBuffer buffer;
    private MetaClass metaClass;

    public LongSmartValue(int capacity) {
        this.buffer = new CircularLongBuffer(capacity);
        this.metaClass = InvokerHelper.getMetaClass(getClass());
    }

    public void push(long value) {
        buffer.push(value);
    }

    public long getAt(int offset) {
        return buffer.get(offset);
    }

    public long value() {
        return buffer.get(0);
    }

    public int size() {
        return buffer.size();
    }

    public int capacity() {
        return buffer.capacity();
    }

    @Override
    public Object invokeMethod(String name, Object args) {
        return metaClass.invokeMethod(this, name, args);
    }

    @Override
    public Object getProperty(String propertyName) {
        return metaClass.getProperty(this, propertyName);
    }

    @Override
    public void setProperty(String propertyName, Object newValue) {
        metaClass.setProperty(this, propertyName, newValue);
    }

    @Override
    public MetaClass getMetaClass() {
        return metaClass;
    }

    @Override
    public void setMetaClass(MetaClass metaClass) {
        this.metaClass = metaClass;
    }

}
