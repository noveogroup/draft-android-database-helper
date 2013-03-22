package com.noveogroup.android.database.provider;

import android.net.Uri;

public interface DatabaseContract {
    public static final String DATABASE_NAME = "blalblabla";
    public static final int DATABASE_VERSION = 1;

    public static final String AUTHORITY = "com.noveogroup.provider";

    public static final String BASE_PATH_USERS = "users";
    public static final String BASE_PATH_TIMELINE = "timeline";
    public static final String BASE_PATH_MESSAGES = "messages";
    public static final String BASE_PATH_RESOURCES = "resources";
    public static final String BASE_PATH_PROFILE = "profile";
    public static final String BASE_PATH_LIKES = "likes";
    public static final String BASE_PATH_COMMENTS = "comments";
    public static final String BASE_PATH_LIKE_LIST = "like_list";
    public static final String BASE_PATH_COMMENT_LIST = "comment_list";

    public static final Uri CONTENT_URI_USERS = Uri.parse("content://" + AUTHORITY + "/" + BASE_PATH_USERS);
    public static final Uri CONTENT_URI_TIMELINE = Uri.parse("content://" + AUTHORITY + "/" + BASE_PATH_TIMELINE);
    public static final Uri CONTENT_URI_MESSAGES = Uri.parse("content://" + AUTHORITY + "/" + BASE_PATH_MESSAGES);
    public static final Uri CONTENT_URI_RESOURCES = Uri.parse("content://" + AUTHORITY + "/" + BASE_PATH_RESOURCES);
    public static final Uri CONTENT_URI_PROFILE = Uri.parse("content://" + AUTHORITY + "/" + BASE_PATH_PROFILE);
    public static final Uri CONTENT_URI_LIKES = Uri.parse("content://" + AUTHORITY + "/" + BASE_PATH_LIKES);
    public static final Uri CONTENT_URI_COMMENTS = Uri.parse("content://" + AUTHORITY + "/" + BASE_PATH_COMMENTS);
    public static final Uri CONTENT_URI_LIKE_LIST = Uri.parse("content://" + AUTHORITY + "/" + BASE_PATH_LIKE_LIST);
    public static final Uri CONTENT_URI_COMMENT_LIST = Uri.parse("content://" + AUTHORITY + "/" + BASE_PATH_COMMENT_LIST);


    public static final String COLUMN_COMMON_ID = "_id";

    public static final long CURRENT_USER_ROW_ID = 1;

    public static final String TABLE_USERS = "users";
    public static final String COLUMN_USERS_ROW_ID = "users_id";
    public static final String COLUMN_USERS_UUID = "user_uuid";
    public static final String COLUMN_USERS_USERNAME = "user_username";
    public static final String COLUMN_USERS_SIGN_IN_TIMESTAMP = "user_sign_in_timestamp";
    public static final String COLUMN_USERS_LOCAL_TIMESTAMP = "user_local_timestamp";
    public static final String COLUMN_USERS_DESCRIPTION = "user_description";
    public static final String COLUMN_USERS_GOOGLE_PROFILE = "user_google_profile";
    public static final String COLUMN_USERS_FACEBOOK_PROFILE = "user_facebook_profile";
    public static final String COLUMN_USERS_TWITTER_PROFILE = "user_twitter_profile";
    public static final String COLUMN_USERS_PHOTO = "user_photo";
    public static final String COLUMN_USERS_HOSTNAME = "user_hostname";
    public static final String COLUMN_USERS_PORT = "user_port";
    public static final String COLUMN_USERS_HIDDEN_FLAG = "user_hidden_flag";
    public static final String COLUMN_USERS_AVAILABLE_FLAG = "user_available_flag";
    public static final String COLUMN_USERS_HAS_VCARD_FLAG = "user_has_vcard_flag";


    public static final String TABLE_MESSAGES = "messages";
    public static final String COLUMN_MESSAGES_ROW_ID = "message_id";
    public static final String COLUMN_MESSAGES_UUID = "message_uuid";
    public static final String COLUMN_MESSAGES_TEXT = "message_text";
    public static final String COLUMN_MESSAGES_TIMESTAMP = "message_timestamp";
    public static final String COLUMN_MESSAGES_OWNER_UUID = "message_owner_uuid";
    public static final String COLUMN_MESSAGES_DESTINATION_UUID = "message_destination_uuid";
    public static final String COLUMN_MESSAGES_SEND_FLAG = "message_send_flag";
    public static final String COLUMN_MESSAGES_RESOURCE_UUID = "message_resource_uuid";
    public static final String COLUMN_MESSAGES_CLOUD_NAME = "message_resource_cloud_name";
    public static final String COLUMN_MESSAGES_LIKE_NUMBER = "message_like_number";
    public static final String COLUMN_MESSAGES_COMMENT_NUMBER = "message_comment_number";

    public static final String TABLE_RESOURCES = "resources";
    public static final String COLUMN_RESOURCES_ROW_ID = "resources_id";
    public static final String COLUMN_RESOURCES_UUID = "resource_uuid";
    public static final String COLUMN_RESOURCES_TYPE = "resource_type";
    public static final String COLUMN_RESOURCES_OWNER_UUID = "resource_owner_uuid";
    public static final String COLUMN_RESOURCES_HAS_PREVIEW = "resource_has_preview";
    public static final String COLUMN_RESOURCES_PREVIEW = "resource_preview";

    public static final String TABLE_RESOURCE_STORAGE = "resource_storage";
    public static final String COLUMN_RESOURCE_STORAGE_ID = "resource_storage_id";
    public static final String COLUMN_RESOURCE_STORAGE_FILENAME = "resource_storage_filename";

    public static final String TABLE_LIKES = "likes";
    public static final String COLUMN_LIKES_ROW_ID = "like_id";
    public static final String COLUMN_LIKES_UUID = "like_uuid";
    public static final String COLUMN_LIKES_MESSAGE_UUID = "like_message_uuid";
    public static final String COLUMN_LIKES_OWNER_UUID = "like_owner_uuid";
    public static final String COLUMN_LIKES_TIMESTAMP = "like_timestamp";

    public static final String TABLE_COMMENTS = "comments";
    public static final String COLUMN_COMMENTS_ROW_ID = "comment_id";
    public static final String COLUMN_COMMENTS_UUID = "comment_uuid";
    public static final String COLUMN_COMMENTS_TEXT = "comment_text";
    public static final String COLUMN_COMMENTS_TIMESTAMP = "comment_timestamp";
    public static final String COLUMN_COMMENTS_OWNER_UUID = "comment_owner_uuid";
    public static final String COLUMN_COMMENTS_MESSAGE_UUID = "comment_message_uuid";

}
