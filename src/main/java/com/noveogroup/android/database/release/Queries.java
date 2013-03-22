package com.noveogroup.android.database.release;

import android.database.Cursor;
import android.database.sqlite.SQLiteDatabase;
import android.widget.BaseAdapter;

public class Queries {

    public static interface Query<S, P> {

        public Cursor query(S source, P param);

    }

    public static interface QueryRunner<S, T, P> {

        public T run(S source, Query<S, P> query, P param);

    }

    public static class SimpleQuery<P> implements Query<SQLiteDatabase, P> {
        @Override
        public Cursor query(SQLiteDatabase source, P param) {
            return null;
        }
    }

    public static class QueryRunnerAdapter<S, P> implements QueryRunner<S, BaseAdapter, P> {

        public BaseAdapter run(S source, Query<S, P> query, P param) {
            return null;
        }

    }

    public static <S, T, P> T run(Query<S, P> query, QueryRunner<S, T, P> runner) {
        return null;
    }

    static {
        run(new SimpleQuery<Void>(), new QueryRunnerAdapter<SQLiteDatabase, Void>());
    }

}
