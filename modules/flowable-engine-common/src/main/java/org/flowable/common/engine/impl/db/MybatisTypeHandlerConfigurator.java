package org.flowable.common.engine.impl.db;

import org.apache.ibatis.type.TypeHandler;
import org.apache.ibatis.type.TypeHandlerRegistry;

/**
 * This class configures {@link TypeHandler} in {@link TypeHandlerRegistry}
 *
 * @author martin.grofcik
 */
public interface MybatisTypeHandlerConfigurator {

    void configure(TypeHandlerRegistry typeHandlerRegistry);
}
