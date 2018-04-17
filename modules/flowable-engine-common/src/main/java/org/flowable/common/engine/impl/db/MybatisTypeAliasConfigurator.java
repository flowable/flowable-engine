package org.flowable.common.engine.impl.db;

import org.apache.ibatis.type.TypeAliasRegistry;

/**
 * This class configures typeAliases in {@link TypeAliasRegistry}
 *
 * @author martin.grofcik
 */
public interface MybatisTypeAliasConfigurator {

    void configure(TypeAliasRegistry typeAliasRegistry);
}
