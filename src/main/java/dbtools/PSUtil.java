package dbtools;

import java.io.ByteArrayInputStream;
import java.io.StringReader;
import java.sql.PreparedStatement;
import java.sql.SQLException;
import java.sql.Timestamp;
import java.sql.Types;

/**
 *
 * @author jan
 */
public class PSUtil {

    private PSUtil () {}

    public static void setClob(PreparedStatement ps, int paramIndex, String value) throws SQLException {
        if (value != null) {
            ps.setCharacterStream(paramIndex,
                    new StringReader(value.toString()),
                    (value.toString()).length());
        } else {
            ps.setNull(paramIndex, Types.CLOB);
        }
    }

    public static void set(PreparedStatement ps, int paramIndex, String value) throws SQLException {
        if (value != null) {
            ps.setString(paramIndex, value);
        } else {
            ps.setNull(paramIndex, Types.VARCHAR);
        }
    }

    public static void set(PreparedStatement ps, int paramIndex, byte[] value) throws Exception {
        if (value != null) {
            ps.setBinaryStream(paramIndex, new ByteArrayInputStream(value), value.length);
        } else {
            ps.setNull(paramIndex, Types.BLOB);
        }
    }

    public static void set(PreparedStatement ps, int paramIndex, java.util.Date value) throws SQLException {
        if (value != null) {
            ps.setTimestamp(paramIndex, new Timestamp(value.getTime()));
        } else {
            ps.setNull(paramIndex, Types.TIMESTAMP);
        }
    }

    public static void set(PreparedStatement ps, int paramIndex, double value) throws SQLException {
        ps.setDouble(paramIndex, value);
    }

    public static void set(PreparedStatement ps, int paramIndex, int value) throws SQLException {
        ps.setInt(paramIndex, value);
    }

    public static void set(PreparedStatement ps, int paramIndex, float value) throws SQLException {
        ps.setFloat(paramIndex, value);
    }

    public static void set(PreparedStatement ps, int paramIndex, byte value) throws SQLException {
        ps.setByte(paramIndex, value);
    }

    public static void set(PreparedStatement ps, int paramIndex, char value) throws SQLException {
        ps.setInt(paramIndex, value);
    }

    public static void set(PreparedStatement ps, int paramIndex, boolean value) throws SQLException {
        ps.setBoolean(paramIndex, value);
    }

    public static void setNullForString(PreparedStatement ps, int paramIndex) throws SQLException {
        ps.setNull(paramIndex, Types.VARCHAR);
    }

    public static void setNullForDate(PreparedStatement ps, int paramIndex) throws SQLException {
        ps.setNull(paramIndex, Types.TIMESTAMP);
    }

    public static void setNullForNumber(PreparedStatement ps, int paramIndex) throws SQLException {
        ps.setNull(paramIndex, Types.NUMERIC);
    }

    public static void setNull(PreparedStatement ps, int paramIndex, int type) throws SQLException {
        ps.setNull(paramIndex, type);
    }

}
