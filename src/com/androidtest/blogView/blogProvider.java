package com.androidtest.blogView;

import android.app.SearchManager;
import android.content.ContentProvider;
import android.content.ContentUris;
import android.content.ContentValues;
import android.content.Context;
import android.content.UriMatcher;
import android.database.Cursor;
import android.database.SQLException;
import android.database.sqlite.SQLiteDatabase;
import android.database.sqlite.SQLiteQueryBuilder;
import android.database.sqlite.SQLiteDatabase.CursorFactory;
import android.database.sqlite.SQLiteOpenHelper;
import android.net.Uri;
import android.text.TextUtils;
import android.util.Log;

public class blogProvider extends ContentProvider {
	
	public static final Uri CONTENT_URI = Uri.parse("content://com.androidtest.provider.blogView/blog");
	
	// 하부 데이터베이스.
	private SQLiteDatabase blogDB;
	private static final String TAG = "BlogProvider";
	private static final String DATABASE_NAME = "blog.db";
	private static final int DATABASE_VERSION = 1;
	private static final String BLOG_TABLE = "blog";

	// 열 이름.
	public static final String KEY_ID = "_id";
	public static final String KEY_TITLE = "title";
	public static final String KEY_LINK = "link";
	public static final String KEY_DATE = "date";
	public static final String KEY_AUTHOR = "author";
	public static final String KEY_READCHECK = "readcheck";
	

	// 열 인덱스.
	public static final int TITLE_COLUMN = 1;
	public static final int LINK_COLUMN = 2;
	public static final int DATE_COLUMN = 3;
	public static final int AUTHOR_COLUMN = 4;
	public static final int READCHECK_COLUMN = 5;
	
	private static final int B_ALL = 1;
	private static final int B_ID = 2;
	
	private static final UriMatcher uriMatcher;
	
	static {
	    uriMatcher = new UriMatcher(UriMatcher.NO_MATCH);
	    uriMatcher.addURI("com.androidtest.provider.blogView", "blog", B_ALL);
	    uriMatcher.addURI("com.androidtest.provider.blogView", "blog/#", B_ID);
	 }
	
	private static class BlogDatabaseHelper extends SQLiteOpenHelper {
		
		private static final String DATABASE_CREATE = 
			"create table " + BLOG_TABLE + " ("
	        + KEY_ID + " integer primary key autoincrement, "
	        + KEY_TITLE + " TEXT, "
	        + KEY_LINK + " TEXT, "
	        + KEY_DATE + " INTEGER, "
	        + KEY_AUTHOR + " TEXT, "
	        + KEY_READCHECK + " INTEGER);";
		
		public BlogDatabaseHelper(Context context, String name,
				CursorFactory factory, int version) {
			super(context, name, factory, version);
			
		}

		@Override
		public void onCreate(SQLiteDatabase db) {
			db.execSQL(DATABASE_CREATE);
		}

		@Override
		public void onUpgrade(SQLiteDatabase db, int oldVersion, int newVersion) {
			Log.w(TAG, "Upgrading database from version " + oldVersion + " to "
	                   + newVersion + ", which will destroy all old data");

	        db.execSQL("DROP TABLE IF EXISTS " + BLOG_TABLE);
	        onCreate(db);
		}

		
	}

	@Override
	public int delete(Uri uri, String where, String[] wArgs) {
		int count;

	    switch (uriMatcher.match(uri)) {
	        case B_ALL:
	            count = blogDB.delete(BLOG_TABLE, where, wArgs);
	            break;

	        case B_ID:
	            String segment = uri.getPathSegments().get(1);
	            count = blogDB.delete(BLOG_TABLE, KEY_ID + "="
	                                                + segment
	                                                + (!TextUtils.isEmpty(where) ? " AND ("
	                                                + where + ')' : ""), wArgs);
	            break;

	        default: throw new IllegalArgumentException("Unsupported URI: " + uri);
	    }

	    getContext().getContentResolver().notifyChange(uri, null);
	    return count;
	}

	@Override
	public String getType(Uri uri) {
		switch (uriMatcher.match(uri)) {
    	case B_ALL: return "vnd.android.cursor.dir/vnd.androidtest.blogView";
        case B_ID: return "vnd.android.cursor.item/vnd.androidtest.blogView";
        default: throw new IllegalArgumentException("Unsupported URI: " + uri);
    }
	}

	@Override
	public Uri insert(Uri uri, ContentValues initialValues) {

	    long rowID = blogDB.insert(BLOG_TABLE, "blog", initialValues);

	    if (rowID > 0) {
	        Uri _uri = ContentUris.withAppendedId(CONTENT_URI, rowID);
	        getContext().getContentResolver().notifyChange(_uri, null);
	        return _uri;
	    }
	    throw new SQLException("Failed to insert row into " + uri);
	}

	@Override
	public boolean onCreate() {
		Context context = getContext();

		BlogDatabaseHelper dbHelper = new BlogDatabaseHelper(context,
	        DATABASE_NAME, null, DATABASE_VERSION);
		blogDB = dbHelper.getWritableDatabase();
	    return (blogDB == null) ? false : true;
	}

	@Override
	public Cursor query(Uri uri, String[] projection, String selection, String[] selectionArgs,
			String sort) {
		SQLiteQueryBuilder qb = new SQLiteQueryBuilder();

	    qb.setTables(BLOG_TABLE);

	    switch (uriMatcher.match(uri)) {
	        case B_ID:
	        	qb.appendWhere(KEY_ID + "=" + uri.getPathSegments().get(1));
	            break;
	        default :
	        	break;
	    }

	    String orderBy;
	    if (TextUtils.isEmpty(sort)) {
	        orderBy = KEY_DATE;
	    } else {
	        orderBy = sort;
	    }

	    Cursor c = qb.query(blogDB, projection,
                            selection, selectionArgs,
                            null, null,
                            null);

	    c.setNotificationUri(getContext().getContentResolver(), uri);

	    return c;
	}

	@Override
	public int update(Uri uri, ContentValues values, String where, String[] wArgs) {
		int count;
	    switch (uriMatcher.match(uri)) {
	        case B_ALL: count = blogDB.update(BLOG_TABLE, values, where, wArgs);
	            break;

	        case B_ID: String segment = uri.getPathSegments().get(1);
	            count = blogDB.update(BLOG_TABLE, values, KEY_ID
	                                        + "=" + segment
	                                        + (!TextUtils.isEmpty(where) ? " AND ("
	                                        + where + ')' : ""), wArgs);
	            break;

	        default: throw new IllegalArgumentException("Unknown URI " + uri);
	    }

	    getContext().getContentResolver().notifyChange(uri, null);
	    return count;
	}

}
