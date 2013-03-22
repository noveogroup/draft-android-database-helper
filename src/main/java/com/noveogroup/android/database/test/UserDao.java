package com.noveogroup.android.database.test;

import android.database.Cursor;
import android.database.sqlite.SQLiteDatabase;

public interface UserDao {

    public Cursor selectUser(SQLiteDatabase db);

    public static class UserDaoImpl implements UserDao {

        @Override
        public Cursor selectUser(SQLiteDatabase db) {
            return null;
        }

    }

}
