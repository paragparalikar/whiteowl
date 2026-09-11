package com.whiteowl.core.backtest.v2.smartvalue;

import groovy.lang.GroovyObject;
import groovy.lang.MetaClass;
import org.codehaus.groovy.runtime.InvokerHelper;

public class FloatSmartValue implements GroovyObject {

    private final CircularFloatBuffer buffer;
    private MetaClass metaClass;

    public FloatSmartValue(int capacity) {
        this.buffer = new CircularFloatBuffer(capacity);
        this.metaClass = InvokerHelper.getMetaClass(getClass());
    }

    public void push(float value) {
        buffer.push(value);
    }

    public float getAt(int offset) {
        return buffer.get(offset);
    }

    public float value() {
        return buffer.get(0);
    }

    public int size() {
        return buffer.size();
    }

    public int capacity() {
        return buffer.capacity();
    }

    public FloatSmartValue plus(FloatSmartValue other) {
        return new DerivedFloatSmartValue(capacity(), this, other, ArithmeticOp.ADD);
    }

    public FloatSmartValue plus(float scalar) {
        return new ScalarDerivedFloatSmartValue(capacity(), this, scalar, ArithmeticOp.ADD);
    }

    public FloatSmartValue minus(FloatSmartValue other) {
        return new DerivedFloatSmartValue(capacity(), this, other, ArithmeticOp.SUBTRACT);
    }

    public FloatSmartValue minus(float scalar) {
        return new ScalarDerivedFloatSmartValue(capacity(), this, scalar, ArithmeticOp.SUBTRACT);
    }

    public FloatSmartValue multiply(FloatSmartValue other) {
        return new DerivedFloatSmartValue(capacity(), this, other, ArithmeticOp.MULTIPLY);
    }

    public FloatSmartValue multiply(float scalar) {
        return new ScalarDerivedFloatSmartValue(capacity(), this, scalar, ArithmeticOp.MULTIPLY);
    }

    public FloatSmartValue div(FloatSmartValue other) {
        return new DerivedFloatSmartValue(capacity(), this, other, ArithmeticOp.DIVIDE);
    }

    public FloatSmartValue div(float scalar) {
        return new ScalarDerivedFloatSmartValue(capacity(), this, scalar, ArithmeticOp.DIVIDE);
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
