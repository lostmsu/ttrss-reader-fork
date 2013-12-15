/*
 * ttrss-reader-fork for Android
 * 
 * Copyright (C) 2010 N. Braden.
 * 
 * This program is free software; you can redistribute it and/or
 * modify it under the terms of the GNU General Public License
 * version 3 as published by the Free Software Foundation.
 * 
 * This program is distributed in the hope that it will be useful,
 * but WITHOUT ANY WARRANTY; without even the implied warranty of
 * MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE. See the
 * GNU General Public License for more details.
 */

package org.ttrssreader.model;

import org.ttrssreader.controllers.Controller;
import org.ttrssreader.controllers.DBHelper;
import android.content.Context;
import android.database.Cursor;
import android.database.sqlite.SQLiteDatabase;
import android.database.sqlite.SQLiteOpenHelper;
import android.database.sqlite.SQLiteStatement;

public class CategoryCursorHelper extends MainCursorHelper {
    
    public CategoryCursorHelper(Context context) {
        super(context);
    }
    
    @Override
    public Cursor createCursor(boolean overrideDisplayUnread, boolean buildSafeQuery) {
        
        boolean displayUnread = Controller.getInstance().onlyUnread();
        boolean invertSortFeedCats = Controller.getInstance().invertSortFeedscats();
        
        if (overrideDisplayUnread)
            displayUnread = false;
        
        if (db != null)
            db.close();
        
        OpenHelper openHelper = new OpenHelper(context);
        db = openHelper.getWritableDatabase();
        insert = db.compileStatement(INSERT);
        
        StringBuilder query;
        // Virtual Feeds
        if (Controller.getInstance().showVirtual()) {
            query = new StringBuilder();
            query.append("SELECT id,title,unread FROM ");
            query.append(DBHelper.TABLE_CATEGORIES);
            query.append(" WHERE id>=-4 AND id<0 ORDER BY id");
            insertValues(query.toString());
        }
        
        // Labels
        query = new StringBuilder();
        query.append("SELECT id,title,unread FROM ");
        query.append(DBHelper.TABLE_FEEDS);
        query.append(" WHERE id<-10");
        query.append(displayUnread ? " AND unread>0" : "");
        query.append(" ORDER BY UPPER(title) ASC");
        query.append(" LIMIT 500 ");
        insertValues(query.toString());
        
        // "Uncategorized Feeds"
        query = new StringBuilder();
        query.append("SELECT id,title,unread FROM ");
        query.append(DBHelper.TABLE_CATEGORIES);
        query.append(" WHERE id=0");
        insertValues(query.toString());
        
        // Categories
        query = new StringBuilder();
        query.append("SELECT id,title,unread FROM ");
        query.append(DBHelper.TABLE_CATEGORIES);
        query.append(" WHERE id>0");
        query.append(displayUnread ? " AND unread>0" : "");
        query.append(" ORDER BY UPPER(title) ");
        query.append(invertSortFeedCats ? "DESC" : "ASC");
        query.append(" LIMIT 500 ");
        insertValues(query.toString());
        
        String[] columns = { "id", "title", "unread" };
        return db.query(TABLE_NAME, columns, null, null, null, null, null, "600");
    }
    
    /*
     * This is quite a hack. Since partial-sorting of sql-results is not possible I wasn't able to sort virtual
     * categories by id, Labels by title, insert uncategorized feeds there and sort categories by title again.
     * No I insert these results one by one in a memory-table in the right order, add an auto-increment-column
     * ("sortId INTEGER PRIMARY KEY") and afterwards select everything from this memory-table sorted by sortId.
     * Works fine!
     */
    private static final String TABLE_NAME = "categories_memory_db";
    private static final String INSERT = "REPLACE INTO " + TABLE_NAME
            + "(id, title, unread, sortId) VALUES (?, ?, ?, null)";
    private SQLiteDatabase db;
    private SQLiteStatement insert;
    
    private static class OpenHelper extends SQLiteOpenHelper {
        OpenHelper(Context context) {
            super(context, null, null, 1);
        }
        
        /**
         * @see android.database.sqlite.SQLiteOpenHelper#onCreate(android.database.sqlite.SQLiteDatabase)
         */
        @Override
        public void onCreate(SQLiteDatabase db) {
            db.execSQL("CREATE TABLE " + TABLE_NAME
                    + " (id INTEGER, title TEXT, unread INTEGER, sortId INTEGER PRIMARY KEY)");
        }
        
        @Override
        public void onUpgrade(SQLiteDatabase db, int oldVersion, int newVersion) {
        }
    }
    
    private void insertValues(String query) {
        Cursor c = null;
        try {
            c = DBHelper.getInstance().query(query.toString(), null);
            if (c == null)
                return;
            if (c.isBeforeFirst() && !c.moveToFirst())
                return;
            
            while (true) {
                insert.bindLong(1, c.getInt(0)); // id
                insert.bindString(2, c.getString(1)); // title
                insert.bindLong(3, c.getInt(2)); // unread
                insert.executeInsert();
                if (!c.moveToNext())
                    break;
            }
        } finally {
            if (c != null && !c.isClosed())
                c.close();
        }
    }
}
