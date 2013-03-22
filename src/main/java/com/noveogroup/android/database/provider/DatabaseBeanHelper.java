/*
 * Copyright (c) 2013 Noveo Group
 *
 * Permission is hereby granted, free of charge, to any person obtaining a copy
 * of this software and associated documentation files (the "Software"), to deal
 * in the Software without restriction, including without limitation the rights
 * to use, copy, modify, merge, publish, distribute, sublicense, and/or sell
 * copies of the Software, and to permit persons to whom the Software is
 * furnished to do so, subject to the following conditions:
 *
 * The above copyright notice and this permission notice shall be included in
 * all copies or substantial portions of the Software.
 *
 * Except as contained in this notice, the name(s) of the above copyright holders
 * shall not be used in advertising or otherwise to promote the sale, use or
 * other dealings in this Software without prior written authorization.
 *
 * THE SOFTWARE IS PROVIDED "AS IS", WITHOUT WARRANTY OF ANY KIND, EXPRESS OR
 * IMPLIED, INCLUDING BUT NOT LIMITED TO THE WARRANTIES OF MERCHANTABILITY,
 * FITNESS FOR A PARTICULAR PURPOSE AND NONINFRINGEMENT. IN NO EVENT SHALL
 * THE AUTHORS OR COPYRIGHT HOLDERS BE LIABLE FOR ANY CLAIM, DAMAGES OR OTHER
 * LIABILITY, WHETHER IN AN ACTION OF CONTRACT, TORT OR OTHERWISE, ARISING FROM,
 * OUT OF OR IN CONNECTION WITH THE SOFTWARE OR THE USE OR OTHER DEALINGS IN
 * THE SOFTWARE.
 */

package com.noveogroup.android.database.provider;

import android.content.ContentValues;
import android.database.Cursor;
import com.noveo.android.bean.Bean;
import com.noveo.android.bean.BeanRegistry;
import com.noveo.android.bean.Property;
import com.noveogroup.sql.TableBuilder;
import com.noveogroup.sql.expression.SQLiteDataType;

import java.lang.reflect.InvocationTargetException;
import java.util.*;

public final class DatabaseBeanHelper {

    private DatabaseBeanHelper() {
        throw new UnsupportedOperationException();
    }

    public static <T> T fromCursor(Cursor cursor, Class<T> beanClass) {
        return fromCursor(cursor, BeanRegistry.getInstance().get(beanClass));
    }

    public static <T> T fromCursor(Cursor cursor, Bean<T> bean) {
        try {
            T instance = bean.newBean();

            for (Property<T> property : bean.getProperties()) {
                ColumnName columnName = property.getAnnotation(ColumnName.class);
                if (columnName != null) {
                    int index = cursor.getColumnIndex(columnName.value());
                    if (index != -1) {
                        if (!cursor.isNull(index)) {
                            Class<?> propertyType = property.getType();
                            if (propertyType == Float.class || propertyType == float.class) {
                                property.setValue(instance, cursor.getFloat(index));
                            } else if (propertyType == Integer.class || propertyType == int.class) {
                                property.setValue(instance, cursor.getInt(index));
                            } else if (propertyType == Long.class || propertyType == long.class) {
                                property.setValue(instance, cursor.getLong(index));
                            } else if (propertyType == Boolean.class || propertyType == boolean.class) {
                                property.setValue(instance, cursor.getInt(index) == 1);
                            } else if (propertyType == String.class) {
                                property.setValue(instance, cursor.getString(index));
                            } else if (propertyType == byte[].class) {
                                property.setValue(instance, cursor.getBlob(index));
                            } else if (propertyType.isEnum()) {
                                Enum e = Enum.valueOf((Class<? extends Enum>) propertyType, cursor.getString(index));
                                property.setValue(instance, e);
                            }
                        }
                    }
                }
            }

            return instance;
        } catch (IllegalAccessException e) {
            throw new RuntimeException(e);
        } catch (InvocationTargetException e) {
            throw new RuntimeException(e);
        } catch (InstantiationException e) {
            throw new RuntimeException(e);
        }
    }

    public static <T> ContentValues toContentValues(T instance, Class<T> beanClass) {
        return toContentValues(instance, BeanRegistry.getInstance().get(beanClass), null);
    }

    public static <T> ContentValues toContentValues(T instance, Class<T> beanClass, String[] columns) {
        return toContentValues(instance, BeanRegistry.getInstance().get(beanClass), columns);
    }

    public static <T> ContentValues toContentValues(T instance, Bean<T> bean, String[] columns) {
        try {
            final ContentValues contentValues = new ContentValues();
            final Set<String> columnsSet = (columns != null) ? new HashSet<String>(Arrays.asList(columns)) : null;

            for (Property<T> property : bean.getProperties()) {
                ColumnName columnName = property.getAnnotation(ColumnName.class);
                boolean autoIncrement = property.getAnnotation(AutoIncrement.class) != null;
                if (columnName != null && !autoIncrement) {
                    if (columnsSet != null && !columnsSet.contains(columnName.value())) {
                        continue;
                    }

                    Class<?> propertyType = property.getType();
                    if (propertyType == Float.class || propertyType == float.class) {
                        contentValues.put(columnName.value(), (Float) property.getValue(instance));
                    } else if (propertyType == Integer.class || propertyType == int.class) {
                        contentValues.put(columnName.value(), (Integer) property.getValue(instance));
                    } else if (propertyType == Long.class || propertyType == long.class) {
                        contentValues.put(columnName.value(), (Long) property.getValue(instance));
                    } else if (propertyType == Boolean.class || propertyType == boolean.class) {
                        contentValues.put(columnName.value(), (Boolean) property.getValue(instance));
                    } else if (propertyType == String.class) {
                        contentValues.put(columnName.value(), (String) property.getValue(instance));
                    } else if (propertyType == byte[].class) {
                        contentValues.put(columnName.value(), (byte[]) property.getValue(instance));
                    } else if (propertyType.isEnum()) {
                        contentValues.put(columnName.value(), ((Enum) property.getValue(instance)).name());
                    }
                }
            }

            return contentValues;
        } catch (InvocationTargetException e) {
            throw new RuntimeException(e);
        } catch (IllegalAccessException e) {
            throw new RuntimeException(e);
        }
    }

    public static <T> TableBuilder sqlCreateTable(Class<T> beanClass) {
        return sqlCreateTable(BeanRegistry.getInstance().get(beanClass));
    }


    public static <T> TableBuilder sqlCreateTable(Bean<T> bean) {
        String tableName = bean.getName();
        TableName tableNameAnnotation = bean.getAnnotation(TableName.class);
        if (tableNameAnnotation != null) {
            tableName = tableNameAnnotation.value();
        }

        TableBuilder tableBuilder = TableBuilder.newTable(tableName);
        List<String> primaryKeys = new ArrayList<String>();
        for (Property<T> property : bean.getProperties()) {
            ColumnName columnName = property.getAnnotation(ColumnName.class);

            if (columnName != null) {
                Class<?> propertyType = property.getType();
                SQLiteDataType sqLiteDataType = null;
                boolean nullable = property.getAnnotation(Nullable.class) != null;
                boolean unique = property.getAnnotation(Unique.class) != null;
                ForeignKey foreignKey = property.getAnnotation(ForeignKey.class);
                PrimaryKey primaryKey = property.getAnnotation(PrimaryKey.class);

                if (propertyType == Integer.class || propertyType == int.class
                        || propertyType == Long.class || propertyType == long.class
                        || propertyType == Boolean.class || propertyType == boolean.class) {
                    boolean autoIncrement = property.getAnnotation(AutoIncrement.class) != null;

                    if (autoIncrement) {
                        sqLiteDataType = SQLiteDataType.AUTOINCREMENT;
                    } else {
                        sqLiteDataType = SQLiteDataType.INTEGER;
                    }
                } else if (propertyType == Float.class || propertyType == float.class) {
                    sqLiteDataType = SQLiteDataType.REAL;
                } else if (propertyType == String.class || propertyType.isEnum()) {
                    sqLiteDataType = SQLiteDataType.TEXT;
                } else if (propertyType == byte[].class) {
                    sqLiteDataType = SQLiteDataType.BLOB;
                }

                tableBuilder.column(columnName.value(), sqLiteDataType, nullable, unique);

                if (foreignKey != null) {
                    tableBuilder.foreignKey(foreignKey.toTable(), columnName.value(), foreignKey.toColumn());
                }

                if (primaryKey != null) {
                    primaryKeys.add(columnName.value());
                }
            }


        }

        if (!primaryKeys.isEmpty()) {
            tableBuilder.primaryKey(primaryKeys.toArray(new String[primaryKeys.size()]));
        }

        return tableBuilder;
    }
}