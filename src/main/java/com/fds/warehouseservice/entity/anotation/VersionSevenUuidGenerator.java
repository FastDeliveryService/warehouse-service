package com.fds.warehouseservice.entity.anotation;

import com.fasterxml.uuid.Generators;
import com.fasterxml.uuid.impl.TimeBasedEpochRandomGenerator;
import org.hibernate.engine.spi.SharedSessionContractImplementor;
import org.hibernate.id.IdentifierGenerator;

public class VersionSevenUuidGenerator implements IdentifierGenerator {

    private static final TimeBasedEpochRandomGenerator generator = Generators.timeBasedEpochRandomGenerator();

    @Override
    public Object generate(SharedSessionContractImplementor session, Object object) {
        return generator.generate();
    }
}
