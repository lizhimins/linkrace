package com.alirace.model;

import org.apache.commons.pool2.BasePooledObjectFactory;
import org.apache.commons.pool2.PooledObject;
import org.apache.commons.pool2.impl.DefaultPooledObject;

public class RecordPoolFactory extends BasePooledObjectFactory<Record> {

    @Override
    public Record create() throws Exception {
        return new Record();
    }

    @Override
    public PooledObject<Record> wrap(Record obj) {
        return new DefaultPooledObject<>(obj);
    }

    @Override
    public void passivateObject(PooledObject<Record> p) {
        // TODO:
        Record record = p.getObject();
    }
}