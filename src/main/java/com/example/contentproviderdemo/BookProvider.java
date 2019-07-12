package com.example.contentproviderdemo;

import android.content.ContentProvider;
import android.content.ContentValues;
import android.content.UriMatcher;
import android.database.Cursor;
import android.database.sqlite.SQLiteDatabase;
import android.net.Uri;
import android.support.annotation.NonNull;
import android.support.annotation.Nullable;
import android.text.TextUtils;

/**
 * Created by haif on 2019/7/12.
 */

public class BookProvider extends ContentProvider {

    public static final String AUTHORITY = "com.example.contentproviderdemo.provider";  // 对应清单文件中给provider设置的authorities

    private static final UriMatcher uriMatch = new UriMatcher(UriMatcher.NO_MATCH);
    private static final int BOOK_URI_CODE = 0;
    private static final int USER_URI_CODE = 1;

    static {
        // 为book表和user表指定Uri，分别为content://com.example.contentproviderdemo/book和content://com.example.contentproviderdemo/user
        // 这两个Uri关联的Uri_Code分别是0和1
        uriMatch.addURI(AUTHORITY, "book", BOOK_URI_CODE);
        uriMatch.addURI(AUTHORITY, "user", USER_URI_CODE);
    }

    private SQLiteDatabase mDb;

    // 根据外界传入的Uri，取出Uri_Code，再根据Uri_Code得到要查询的是哪个数据表
    private String getTableName(Uri uri) {
        String tableName = null;
        switch (uriMatch.match(uri)) {
            case BOOK_URI_CODE:
                tableName = DbOpenHelper.BOOK_TABLE_NAME;
                break;
            case USER_URI_CODE:
                tableName = DbOpenHelper.USER_TABLE_NAME;
                break;
        }
        return tableName;
    }

    // ContentProvider的创建，通常做一些初始化操作。运行在主线程，其它5个方法都运行在Binder线程池中
    @Override
    public boolean onCreate() {
        // 初始化，创建数据库，插入些数据（数据库操作，不能放在主线程中）
        new Thread(new Runnable() {
            @Override
            public void run() {
                mDb = new DbOpenHelper(getContext()).getWritableDatabase();

                mDb.execSQL("delete from " + DbOpenHelper.BOOK_TABLE_NAME);
                mDb.execSQL("delete from " + DbOpenHelper.USER_TABLE_NAME);

                mDb.execSQL("insert into " + DbOpenHelper.BOOK_TABLE_NAME + " values(3,'Android');");
                mDb.execSQL("insert into " + DbOpenHelper.BOOK_TABLE_NAME + " values(4,'Ios');");
                mDb.execSQL("insert into " + DbOpenHelper.USER_TABLE_NAME + " values(1,'Tom','male');");
                mDb.execSQL("insert into " + DbOpenHelper.USER_TABLE_NAME + " values(2,'Jake','female');");
            }
        }).start();
        return true;
    }

    // 返回一个Uri请求所对应的MIME类型（媒体类型），比如图片、视频等。如果我们的应用不关注这个选项，可以直接在这个方法中返回null或者"*/*"
    @Nullable
    @Override
    public String getType(@NonNull Uri uri) {
        return null;
    }

    // 增删改查以及getType运行在Binder线程池
    // 增删改查，存在多线程并发访问的问题。这里由于采用的是SQLite并且只有一个SQLiteDatabase的连接，所以可以正确应对多线程的问题
    // 具体原因是SQLiteDatabase内部对数据库的操作是有同步处理的，但是如果通过多个SQLiteDatabase对象来操作数据库就无法保证线程同步，因为SQLiteDatabase对象之间无法进行线程同步
    @Nullable
    @Override
    public Cursor query(@NonNull Uri uri, @Nullable String[] projection, @Nullable String selection, @Nullable String[] selectionArgs, @Nullable String sortOrder) {
        // 获取查询的是哪个表
        String tableName = getTableName(uri);
        if (TextUtils.isEmpty(tableName)) {
            return null;
        }
        return mDb.query(tableName, projection, selection, selectionArgs, null, null, sortOrder, null);
    }

    @Nullable
    @Override
    public Uri insert(@NonNull Uri uri, @Nullable ContentValues values) {
        // 获取查询的是哪个表
        String tableName = getTableName(uri);
        if (TextUtils.isEmpty(tableName)) {
            return null;
        }
        mDb.insert(tableName, null, values);
        getContext().getContentResolver().notifyChange(uri, null);  // 在ContentProvider发生数据变化时调用getContentResolver().notifyChange(uri, null)来通知注册在此URI上的访问者。
        return uri;
    }

    @Override
    public int delete(@NonNull Uri uri, @Nullable String selection, @Nullable String[] selectionArgs) {
        // 获取查询的是哪个表
        String tableName = getTableName(uri);
        if (TextUtils.isEmpty(tableName)) {
            return 0;
        }
        int count = mDb.delete(tableName, selection, selectionArgs);
        if (count > 0) {
            getContext().getContentResolver().notifyChange(uri, null);
        }
        return count;
    }

    @Override
    public int update(@NonNull Uri uri, @Nullable ContentValues values, @Nullable String selection, @Nullable String[] selectionArgs) {
        // 获取查询的是哪个表
        String tableName = getTableName(uri);
        if (TextUtils.isEmpty(tableName)) {
            return 0;
        }
        int count = mDb.update(tableName, values, selection, selectionArgs);
        if (count > 0) {
            getContext().getContentResolver().notifyChange(uri, null);
        }
        return count;
    }
}
