package org.thoughtcrime.securesms.database;

import android.content.ContentValues;
import android.content.Context;
import android.database.Cursor;
import android.util.Pair;

import net.sqlcipher.database.SQLiteDatabase;

import org.thoughtcrime.securesms.database.helpers.SQLCipherOpenHelper;

import java.util.LinkedList;
import java.util.List;

public class JobQueueDatabase extends Database {

  @SuppressWarnings("unused")
  private static final String TAG = JobQueueDatabase.class.getSimpleName();

  public  static final String TABLE_NAME   = "queue";
  private static final String ID           = "_id";
  private static final String ITEM         = "item";

  public  static final String CREATE_TABLE = "CREATE TABLE " + TABLE_NAME +
      "(" + ID + " INTEGER PRIMARY KEY, " + ITEM + " TEXT NOT NULL);";

  JobQueueDatabase(Context context, SQLCipherOpenHelper databaseHelper) {
    super(context, databaseHelper);
  }

  public long store(String item) {
    SQLiteDatabase database = databaseHelper.getWritableDatabase();

    ContentValues contentValues = new ContentValues();
    contentValues.put(ITEM, item);

    return database.insert(TABLE_NAME, null, contentValues);
  }

  public List<Pair<Long, String>> getJobs() {
    List<Pair<Long, String>> results = new LinkedList<>();

    Cursor cursor = databaseHelper.getReadableDatabase()
        .query(TABLE_NAME, null, null, null, null, null, ID + " ASC", null);

    try {
      while (cursor.moveToNext()) {
        long id = cursor.getLong(cursor.getColumnIndexOrThrow(ID));
        String item = cursor.getString(cursor.getColumnIndexOrThrow(ITEM));

        results.add(new Pair<>(id, item));
      }
      return results;
    } finally {
      if (cursor != null)
        cursor.close();
    }
  }

  public void remove(long id) {
    databaseHelper.getWritableDatabase()
        .delete(TABLE_NAME, ID + " = ?", new String[] {String.valueOf(id)});
  }

}
