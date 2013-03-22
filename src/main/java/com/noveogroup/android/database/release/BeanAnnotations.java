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

package com.noveogroup.android.database.release;

import android.content.ContentValues;
import android.database.Cursor;
import android.os.Bundle;

import java.lang.annotation.ElementType;
import java.lang.annotation.Retention;
import java.lang.annotation.RetentionPolicy;
import java.lang.annotation.Target;

public class BeanAnnotations {

    public interface Serializator<T> {

        public T load(Cursor cursor);

    }

    public enum Type {
        TEXT, INTEGER, BLOB, REAL, AUTOINCREMENT
    }

    @Target({ElementType.TYPE})
    @Retention(RetentionPolicy.RUNTIME)
    public @interface Table {

        public String name();

    }

    @Target({ElementType.FIELD})
    @Retention(RetentionPolicy.RUNTIME)
    public @interface Column {

        public String name() default "";

        public Type type() default Type.TEXT;

        public String rawSql() default ""; // there should be a lot of such rawSql

        public String value() default "";

        public boolean nullable() default true;
    }

    @Target({ElementType.FIELD, ElementType.TYPE})
    @Retention(RetentionPolicy.RUNTIME)
    public @interface Serializer {

        public Class<? extends Serializator> value() default Serializator.class; // to save/load column to table manually

    }

    @Target({ElementType.FIELD})
    @Retention(RetentionPolicy.RUNTIME)
    public @interface PrimaryKey {
    }

    @Target({ElementType.FIELD})
    @Retention(RetentionPolicy.RUNTIME)
    public @interface ForeignKey {

        public String group() default ""; // for key groups. user can choose any

        public String table();

        public String column() default "";

    }

    @Target({ElementType.FIELD})
    @Retention(RetentionPolicy.RUNTIME)
    public @interface Unique {

        public String group() default ""; // for groups. user can choose any

    }

    @Target({ElementType.FIELD})
    @Retention(RetentionPolicy.RUNTIME)
    public @interface Index {

        public String name() default ""; // for groups. user can choose any

        public boolean unique() default false;

    }

    public static class ExampleBean {

        @Column()
        @PrimaryKey
        private int id;

        @PrimaryKey
        private int blahId;

        @ForeignKey(table = "Test")
        private int testId;
    }


    public static interface BeanLoader<T> {

        public T load(Cursor cursor, T bean, Bundle bundle);

    }

    public static interface BeanSaver<T> {

        public ContentValues save(ContentValues values, T bean, Bundle bundle);

    }

    // singleton
    public static class BeanMapperRegistry {
    }

}
