package com.noveogroup.android.database;

/*
Dao:
- Cursor
- Bean
- List of beans
- Loader
- Adapter
 */
public final class DaoHelper {

    private DaoHelper() {
        throw new UnsupportedOperationException();
    }

    public static <T, I extends T> T createDaoProxy(I dao, Class<T> daoClass) {
        return null;
    }

    public static <T> T createDao(Class<T> daoClass) {
        return null;
    }

}
