package com.noveogroup.android.database.provider;


import android.content.Context;
import android.database.Cursor;
import android.database.sqlite.SQLiteDatabase;
import android.text.TextUtils;

import java.util.ArrayList;
import java.util.List;


public final class DatabaseUtils {
    private DatabaseUtils() {
        new UnsupportedOperationException();
    }

    public static void insertResource(Context context, final Resource resource) {
        final SQLiteDatabase database = OpenHelper.getInstance().getDatabase(context);
        TransactionHelper.transaction(database, new TransactionHelper.TransactionCallback() {
            @Override
            public void doInTransaction() {
                database.insertOrThrow(TABLE_RESOURCES, null, DatabaseBeanHelper.toContentValues(resource, Resource.class));
            }
        });
        context.getContentResolver().notifyChange(DatabaseContract.CONTENT_URI_TIMELINE, null);
        context.getContentResolver().notifyChange(DatabaseContract.CONTENT_URI_RESOURCES, null);
    }

    public static void insertMessage(final Context context, final Message message) {
        final SQLiteDatabase database = OpenHelper.getInstance().getDatabase(context);
        TransactionHelper.transaction(database, new TransactionHelper.TransactionCallback() {
            @Override
            public void doInTransaction() {
                long timestamp = message.getTimestamp();
                timestamp = Math.max(timestamp, getMaxMessageTimestamp() + 1);
                message.setTimestamp(timestamp);
                message.setUuid(timestamp);
                message.setCloudName(Preferences.getCloudName(context));
                final Resource resource = message.getResource();
                if (resource != null) {
                    resource.setOwner(message.getOwnerUUID());
                    message.setResourceUuid(message.getResource().getUuid());
                    insertResource(context, resource);
                }
                database.insert(TABLE_MESSAGES, null, DatabaseBeanHelper.toContentValues(message, Message.class));
            }
        });
        context.getContentResolver().notifyChange(DatabaseContract.CONTENT_URI_TIMELINE, null);
        context.getContentResolver().notifyChange(DatabaseContract.CONTENT_URI_MESSAGES, null);
    }

    public static void insertIncomingMessage(final Context context, final Message message) {
        message.setSendFlag(true);
        message.setUuid(message.getTimestamp());
        message.setCloudName(Preferences.getCloudName(context));
        message.setTimestamp(TimeSynchronisationUtils.calculateLocalMessageTimestamp(context, message));
        final SQLiteDatabase database = OpenHelper.getInstance().getDatabase(context);
        TransactionHelper.transaction(database, new TransactionHelper.TransactionCallback() {
            @Override
            public void doInTransaction() {
                final Resource resource = message.getResource();
                if (resource != null) {
                    resource.setOwner(message.getOwnerUUID());
                    message.setResourceUuid(message.getResource().getUuid());
                    insertResource(context, resource);
                }
                database.insert(TABLE_MESSAGES, null, DatabaseBeanHelper.toContentValues(message, Message.class));
            }
        });
        context.getContentResolver().notifyChange(DatabaseContract.CONTENT_URI_TIMELINE, null);
        context.getContentResolver().notifyChange(DatabaseContract.CONTENT_URI_RESOURCES, null);
    }

    public static boolean messageExists(Message message) {
        final SQLiteDatabase database = OpenHelper.getInstance().getDatabase();
        Cursor cursor = null;
        try {
            cursor = database.query(TABLE_MESSAGES, new String[]{"" + COLUMN_MESSAGES_ROW_ID}, COLUMN_MESSAGES_UUID + " = ? AND " + COLUMN_MESSAGES_TEXT + " = ? AND " + COLUMN_MESSAGES_OWNER_UUID + " = ?", new String[]{Long.toString(message.getUuid()), message.getText(), message.getOwnerUUID()}, null, null, null, null);
            return cursor.moveToFirst();
        } finally {
            cursor.close();
        }


    }

    public static void insertIncomingMessages(final Context context, final List<Message> messages) {

        final SQLiteDatabase database = OpenHelper.getInstance().getDatabase(context);
        TransactionHelper.transaction(database, new TransactionHelper.TransactionCallback() {
            @Override
            public void doInTransaction() {
                for (final Message message : messages) {
                    message.setUuid(message.getTimestamp());
                    message.setTimestamp(TimeSynchronisationUtils.calculateLocalMessageTimestamp(context, message));
                    message.setCloudName(Preferences.getCloudName(context));
                    if (!messageExists(message)) {

                        message.setSendFlag(true);
                        final Resource resource = message.getResource();
                        if (resource != null) {
                            resource.setOwner(message.getOwnerUUID());
                            message.setResourceUuid(message.getResource().getUuid());
                            database.insert(TABLE_RESOURCES, null, DatabaseBeanHelper.toContentValues(resource, Resource.class));
                        }
                        database.insert(TABLE_MESSAGES, null, DatabaseBeanHelper.toContentValues(message, Message.class));
                    }
                }

//                database.rawQuery("DELETE FROM " + TABLE_MESSAGES + " WHERE " + COLUMN_MESSAGES_ROW_ID
//                        + " NOT IN (SELECT MIN(" + COLUMN_MESSAGES_ROW_ID + ") FROM " + TABLE_MESSAGES
//                        + " GROUP BY " + COLUMN_MESSAGES_TIMESTAMP + ", " + COLUMN_MESSAGES_TEXT + ", " + COLUMN_MESSAGES_OWNER_UUID + ")", null);
            }
        });
        context.getContentResolver().notifyChange(DatabaseContract.CONTENT_URI_TIMELINE, null);
        context.getContentResolver().notifyChange(DatabaseContract.CONTENT_URI_RESOURCES, null);

    }

    public static long getMaxMessageTimestamp() {
        final List<Long> out = new ArrayList<Long>();
        final SQLiteDatabase database = OpenHelper.getInstance().getDatabase();

        TransactionHelper.transaction(database, new TransactionHelper.TransactionCallback() {
            @Override
            public void doInTransaction() {
                final Cursor cursor = database.rawQuery("SELECT MAX(" + COLUMN_MESSAGES_TIMESTAMP + ") FROM " + TABLE_MESSAGES, null);
                try {
                    if (cursor.moveToFirst()) {
                        out.add(cursor.getLong(0));
                    }

                } finally {
                    cursor.close();
                }
            }
        });

        return out.isEmpty() ? 0 : out.get(0);
    }

    public static long getMaxCommentTimestamp(final String messageUuid) {
        final List<Long> out = new ArrayList<Long>();
        final SQLiteDatabase database = OpenHelper.getInstance().getDatabase();

        TransactionHelper.transaction(database, new TransactionHelper.TransactionCallback() {
            @Override
            public void doInTransaction() {
                final Cursor cursor = database.rawQuery("SELECT MAX(" + COLUMN_COMMENTS_TIMESTAMP + ") FROM " + TABLE_COMMENTS +
                        " WHERE " + COLUMN_COMMENTS_MESSAGE_UUID + " = ?", new String[]{messageUuid});

                try {
                    if (cursor.moveToFirst()) {
                        out.add(cursor.getLong(0));
                    }

                } finally {
                    cursor.close();
                }
            }
        });

        return out.isEmpty() ? 0 : out.get(0);
    }

    public static List<Message> queryUnsentMessages(Context context) {
        final List<Message> messages = new ArrayList<Message>();
        final SQLiteDatabase database = OpenHelper.getInstance().getDatabase(context);

        TransactionHelper.transaction(database, new TransactionHelper.TransactionCallback() {
            @Override
            public void doInTransaction() {
                String query = "SELECT " + TextUtils.join(", ", new String[]{TABLE_MESSAGES + "." + COLUMN_MESSAGES_ROW_ID, COLUMN_MESSAGES_UUID, COLUMN_MESSAGES_OWNER_UUID, COLUMN_MESSAGES_DESTINATION_UUID, COLUMN_MESSAGES_TEXT, COLUMN_MESSAGES_TIMESTAMP, COLUMN_MESSAGES_RESOURCE_UUID, COLUMN_RESOURCES_PREVIEW, COLUMN_RESOURCES_TYPE, COLUMN_RESOURCES_OWNER_UUID, COLUMN_RESOURCES_UUID})
                        + " FROM " + TABLE_MESSAGES + " LEFT OUTER JOIN " + TABLE_RESOURCES + " ON " + COLUMN_MESSAGES_RESOURCE_UUID + " = " + COLUMN_RESOURCES_UUID + " WHERE " + COLUMN_MESSAGES_SEND_FLAG + " = ?";
                final Cursor cursor = database.rawQuery(query, new String[]{Integer.toString(0)});
                try {
                    for (boolean hasItem = cursor.moveToFirst(); hasItem; hasItem = cursor.moveToNext()) {
                        final Message message = DatabaseBeanHelper.fromCursor(cursor, Message.class);
                        if (message.getResourceUuid() != null) {
                            message.setResource(DatabaseBeanHelper.fromCursor(cursor, Resource.class));
                        }
                        messages.add(message);
                    }
                } finally {
                    cursor.close();
                }


            }
        });

        return messages;
    }

    // TODO query resource with obj
    public static List<Message> queryUserMessages(final Context context, final User user) {
        final List<Message> messages = new ArrayList<Message>();
        final SQLiteDatabase database = OpenHelper.getInstance().getDatabase();

        TransactionHelper.transaction(database, new TransactionHelper.TransactionCallback() {
            @Override
            public void doInTransaction() {

                String query = "SELECT " + TextUtils.join(", ", new String[]{COLUMN_MESSAGES_UUID, COLUMN_MESSAGES_OWNER_UUID, COLUMN_MESSAGES_DESTINATION_UUID, COLUMN_MESSAGES_TEXT, COLUMN_MESSAGES_TIMESTAMP, COLUMN_MESSAGES_RESOURCE_UUID, COLUMN_RESOURCES_PREVIEW, COLUMN_RESOURCES_TYPE, COLUMN_RESOURCES_OWNER_UUID, COLUMN_RESOURCES_UUID})
                        + " FROM " + TABLE_MESSAGES + " LEFT OUTER JOIN " + TABLE_RESOURCES + " ON " + COLUMN_MESSAGES_RESOURCE_UUID + " = " + COLUMN_RESOURCES_UUID
                        + " WHERE " + COLUMN_MESSAGES_OWNER_UUID + " = ? AND " + COLUMN_MESSAGES_CLOUD_NAME + " = ?";
                final Cursor cursor = database.rawQuery(query, new String[]{user.getUuid(), Preferences.getCloudName(context)});

                try {
                    for (boolean hasItem = cursor.moveToFirst(); hasItem; hasItem = cursor.moveToNext()) {
                        final Message message = DatabaseBeanHelper.fromCursor(cursor, Message.class);
                        if (message.getResourceUuid() != null) {
                            message.setResource(DatabaseBeanHelper.fromCursor(cursor, Resource.class));
                        }
                        messages.add(message);
                    }
                } finally {
                    cursor.close();
                }
            }
        });

        return messages;
    }

    public static void updateMessagesSendFlag(Context context, final List<Message> messages) {
        final SQLiteDatabase database = OpenHelper.getInstance().getDatabase(context);

        TransactionHelper.transaction(database, new TransactionHelper.TransactionCallback() {
            @Override
            public void doInTransaction() {
                for (Message message : messages) {
                    database.update(TABLE_MESSAGES, DatabaseBeanHelper.toContentValues(message, Message.class, new String[]{COLUMN_MESSAGES_SEND_FLAG}), COLUMN_MESSAGES_ROW_ID + " = ?", new String[]{Long.toString(message.getId())});
                }
            }
        });
    }

    public static void setMessageLikeNumber(Context context, final Message message, final int likeNumber) {
        final SQLiteDatabase database = OpenHelper.getInstance().getDatabase(context);

        TransactionHelper.transaction(database, new TransactionHelper.TransactionCallback() {
            @Override
            public void doInTransaction() {
                if (message != null) {
                    message.setLikeNumber(likeNumber);

                    database.update(TABLE_MESSAGES,
                            DatabaseBeanHelper.toContentValues(message, Message.class, new String[]{COLUMN_MESSAGES_LIKE_NUMBER}),
                            COLUMN_MESSAGES_UUID + " = ?",
                            new String[]{String.valueOf(message.getUuid())});
                }
            }
        });
    }

    public static void setMessageCommentNumber(Context context, final Message message, final int commentNumber) {
        final SQLiteDatabase database = OpenHelper.getInstance().getDatabase(context);

        TransactionHelper.transaction(database, new TransactionHelper.TransactionCallback() {
            @Override
            public void doInTransaction() {
                if (message != null) {
                    message.setCommentNumber(commentNumber);

                    database.update(TABLE_MESSAGES,
                            DatabaseBeanHelper.toContentValues(message, Message.class, new String[]{COLUMN_MESSAGES_COMMENT_NUMBER}),
                            COLUMN_MESSAGES_UUID + " = ?",
                            new String[]{String.valueOf(message.getUuid())});
                }
            }
        });
    }

    /**
     * Return all users except CURRENT_USER object
     *
     * @param context the context
     * @return the user list
     */
    public static List<User> queryUsersInfo(Context context) {
        final List<User> users = new ArrayList<User>();
        final SQLiteDatabase database = OpenHelper.getInstance().getDatabase(context);

        TransactionHelper.transaction(database, new TransactionHelper.TransactionCallback() {
            @Override
            public void doInTransaction() {
                final Cursor cursor = database.query(TABLE_USERS, new String[]{COLUMN_USERS_ROW_ID, COLUMN_USERS_UUID, COLUMN_USERS_HOSTNAME, COLUMN_USERS_PORT}, COLUMN_USERS_ROW_ID + " != ?", new String[]{Long.toString(CURRENT_USER_ROW_ID)}, null, null, null);
                try {
                    for (boolean hasItem = cursor.moveToFirst(); hasItem; hasItem = cursor.moveToNext()) {
                        users.add(DatabaseBeanHelper.fromCursor(cursor, User.class));
                    }
                } finally {
                    cursor.close();
                }
            }
        });

        return users;
    }

    // CR ensure that database is initialized and use getDatabase without context
    public static User getCurrentUser() {
        final List<User> users = new ArrayList<User>();
        final SQLiteDatabase database = OpenHelper.getInstance().getDatabase();

        TransactionHelper.transaction(database, new TransactionHelper.TransactionCallback() {
            @Override
            public void doInTransaction() {
                final Cursor cursor = database.query(TABLE_USERS, null, COLUMN_USERS_ROW_ID + " = ?", new String[]{Long.toString(CURRENT_USER_ROW_ID)}, null, null, null);
                try {
                    if (cursor.moveToFirst()) {
                        users.add(DatabaseBeanHelper.fromCursor(cursor, User.class));
                    }

                } finally {
                    cursor.close();
                }
            }
        });

        return users.isEmpty() ? null : users.get(0);
    }


    public static User getCurrentUserFull(final Context context) {
        final List<User> users = new ArrayList<User>();
        final SQLiteDatabase database = OpenHelper.getInstance().getDatabase();

        TransactionHelper.transaction(database, new TransactionHelper.TransactionCallback() {
            @Override
            public void doInTransaction() {
                User user = null;
                final Cursor cursor = database.query(TABLE_USERS, null, COLUMN_USERS_ROW_ID + " = ?", new String[]{Long.toString(CURRENT_USER_ROW_ID)}, null, null, null);
                try {
                    if (cursor.moveToFirst()) {
                        user = DatabaseBeanHelper.fromCursor(cursor, User.class);
                    }

                } finally {
                    cursor.close();
                }
                user.setMessages(queryUserMessages(context, user));
                users.add(user);

            }
        });

        return users.isEmpty() ? null : users.get(0);
    }

    public static User getUserConnectionInfo(Context context, final String uuid) {
        final List<User> users = new ArrayList<User>();
        final SQLiteDatabase database = OpenHelper.getInstance().getDatabase(context);

        TransactionHelper.transaction(database, new TransactionHelper.TransactionCallback() {
            @Override
            public void doInTransaction() {
                final Cursor cursor = database.query(TABLE_USERS, new String[]{COLUMN_USERS_ROW_ID, COLUMN_USERS_UUID, COLUMN_USERS_HOSTNAME, COLUMN_USERS_PORT}, COLUMN_USERS_UUID + " = ?", new String[]{uuid}, null, null, null);
                try {
                    if (cursor.moveToFirst()) {
                        users.add(DatabaseBeanHelper.fromCursor(cursor, User.class));
                    }

                } finally {
                    cursor.close();
                }
            }
        });

        return users.isEmpty() ? null : users.get(0);
    }

    public static User getUser(Context context, final String uuid) {
        final List<User> users = new ArrayList<User>();
        final SQLiteDatabase database = OpenHelper.getInstance().getDatabase(context);

        TransactionHelper.transaction(database, new TransactionHelper.TransactionCallback() {
            @Override
            public void doInTransaction() {
                final Cursor cursor = database.query(TABLE_USERS, null, COLUMN_USERS_UUID + " = ?", new String[]{uuid}, null, null, null);
                try {
                    if (cursor.moveToFirst()) {
                        users.add(DatabaseBeanHelper.fromCursor(cursor, User.class));
                    }

                } finally {
                    cursor.close();
                }
            }
        });

        return users.isEmpty() ? null : users.get(0);
    }

    public static List<User> getUnavailableUsers() {
        final List<User> users = new ArrayList<User>();
        final SQLiteDatabase database = OpenHelper.getInstance().getDatabase();

        TransactionHelper.transaction(database, new TransactionHelper.TransactionCallback() {
            @Override
            public void doInTransaction() {
                final Cursor cursor = database.query(TABLE_USERS, new String[]{COLUMN_USERS_UUID}, COLUMN_USERS_AVAILABLE_FLAG + " = ? AND " + COLUMN_USERS_ROW_ID + " != ?", new String[]{"0", Long.toString(CURRENT_USER_ROW_ID)}, null, null, null);
                try {
                    for (boolean hasItem = cursor.moveToFirst(); hasItem; hasItem = cursor.moveToNext()) {
                        users.add(DatabaseBeanHelper.fromCursor(cursor, User.class));
                    }

                } finally {
                    cursor.close();
                }
            }
        });

        return users;
    }

    public static User getUser(Context context, final long id) {
        final List<User> users = new ArrayList<User>();
        final SQLiteDatabase database = OpenHelper.getInstance().getDatabase(context);

        TransactionHelper.transaction(database, new TransactionHelper.TransactionCallback() {
            @Override
            public void doInTransaction() {
                final Cursor cursor = database.query(TABLE_USERS, null, COLUMN_USERS_ROW_ID + " = ?", new String[]{Long.toString(id)}, null, null, null);
                try {
                    if (cursor.moveToFirst()) {
                        users.add(DatabaseBeanHelper.fromCursor(cursor, User.class));
                    }

                } finally {
                    cursor.close();
                }
            }
        });

        return users.isEmpty() ? null : users.get(0);
    }

    public static void updateUser(Context context, final User user) {
        final SQLiteDatabase database = OpenHelper.getInstance().getDatabase(context);

        TransactionHelper.transaction(database, new TransactionHelper.TransactionCallback() {
            @Override
            public void doInTransaction() {

                database.update(TABLE_USERS, DatabaseBeanHelper.toContentValues(user, User.class), COLUMN_USERS_UUID + " = ?", new String[]{user.getUuid()});

            }
        });
        ImageCache.getInstance().remove(user.getUuid());
        context.getContentResolver().notifyChange(DatabaseContract.CONTENT_URI_USERS, null);
        context.getContentResolver().notifyChange(DatabaseContract.CONTENT_URI_TIMELINE, null);
    }

    public static void clearAvailabilityStatus() {
        final SQLiteDatabase database = OpenHelper.getInstance().getDatabase();

        TransactionHelper.transaction(database, new TransactionHelper.TransactionCallback() {
            @Override
            public void doInTransaction() {
                User user = new User();
                user.setAvailable(false);
                database.update(TABLE_USERS, DatabaseBeanHelper.toContentValues(user, User.class, new String[]{COLUMN_USERS_AVAILABLE_FLAG}), null, null);

            }
        });
    }

    public static void updateUserAvailabilityStatus(final User user) {
        final SQLiteDatabase database = OpenHelper.getInstance().getDatabase();

        TransactionHelper.transaction(database, new TransactionHelper.TransactionCallback() {
            @Override
            public void doInTransaction() {

                database.update(TABLE_USERS, DatabaseBeanHelper.toContentValues(user, User.class, new String[]{COLUMN_USERS_AVAILABLE_FLAG}), COLUMN_USERS_UUID + " = ?", new String[]{user.getUuid()});

            }
        });
    }

    public static User getUserByAddress(final String address) {
        final List<User> users = new ArrayList<User>();
        final SQLiteDatabase database = OpenHelper.getInstance().getDatabase();

        TransactionHelper.transaction(database, new TransactionHelper.TransactionCallback() {
            @Override
            public void doInTransaction() {
                final Cursor cursor = database.query(TABLE_USERS, new String[]{COLUMN_USERS_ROW_ID, COLUMN_USERS_UUID}, COLUMN_USERS_HOSTNAME + " = ?", new String[]{address}, null, null, null);
                try {
                    if (cursor.moveToFirst()) {
                        users.add(DatabaseBeanHelper.fromCursor(cursor, User.class));
                    }

                } finally {
                    cursor.close();
                }
            }
        });

        return users.isEmpty() ? null : users.get(0);
    }


    public static void updateUserConnectionInfo(Context context, final User user) {
        final SQLiteDatabase database = OpenHelper.getInstance().getDatabase(context);

        TransactionHelper.transaction(database, new TransactionHelper.TransactionCallback() {
            @Override
            public void doInTransaction() {

                database.update(TABLE_USERS, DatabaseBeanHelper.toContentValues(user, User.class, new String[]{COLUMN_USERS_HOSTNAME, COLUMN_USERS_PORT}), COLUMN_USERS_UUID + " = ?", new String[]{user.getUuid()});

            }
        });

        context.getContentResolver().notifyChange(DatabaseContract.CONTENT_URI_USERS, null);
    }

    public static void updateOrInsertUser(Context context, final User user) {
        final SQLiteDatabase database = OpenHelper.getInstance().getDatabase(context);

        user.setLocalTimestamp(System.currentTimeMillis());

        TransactionHelper.transaction(database, new TransactionHelper.TransactionCallback() {
            @Override
            public void doInTransaction() {
                boolean updateStatus = database.update(TABLE_USERS, DatabaseBeanHelper.toContentValues(user, User.class), COLUMN_USERS_UUID + " = ?", new String[]{user.getUuid()}) > 0;
                if (!updateStatus) {
                    database.insert(TABLE_USERS, null, DatabaseBeanHelper.toContentValues(user, User.class));
                }
            }
        });
        ImageCache.getInstance().remove(user.getUuid());
        context.getContentResolver().notifyChange(DatabaseContract.CONTENT_URI_USERS, null);
        context.getContentResolver().notifyChange(DatabaseContract.CONTENT_URI_TIMELINE, null);
    }


    public static void updateCurrentUser(Context context, final User user) {
        final SQLiteDatabase database = OpenHelper.getInstance().getDatabase(context);

        TransactionHelper.transaction(database, new TransactionHelper.TransactionCallback() {
            @Override
            public void doInTransaction() {
                database.update(TABLE_USERS, DatabaseBeanHelper.toContentValues(user, User.class), COLUMN_USERS_ROW_ID + " = ?", new String[]{Long.toString(CURRENT_USER_ROW_ID)});
            }
        });
        ImageCache.getInstance().remove(user.getUuid());
        context.getContentResolver().notifyChange(DatabaseContract.CONTENT_URI_PROFILE, null);
        context.getContentResolver().notifyChange(DatabaseContract.CONTENT_URI_TIMELINE, null);
        context.getContentResolver().notifyChange(DatabaseContract.CONTENT_URI_USERS, null);
    }

    public static void insertUser(final Context context, final User user) {
        final SQLiteDatabase database = OpenHelper.getInstance().getDatabase(context);

        user.setLocalTimestamp(System.currentTimeMillis());

        TransactionHelper.transaction(database, new TransactionHelper.TransactionCallback() {
            @Override
            public void doInTransaction() {
                database.insert(TABLE_USERS, null, DatabaseBeanHelper.toContentValues(user, User.class));
            }
        });

        context.getContentResolver().notifyChange(DatabaseContract.CONTENT_URI_USERS, null);
    }

    public static void deleteUser(final Context context, final User user) {
        final SQLiteDatabase database = OpenHelper.getInstance().getDatabase(context);

        TransactionHelper.transaction(database, new TransactionHelper.TransactionCallback() {
            @Override
            public void doInTransaction() {
                database.delete(TABLE_USERS, COLUMN_USERS_UUID + " = ?", new String[]{user.getUuid()});
                database.delete(TABLE_MESSAGES, COLUMN_MESSAGES_OWNER_UUID + " = ?", new String[]{user.getUuid()});
                database.delete(TABLE_RESOURCES, COLUMN_RESOURCES_OWNER_UUID + " = ?", new String[]{user.getUuid()});
            }
        });

        context.getContentResolver().notifyChange(DatabaseContract.CONTENT_URI_USERS, null);
        context.getContentResolver().notifyChange(DatabaseContract.CONTENT_URI_TIMELINE, null);
        context.getContentResolver().notifyChange(DatabaseContract.CONTENT_URI_RESOURCES, null);
    }

    public static void loadUserPhoto(final Context context, final User user) {
        final SQLiteDatabase database = OpenHelper.getInstance().getDatabase(context);

        TransactionHelper.transaction(database, new TransactionHelper.TransactionCallback() {
            @Override
            public void doInTransaction() {
                final Cursor cursor = database.query(TABLE_USERS, new String[]{COLUMN_USERS_PHOTO}, COLUMN_USERS_UUID + " = ?", new String[]{user.getUuid()}, null, null, null);
                try {
                    if (cursor.moveToFirst()) {

                        user.setAvatar(cursor.getBlob(0));
                    }

                } finally {
                    cursor.close();
                }
            }
        });

    }

    public static void loadResourcePreview(final Context context, final Resource resource) {
        final SQLiteDatabase database = OpenHelper.getInstance().getDatabase(context);

        TransactionHelper.transaction(database, new TransactionHelper.TransactionCallback() {
            @Override
            public void doInTransaction() {
                final Cursor cursor = database.query(TABLE_RESOURCES, new String[]{COLUMN_RESOURCES_PREVIEW}, COLUMN_RESOURCES_UUID + " = ?", new String[]{resource.getUuid()}, null, null, null);
                try {
                    if (cursor.moveToFirst()) {

                        resource.setPreview(cursor.getBlob(0));
                    }

                } finally {
                    cursor.close();
                }
            }
        });

    }

    public static void clearMessages(final Context context, final User user) {
        final SQLiteDatabase database = OpenHelper.getInstance().getDatabase(context);

        TransactionHelper.transaction(database, new TransactionHelper.TransactionCallback() {
            @Override
            public void doInTransaction() {
                database.delete(TABLE_MESSAGES, COLUMN_MESSAGES_OWNER_UUID + " = ?", new String[]{user.getUuid()});
            }
        });
        context.getContentResolver().notifyChange(DatabaseContract.CONTENT_URI_LIKES, null);
        context.getContentResolver().notifyChange(DatabaseContract.CONTENT_URI_LIKE_LIST, null);
        context.getContentResolver().notifyChange(DatabaseContract.CONTENT_URI_TIMELINE, null);
    }

    public static void clearDatabase(final Context context) {
        final SQLiteDatabase database = OpenHelper.getInstance().getDatabase(context);

//        database.delete(TABLE_MESSAGES, null, null);
//        database.delete(TABLE_USERS, COLUMN_USERS_ROW_ID + " != ?", new String[]{Long.toString(CURRENT_USER_ROW_ID)});
//        database.delete(TABLE_RESOURCES, null, null);

        final User currentUser = getCurrentUser();
        database.delete(TABLE_MESSAGES, COLUMN_MESSAGES_OWNER_UUID + " != ?", new String[]{currentUser.getUuid()});
        database.delete(TABLE_USERS, COLUMN_USERS_ROW_ID + " != ?", new String[]{Long.toString(CURRENT_USER_ROW_ID)});
        database.delete(TABLE_RESOURCES, COLUMN_RESOURCES_OWNER_UUID + " != ?", new String[]{currentUser.getUuid()});
    }

    public static void deleteMessage(Context context, final long uuid, final String ownerUUID, final String destinationUUID) {
        final SQLiteDatabase database = OpenHelper.getInstance().getDatabase();
        TransactionHelper.transaction(database, new TransactionHelper.TransactionCallback() {
            @Override
            public void doInTransaction() {
                String selection = COLUMN_MESSAGES_UUID + " = ? AND " + COLUMN_MESSAGES_OWNER_UUID + " = ?";
                String[] selectionArgs = new String[]{Long.toString(uuid), ownerUUID};

                database.execSQL("DELETE FROM " + TABLE_RESOURCES + " WHERE " + COLUMN_RESOURCES_UUID +
                        " = (SELECT " + COLUMN_MESSAGES_RESOURCE_UUID + " FROM " + TABLE_MESSAGES + " WHERE " + selection + ")", selectionArgs);
                database.execSQL("DELETE FROM " + TABLE_LIKES + " WHERE " + COLUMN_LIKES_MESSAGE_UUID + " =?", selectionArgs);
                database.execSQL("DELETE FROM " + TABLE_COMMENTS + " WHERE " + COLUMN_COMMENTS_MESSAGE_UUID + " =?", selectionArgs);

                database.delete(TABLE_MESSAGES, selection, selectionArgs);
            }
        });
        context.getContentResolver().notifyChange(DatabaseContract.CONTENT_URI_LIKES, null);
        context.getContentResolver().notifyChange(DatabaseContract.CONTENT_URI_LIKE_LIST, null);
        context.getContentResolver().notifyChange(DatabaseContract.CONTENT_URI_TIMELINE, null);
        context.getContentResolver().notifyChange(DatabaseContract.CONTENT_URI_RESOURCES, null);
    }

    public static FileResourceStorage getFileResource(final String id) {
        final List<FileResourceStorage> list = new ArrayList<FileResourceStorage>();
        final SQLiteDatabase database = OpenHelper.getInstance().getDatabase();

        TransactionHelper.transaction(database, new TransactionHelper.TransactionCallback() {
            @Override
            public void doInTransaction() {
                final Cursor cursor = database.query(TABLE_RESOURCE_STORAGE, null, COLUMN_RESOURCE_STORAGE_ID + " = ?", new String[]{id}, null, null, null);
                try {
                    if (cursor.moveToFirst()) {
                        list.add(DatabaseBeanHelper.fromCursor(cursor, FileResourceStorage.class));
                    }

                } finally {
                    cursor.close();
                }
            }
        });

        return list.isEmpty() ? null : list.get(0);
    }

    public static void insertFileResource(final FileResourceStorage resourceStorage) {
        final SQLiteDatabase database = OpenHelper.getInstance().getDatabase();


        TransactionHelper.transaction(database, new TransactionHelper.TransactionCallback() {
            @Override
            public void doInTransaction() {
                database.insert(TABLE_RESOURCE_STORAGE, null, DatabaseBeanHelper.toContentValues(resourceStorage, FileResourceStorage.class));
            }
        });


    }

    public static void deleteFileResource(final String id) {
        final SQLiteDatabase database = OpenHelper.getInstance().getDatabase();

        TransactionHelper.transaction(database, new TransactionHelper.TransactionCallback() {
            @Override
            public void doInTransaction() {
                database.delete(TABLE_RESOURCE_STORAGE, COLUMN_RESOURCE_STORAGE_ID + " = ?", new String[]{id});
            }
        });
    }

    public static Message getMessageByResource(final String resourceUUID) {
        final List<Message> messages = new ArrayList<Message>();
        final SQLiteDatabase database = OpenHelper.getInstance().getDatabase();

        TransactionHelper.transaction(database, new TransactionHelper.TransactionCallback() {
            @Override
            public void doInTransaction() {
                final Cursor cursor = database.query(TABLE_MESSAGES, null, COLUMN_MESSAGES_RESOURCE_UUID + " = ?", new String[]{resourceUUID}, null, null, null);
                try {
                    if (cursor.moveToFirst()) {
                        messages.add(DatabaseBeanHelper.fromCursor(cursor, Message.class));
                    }

                } finally {
                    cursor.close();
                }
            }
        });

        return messages.isEmpty() ? null : messages.get(0);
    }

    public static Message getMessage(final String messageUuid) {
        final List<Message> messages = new ArrayList<Message>();
        final SQLiteDatabase database = OpenHelper.getInstance().getDatabase();

        TransactionHelper.transaction(database, new TransactionHelper.TransactionCallback() {
            @Override
            public void doInTransaction() {
                final Cursor cursor = database.query(TABLE_MESSAGES, null, COLUMN_MESSAGES_UUID + " = ?", new String[]{messageUuid}, null, null, null);
                try {
                    if (cursor.moveToFirst()) {
                        messages.add(DatabaseBeanHelper.fromCursor(cursor, Message.class));
                    }

                } finally {
                    cursor.close();
                }
            }
        });

        return messages.isEmpty() ? null : messages.get(0);
    }

    public static void insertLike(final Context context, final Like like) {
        final SQLiteDatabase database = OpenHelper.getInstance().getDatabase(context);
        TransactionHelper.transaction(database, new TransactionHelper.TransactionCallback() {
            @Override
            public void doInTransaction() {
                if (database.insert(TABLE_LIKES, null, DatabaseBeanHelper.toContentValues(like, Like.class)) != -1) {
                    Message message = getMessage(like.getMessageUuid());
                    if (message != null) {
                        setMessageLikeNumber(context, message, message.getLikeNumber() + 1);
                    }
                }
            }
        });

        context.getContentResolver().notifyChange(DatabaseContract.CONTENT_URI_TIMELINE, null);
        context.getContentResolver().notifyChange(DatabaseContract.CONTENT_URI_MESSAGES, null);
        context.getContentResolver().notifyChange(DatabaseContract.CONTENT_URI_LIKE_LIST, null);
        context.getContentResolver().notifyChange(DatabaseContract.CONTENT_URI_LIKES, null);
    }

    public static void deleteLike(final Context context, final Like like) {
        final SQLiteDatabase database = OpenHelper.getInstance().getDatabase();
        TransactionHelper.transaction(database, new TransactionHelper.TransactionCallback() {
            @Override
            public void doInTransaction() {
                String selection = COLUMN_LIKES_UUID + " = ?";
                String[] selectionArgs = new String[]{like.getUuid()};

                if (database.delete(TABLE_LIKES, selection, selectionArgs) != 0) {
                    Message message = getMessage(like.getMessageUuid());
                    if (message != null) {
                        int likeNumber = message.getLikeNumber() > 0 ? message.getLikeNumber() - 1 : 0;
                        setMessageLikeNumber(context, message, likeNumber);
                    }
                }
            }
        });
        context.getContentResolver().notifyChange(DatabaseContract.CONTENT_URI_TIMELINE, null);
        context.getContentResolver().notifyChange(DatabaseContract.CONTENT_URI_MESSAGES, null);
        context.getContentResolver().notifyChange(DatabaseContract.CONTENT_URI_LIKE_LIST, null);
        context.getContentResolver().notifyChange(DatabaseContract.CONTENT_URI_LIKES, null);
    }

    public static Like getLike(final String likeUuid) {
        final List<Like> likeList = new ArrayList<Like>();
        final SQLiteDatabase database = OpenHelper.getInstance().getDatabase();

        TransactionHelper.transaction(database, new TransactionHelper.TransactionCallback() {
            @Override
            public void doInTransaction() {
                final Cursor cursor = database.query(TABLE_LIKES, null, COLUMN_LIKES_UUID + " = ?", new String[]{likeUuid}, null, null, null);
                try {
                    if (cursor.moveToFirst()) {
                        likeList.add(DatabaseBeanHelper.fromCursor(cursor, Like.class));
                    }
                } finally {
                    cursor.close();
                }
            }
        });

        return likeList.isEmpty() ? null : likeList.get(0);
    }

    public static Like getLike(final long likeId) {
        final List<Like> likeList = new ArrayList<Like>();
        final SQLiteDatabase database = OpenHelper.getInstance().getDatabase();

        TransactionHelper.transaction(database, new TransactionHelper.TransactionCallback() {
            @Override
            public void doInTransaction() {
                final Cursor cursor = database.query(TABLE_LIKES, null, COLUMN_LIKES_ROW_ID + " = ?", new String[]{String.valueOf(likeId)}, null, null, null);
                try {
                    if (cursor.moveToFirst()) {
                        likeList.add(DatabaseBeanHelper.fromCursor(cursor, Like.class));
                    }
                } finally {
                    cursor.close();
                }
            }
        });

        return likeList.isEmpty() ? null : likeList.get(0);
    }

    public static void insertComment(final Context context, final Comment comment) {
        final SQLiteDatabase database = OpenHelper.getInstance().getDatabase(context);
        TransactionHelper.transaction(database, new TransactionHelper.TransactionCallback() {
            @Override
            public void doInTransaction() {
                long timestamp = comment.getTimestamp();
                timestamp = Math.max(timestamp, getMaxCommentTimestamp(comment.getMessageUuid()) + 1);
                comment.setTimestamp(timestamp);

                if (database.insert(TABLE_COMMENTS, null, DatabaseBeanHelper.toContentValues(comment, Comment.class)) != -1) {
                    Message message = getMessage(comment.getMessageUuid());
                    if (message != null) {
                        setMessageCommentNumber(context, message, message.getCommentNumber() + 1);
                    }
                }
            }
        });
        context.getContentResolver().notifyChange(DatabaseContract.CONTENT_URI_TIMELINE, null);
        context.getContentResolver().notifyChange(DatabaseContract.CONTENT_URI_MESSAGES, null);
        context.getContentResolver().notifyChange(DatabaseContract.CONTENT_URI_COMMENT_LIST, null);
        context.getContentResolver().notifyChange(DatabaseContract.CONTENT_URI_COMMENTS, null);
    }
}
