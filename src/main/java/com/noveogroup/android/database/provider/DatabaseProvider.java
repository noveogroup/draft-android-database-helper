package com.noveogroup.android.database.provider;

import android.content.ContentProvider;
import android.content.ContentResolver;
import android.content.ContentValues;
import android.content.UriMatcher;
import android.database.Cursor;
import android.database.sqlite.SQLiteDatabase;
import android.database.sqlite.SQLiteQueryBuilder;
import android.net.Uri;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;

// CR discuss indroducing AbstractContentProvider - for specific table bean
public class DatabaseProvider extends ContentProvider {

    private static final int URI_TYPE_USERS = 1;
    private static final int URI_TYPE_TIMELINE = 2;
    private static final int URI_TYPE_RESOURCES = 3;
    private static final int URI_TYPE_LIKE_LIST = 4;
    private static final int URI_TYPE_COMMENT_LIST = 5;


    private static final UriMatcher URI_MATCHER = new UriMatcher(UriMatcher.NO_MATCH);

    static {
        URI_MATCHER.addURI(AUTHORITY, BASE_PATH_USERS, URI_TYPE_USERS);
        URI_MATCHER.addURI(AUTHORITY, BASE_PATH_TIMELINE, URI_TYPE_TIMELINE);
        URI_MATCHER.addURI(AUTHORITY, BASE_PATH_RESOURCES, URI_TYPE_RESOURCES);
        URI_MATCHER.addURI(AUTHORITY, BASE_PATH_LIKE_LIST, URI_TYPE_LIKE_LIST);
        URI_MATCHER.addURI(AUTHORITY, BASE_PATH_COMMENT_LIST, URI_TYPE_COMMENT_LIST);
    }

    private OpenHelper helper;

    @Override
    public boolean onCreate() {
        helper = OpenHelper.getInstance();
        return true;
    }


    @Override
    public Cursor query(Uri uri, String[] projection, String selection, String[] selectionArgs, String sortOrder) {
        final SQLiteQueryBuilder builder = new SQLiteQueryBuilder();
        final SQLiteDatabase database = helper.getDatabase(getContext());
        final ContentResolver resolver = getContext().getContentResolver();
        final int type = URI_MATCHER.match(uri);
        Cursor cursor = null;


        if (type == UriMatcher.NO_MATCH) {
            throw new IllegalArgumentException("Unsupported uri : " + uri);
        }


        if (type == URI_TYPE_TIMELINE) {
            String query = "SELECT " + COLUMN_USERS_ROW_ID + ", " + COLUMN_USERS_UUID + ", " + COLUMN_USERS_USERNAME + ", " + COLUMN_USERS_HOSTNAME + ", " + COLUMN_USERS_PORT
                    + ", " + COLUMN_MESSAGES_ROW_ID + ", " + COLUMN_MESSAGES_TIMESTAMP + ", " + COLUMN_MESSAGES_UUID
                    + ", " + COLUMN_MESSAGES_DESTINATION_UUID + ", " + COLUMN_MESSAGES_OWNER_UUID + ", " + COLUMN_MESSAGES_TEXT + ", " + COLUMN_MESSAGES_RESOURCE_UUID
                    + ", " + COLUMN_MESSAGES_LIKE_NUMBER + ", " + COLUMN_MESSAGES_COMMENT_NUMBER
                    + ", " + COLUMN_RESOURCES_UUID + ", " + COLUMN_RESOURCES_TYPE + ", " + COLUMN_RESOURCES_HAS_PREVIEW
                    + " FROM " + TABLE_USERS + ", " + TABLE_MESSAGES + " LEFT OUTER JOIN " + TABLE_RESOURCES + " ON " + COLUMN_MESSAGES_RESOURCE_UUID + " = " + COLUMN_RESOURCES_UUID
                    + " WHERE " + COLUMN_MESSAGES_OWNER_UUID + " = " + COLUMN_USERS_UUID + " AND " + COLUMN_MESSAGES_CLOUD_NAME + " = ? " + ((selection != null) ? " AND " + selection : "") + " ORDER BY " + COLUMN_MESSAGES_TIMESTAMP + " DESC";

            cursor = database.rawQuery(query, initSelectionArgs(selectionArgs));

        } else if (type == URI_TYPE_USERS) {
            cursor = database.query(TABLE_USERS, new String[]{COLUMN_USERS_UUID, COLUMN_USERS_ROW_ID, COLUMN_USERS_USERNAME, "(" + COLUMN_USERS_ROW_ID + " = " + CURRENT_USER_ROW_ID + ") is_current_user"}, COLUMN_USERS_HIDDEN_FLAG+" = ?", new String[]{"0"}, null, null, "is_current_user DESC, " + COLUMN_USERS_USERNAME + " ASC");
        } else if (type == URI_TYPE_RESOURCES) {

            String query = "SELECT " + TABLE_RESOURCES + ".*" + ", " + COLUMN_MESSAGES_DESTINATION_UUID
                    + " FROM " + TABLE_RESOURCES + " LEFT OUTER JOIN " + TABLE_MESSAGES + " ON " + COLUMN_MESSAGES_RESOURCE_UUID + " = " + COLUMN_RESOURCES_UUID
                    + " WHERE " + COLUMN_MESSAGES_CLOUD_NAME + " = ? " + ((selection != null) ? " AND " + selection : "");

            cursor = database.rawQuery(query, initSelectionArgs(selectionArgs));
        } else if (type == URI_TYPE_LIKE_LIST) {

            String query = "SELECT " + TABLE_USERS + ".* FROM " +
                    TABLE_LIKES + " INNER JOIN " + TABLE_USERS + " ON " + COLUMN_LIKES_OWNER_UUID + " = " + COLUMN_USERS_UUID +
                    " INNER JOIN " + TABLE_MESSAGES + " ON " + COLUMN_LIKES_MESSAGE_UUID + " = " + COLUMN_MESSAGES_UUID +
                    " WHERE " + COLUMN_MESSAGES_CLOUD_NAME + " = ? AND " + COLUMN_LIKES_MESSAGE_UUID  + " = ? " +
                    ((selection != null) ? " AND " + selection : "") + " ORDER BY " + COLUMN_LIKES_TIMESTAMP + " DESC";

            cursor = database.rawQuery(query, initSelectionArgs(selectionArgs));
        } else if (type == URI_TYPE_COMMENT_LIST) {
            String query = "SELECT " + TABLE_USERS + ".*, " + COLUMN_COMMENTS_TEXT + ", " + COLUMN_COMMENTS_TIMESTAMP + " " +
                    " FROM " + TABLE_COMMENTS + " INNER JOIN " + TABLE_USERS + " ON " + COLUMN_COMMENTS_OWNER_UUID + " = " + COLUMN_USERS_UUID +
                    " INNER JOIN " + TABLE_MESSAGES + " ON " + COLUMN_COMMENTS_MESSAGE_UUID + " = " + COLUMN_MESSAGES_UUID +
                    " WHERE " + COLUMN_MESSAGES_CLOUD_NAME + " = ? AND " + COLUMN_COMMENTS_MESSAGE_UUID  + " = ? " +
                    ((selection != null) ? " AND " + selection : "") + " ORDER BY " + COLUMN_COMMENTS_TIMESTAMP + " DESC";

            cursor = database.rawQuery(query, initSelectionArgs(selectionArgs));
        }


        cursor.setNotificationUri(resolver, uri);
        return cursor;
    }

    private String[] initSelectionArgs(String[] selectionArgs) {
        List<String> args = new ArrayList<String>();
        args.add(Preferences.getCloudName(getContext()));
        if (selectionArgs != null) {
            args.addAll(Arrays.asList(selectionArgs));
        }
        return args.toArray(new String[args.size()]);
    }

    // CR return a type

    @Override
    public String getType(Uri uri) {

        return null;
    }

    // CR discuss: throw UnsupportedOperationException

    @Override
    public Uri insert(Uri uri, ContentValues values) {
        return null;
    }

    @Override
    public int delete(Uri uri, String selection, String[] selectionArgs) {

        return 0;
    }

    @Override
    public int update(Uri uri, ContentValues values, String selection, String[] selectionArgs) {


        return 0;
    }
}
