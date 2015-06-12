package com.assign3.bhavanavennamaneni.rate_instructor;

import android.content.ContentValues;
import android.content.Context;
import android.database.Cursor;
import android.database.sqlite.SQLiteDatabase;
import android.database.sqlite.SQLiteOpenHelper;
import android.util.Log;

import org.json.JSONArray;
import org.json.JSONException;
import org.json.JSONObject;

import java.util.HashMap;

/**
 * Created by bhavanavennamaneni on 3/15/15.
 */
public class DatabaseHelper extends SQLiteOpenHelper {

    private static final String DATABASE_NAME = "instructor.db";
    private static final int DATABASE_VERSION = 1;
    private static final String LIST_TABLE = "i_list";
    private static final String DATA_TABLE = "i_data";
    private static final String COMMENTS_TABLE = "i_comments";
    private static final String I_ID = "i_id";
    private static final String F_NAME = "f_name";
    private static final String L_NAME = "l_name";
    private static final String OFFICE = "office", PHONE = "phone", EMAIL = "email", AVERAGE = "average", TOTAL = "total";
    private static final String DATE = "date", COMMENT = "comment";

    public DatabaseHelper(Context context) {
        super(context, DATABASE_NAME, null, DATABASE_VERSION);
    }

    @Override
    public void onCreate(SQLiteDatabase db) {

        try {
            String CREATE_LIST_TABLE = "CREATE TABLE IF NOT EXISTS " + LIST_TABLE + " (" + I_ID + " INTEGER PRIMARY KEY, " + F_NAME + " VARCHAR(65)," + L_NAME + " VARCHAR(65));";
            db.execSQL(CREATE_LIST_TABLE);
            String CREATE_DATA_TABLE = "CREATE TABLE IF NOT EXISTS " + DATA_TABLE + " (" + I_ID + " INTEGER PRIMARY KEY, " + OFFICE +
                    " VARCHAR(25), " + PHONE + " VARCHAR(15)," + EMAIL + " VARCHAR(100), " + AVERAGE + " DECIMAL(2,2), " + TOTAL + " INTEGER );";
            db.execSQL(CREATE_DATA_TABLE);
            String CREATE_COMMENTS_TABLE = "CREATE TABLE IF NOT EXISTS " + COMMENTS_TABLE + " (" + I_ID + " INTEGER ," + DATE +
                    " VARCHAR(15)," + COMMENT + " TEXT);";
            db.execSQL(CREATE_COMMENTS_TABLE);
        } catch (Exception e) {
            Log.i("info", e.toString());
        }
    }


    public void AddListValues(JSONArray jsonListArray) {

        try {
            SQLiteDatabase db = this.getWritableDatabase();
            String delete_table = "DELETE FROM " + LIST_TABLE;
            db.execSQL(delete_table);
            for (int x = 0; x < jsonListArray.length(); x++) {
                JSONObject jsonObject = (JSONObject) jsonListArray.get(x);
                int id = jsonObject.getInt("id");
                String firstName = jsonObject.getString("firstName");
                String lastName = jsonObject.getString("lastName");
                ContentValues values = new ContentValues();
                values.put(I_ID, id);
                values.put(F_NAME, firstName);
                values.put(L_NAME, lastName);
                try {
                    long row = db.insert(LIST_TABLE, null, values);
                } catch (Exception e) {
                    Log.i("info", "Error in Insertion " + e);
                }
            }
            db.close();
        } catch (JSONException e) {
            Log.i("info", e.toString());
        }
    }

    public HashMap<String, Integer> GetListValues() {
        SQLiteDatabase db = this.getWritableDatabase();
        String[] columns = {I_ID, F_NAME, L_NAME};
        HashMap<String, Integer> ListMap = new HashMap<>();

        Cursor cursor = db.query(LIST_TABLE, columns, null, null, null, null, I_ID + " ASC");

        while (cursor.moveToNext()) {
            int id = cursor.getInt(cursor.getColumnIndex(I_ID));
            String firstName = cursor.getString(cursor.getColumnIndex(F_NAME));
            String lastName = cursor.getString(cursor.getColumnIndex(L_NAME));
            String name = firstName + " " + lastName;
            ListMap.put(name, id);
        }
        cursor.close();
        db.close();
        return ListMap;
    }

    public void AddInstructorData(JSONObject jsonInstructorObject, int instructor_id) {

        SQLiteDatabase db = this.getWritableDatabase();
        try {
            String delete_record = "DELETE FROM " + DATA_TABLE + " WHERE EXISTS (SELECT " + I_ID + " FROM " + DATA_TABLE + " WHERE " + I_ID + "=" + instructor_id + " );";
            db.execSQL(delete_record);
            String office = jsonInstructorObject.getString("office");
            String phone = jsonInstructorObject.getString("phone");
            String email = jsonInstructorObject.getString("email");
            JSONObject jsonRating = new JSONObject(jsonInstructorObject.getString("rating"));
            ContentValues values = new ContentValues();
            values.put(I_ID, instructor_id);
            values.put(OFFICE, office);
            values.put(PHONE, phone);
            values.put(EMAIL, email);
            values.put(AVERAGE, jsonRating.getString("average"));
            values.put(TOTAL, jsonRating.getString("totalRatings"));
            try {
                long row_num = db.insert(DATA_TABLE, null, values);
            } catch (Exception e) {
                Log.i("info", "Error in Data Insertion " + e);
            }
        } catch (JSONException e) {
            Log.i("info", e.toString());
        }
        db.close();
    }


    public JSONObject GetInstructorData(int instructor_id) {
        SQLiteDatabase db = this.getWritableDatabase();
        Cursor cursor = db.rawQuery("SELECT * FROM " + DATA_TABLE + " JOIN " + LIST_TABLE + " ON " + DATA_TABLE + "." + I_ID + "=" + LIST_TABLE + "." + I_ID + " WHERE " + DATA_TABLE + "." +
                I_ID + "=" + instructor_id + ";", null);
        JSONObject dataObject = new JSONObject();
        while (cursor.moveToNext()) {
            try {
                dataObject.put("id", cursor.getInt(cursor.getColumnIndex(I_ID)));
                dataObject.put("office", cursor.getString(cursor.getColumnIndex(OFFICE)));
                dataObject.put("phone", cursor.getString(cursor.getColumnIndex(PHONE)));
                dataObject.put("email", cursor.getString(cursor.getColumnIndex(EMAIL)));
                JSONObject ratingObject = new JSONObject();
                ratingObject.put("average", cursor.getString(cursor.getColumnIndex(AVERAGE)));
                ratingObject.put("totalRatings", cursor.getInt(cursor.getColumnIndex(TOTAL)));
                dataObject.put("rating", ratingObject);
                dataObject.put("firstName", cursor.getString(cursor.getColumnIndex(F_NAME)));
                dataObject.put("lastName", cursor.getString(cursor.getColumnIndex(L_NAME)));
            } catch (JSONException e) {
                e.printStackTrace();
                Log.i("info", "Error in Data Retrieval " + e.toString());
            }
        }
        cursor.close();
        db.close();
        return dataObject;
    }


    public JSONArray GetComments(int instructor_id) {
        JSONArray getComments = new JSONArray();

        SQLiteDatabase db = this.getWritableDatabase();
        String[] columns = {I_ID, DATE, COMMENT};
        Cursor cursor = db.query(COMMENTS_TABLE, columns, I_ID + "=" + instructor_id, null, null, null, null);

        while (cursor.moveToNext()) {
            try {
                JSONObject object = new JSONObject();
                object.put("text", cursor.getString(cursor.getColumnIndex(COMMENT)));
                object.put("date", cursor.getString(cursor.getColumnIndex(DATE)));
                getComments.put(object);
            } catch (JSONException e) {
                e.printStackTrace();
                Log.i("info", "Error in Comments Retrieval " + e.toString());
            }
        }
        cursor.close();
        db.close();
        return getComments;
    }


    @Override
    public void onUpgrade(SQLiteDatabase db, int oldVersion, int newVersion) {

    }

}
