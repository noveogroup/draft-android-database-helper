package com.noveogroup.android.database.test;

import android.database.sqlite.SQLiteDatabase;

public interface RoleDao {

    @Query("select * from role")
    public Role selectRole(SQLiteDatabase database, @Column("") int id);

}
