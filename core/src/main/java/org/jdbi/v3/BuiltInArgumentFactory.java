/*
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 * http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */
package org.jdbi.v3;

import java.math.BigDecimal;
import java.net.URL;
import java.sql.Blob;
import java.sql.Clob;
import java.sql.PreparedStatement;
import java.sql.SQLException;
import java.sql.Time;
import java.sql.Timestamp;
import java.sql.Types;
import java.util.Arrays;
import java.util.IdentityHashMap;
import java.util.Map;

import org.jdbi.v3.tweak.Argument;
import org.jdbi.v3.tweak.ArgumentFactory;

/**
 * The BuiltInArgumentFactory provides instances of {@link Argument} for
 * many core Java types.  Generally you should not need to use this
 * class directly, but instead should bind your object with the
 * {@link SQLStatement} convenience methods.
 */
public class BuiltInArgumentFactory<T> implements ArgumentFactory<T> {

    public static final ArgumentFactory<?> INSTANCE = new BuiltInArgumentFactory<Object>();

    private static final Map<Class<?>, ArgBuilder<?>> BUILDERS = new IdentityHashMap<>();
    private static final ArgBuilder<String> STR_BUILDER = v -> new BuiltInArgument<>(String.class, Types.VARCHAR, PreparedStatement::setString, v);
    private static final ArgBuilder<Object> OBJ_BUILDER = v -> new BuiltInArgument<>(Object.class, Types.NULL, PreparedStatement::setObject, v);

    /**
     * Create an Argument for a built in type.  If the type is not recognized,
     * the result will delegate to {@link PreparedStatement#setObject(int, Object)}.
     */
    @SuppressWarnings("unchecked")
    public static Argument build(Object arg) {
        return ((ArgumentFactory<Object>)INSTANCE).build(arg == null ? Object.class : arg.getClass(), arg, null);
    }

    private static <T> void register(Class<T> klass, int type, StatementBinder<T> binder) {
        register(klass, v -> new BuiltInArgument<T>(klass, type, binder, v));
    }

    private static <T> void register(Class<T> klass, ArgBuilder<T> builder) {
        BUILDERS.put(klass, builder);
    }

    private static <T> StatementBinder<T> stringify(StatementBinder<String> real) {
        return (p, i, v) -> real.bind(p, i, String.valueOf(v));
    }

    static {
        register(BigDecimal.class, Types.NUMERIC, PreparedStatement::setBigDecimal);
        register(Blob.class, Types.BLOB, PreparedStatement::setBlob);
        register(Boolean.class, Types.BOOLEAN, PreparedStatement::setBoolean);
        register(boolean.class, Types.BOOLEAN, PreparedStatement::setBoolean);
        register(Byte.class, Types.TINYINT, PreparedStatement::setByte);
        register(byte.class, Types.TINYINT, PreparedStatement::setByte);
        register(byte[].class, Types.VARBINARY, PreparedStatement::setBytes);
        register(Character.class, Types.CHAR, stringify(PreparedStatement::setString));
        register(char.class, Types.CHAR, stringify(PreparedStatement::setString));
        register(Clob.class, Types.CLOB, PreparedStatement::setClob);
        register(Double.class, Types.DOUBLE, PreparedStatement::setDouble);
        register(double.class, Types.DOUBLE, PreparedStatement::setDouble);
        register(Float.class, Types.FLOAT, PreparedStatement::setFloat);
        register(float.class, Types.FLOAT, PreparedStatement::setFloat);
        register(Integer.class, Types.INTEGER, PreparedStatement::setInt);
        register(int.class, Types.INTEGER, PreparedStatement::setInt);
        register(java.util.Date.class, Types.TIMESTAMP, (p, i, v) -> p.setTimestamp(i, new Timestamp(v.getTime())));
        register(Long.class, Types.INTEGER, PreparedStatement::setLong);
        register(long.class, Types.INTEGER, PreparedStatement::setLong);
        register(Object.class, OBJ_BUILDER);
        register(Short.class, Types.SMALLINT, PreparedStatement::setShort);
        register(short.class, Types.SMALLINT, PreparedStatement::setShort);
        register(java.sql.Date.class, Types.DATE, PreparedStatement::setDate);
        register(String.class, STR_BUILDER);
        register(Time.class, Types.TIME, PreparedStatement::setTime);
        register(Timestamp.class, Types.TIMESTAMP, PreparedStatement::setTimestamp);
        register(URL.class, Types.DATALINK, PreparedStatement::setURL);
    }

    @Override
    public boolean accepts(Class<?> expectedType, Object value, StatementContext ctx)
    {
        return BUILDERS.containsKey(expectedType) || value == null || value.getClass().isEnum();
    }

    @SuppressWarnings("unchecked")
    @Override
    public Argument build(Class<?> expectedType, T value, StatementContext ctx)
    {
        ArgBuilder<T> p = (ArgBuilder<T>) BUILDERS.get(expectedType);

        if (value != null && expectedType == Object.class) {
            ArgBuilder<T> v = (ArgBuilder<T>) BUILDERS.get(value.getClass());
            if (v != null) {
                return v.build(value);
            }

            if (value.getClass().isEnum()) {
                return STR_BUILDER.build((((Enum<?>)value).name()));
            }
        }

        if (p != null) {
            return p.build(value);
        }

        // Enums must be bound as VARCHAR.
        if (expectedType.isEnum()) {
            return STR_BUILDER.build(value.toString());
        }

        // Fallback to generic ObjectArgument
        return OBJ_BUILDER.build(value);
    }

    @FunctionalInterface
    interface StatementBinder<T> {
        void bind(PreparedStatement p, int index, T value) throws SQLException;
    }

    @FunctionalInterface
    interface ArgBuilder<T> {
        Argument build(final T value);
    }

    static final class BuiltInArgument<T> implements Argument {
        private final T value;
        private final boolean isArray;
        private final int type;
        private final StatementBinder<T> binder;

        private BuiltInArgument(Class<T> klass, int type, StatementBinder<T> binder, T value) {
            this.binder = binder;
            this.isArray = klass.isArray();
            this.type = type;
            this.value = value;
        }

        @Override
        public void apply(int position, PreparedStatement statement, StatementContext ctx) throws SQLException {
            if (value == null) {
                statement.setNull(position, type);
                return;
            }
            binder.bind(statement, position, value);
        }

        @Override
        public String toString() {
            if (isArray) {
                return Arrays.toString((Object[]) value);
            }
            return String.valueOf(value);
        }

        StatementBinder<T> getStatementBinder() {
            return binder;
        }
    }
}
