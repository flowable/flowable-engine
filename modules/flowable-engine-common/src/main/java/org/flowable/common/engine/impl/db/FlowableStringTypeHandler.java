package org.flowable.common.engine.impl.db;

import java.sql.CallableStatement;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.sql.Types;

import org.apache.ibatis.type.BaseTypeHandler;
import org.apache.ibatis.type.JdbcType;
import org.apache.ibatis.type.TypeException;

public class FlowableStringTypeHandler extends BaseTypeHandler<String> {
    
    protected boolean useDefaultJdbcType;
    
    public FlowableStringTypeHandler(boolean useDefaultJdbcType) {
        this.useDefaultJdbcType = useDefaultJdbcType;
    }
    
    @Override
    public void setParameter(PreparedStatement ps, int i, String parameter, JdbcType jdbcType) throws SQLException {
        if (parameter == null) {
            if (jdbcType == null) {
                throw new TypeException("JDBC requires that the JdbcType must be specified for all nullable parameters.");
            }
            try {
                if (useDefaultJdbcType) {
                    JdbcType finalJdbcType = null;
                    if (jdbcType.equals(JdbcType.NVARCHAR)) {
                        finalJdbcType = JdbcType.VARCHAR;
                    } else {
                        finalJdbcType = jdbcType;
                    }
                    ps.setNull(i,  finalJdbcType.TYPE_CODE);
                    
                } else {
                    ps.setNull(i, Types.NULL);
                }
            } catch (SQLException e) {
                throw new TypeException("Error setting null for parameter #" + i + " with JdbcType " + jdbcType + " . "
                      + "Try setting a different JdbcType for this parameter or a different jdbcTypeForNull configuration property. "
                      + "Cause: " + e, e);
            }
        } else {
            try {
                setNonNullParameter(ps, i, parameter, jdbcType);
            } catch (Exception e) {
              throw new TypeException("Error setting non null for parameter #" + i + " with JdbcType " + jdbcType + " . "
                  + "Try setting a different JdbcType for this parameter or a different configuration property. " + "Cause: "
                  + e, e);
            }
        }
    }

    @Override
    public void setNonNullParameter(PreparedStatement ps, int i, String parameter, JdbcType jdbcType) throws SQLException {
        ps.setString(i, parameter);
    }

    @Override
    public String getNullableResult(ResultSet rs, String columnName) throws SQLException {
        return rs.getString(columnName);
    }

    @Override
    public String getNullableResult(ResultSet rs, int columnIndex) throws SQLException {
        return rs.getString(columnIndex);
    }

    @Override
    public String getNullableResult(CallableStatement cs, int columnIndex) throws SQLException {
        return cs.getString(columnIndex);
    }
}
