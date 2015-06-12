package com.assign3.bhavanavennamaneni.rate_instructor;

import android.app.AlertDialog;
import android.content.ContentValues;
import android.content.Context;
import android.content.DialogInterface;
import android.database.sqlite.SQLiteDatabase;
import android.net.ConnectivityManager;
import android.net.NetworkInfo;
import android.net.http.AndroidHttpClient;
import android.os.Bundle;
import android.os.Handler;
import android.os.Looper;
import android.os.Message;
import android.support.v7.app.ActionBarActivity;
import android.util.Log;
import android.view.LayoutInflater;
import android.view.Menu;
import android.view.MenuItem;
import android.view.View;
import android.view.WindowManager;
import android.widget.Button;
import android.widget.EditText;
import android.widget.RatingBar;
import android.widget.TextView;
import android.widget.Toast;

import com.android.volley.Cache;
import com.android.volley.Request;
import com.android.volley.RequestQueue;
import com.android.volley.Response;
import com.android.volley.VolleyError;
import com.android.volley.toolbox.JsonArrayRequest;
import com.android.volley.toolbox.JsonObjectRequest;
import com.android.volley.toolbox.Volley;

import org.apache.http.HttpResponse;
import org.apache.http.client.methods.HttpPost;
import org.apache.http.entity.StringEntity;
import org.apache.http.protocol.HTTP;
import org.json.JSONArray;
import org.json.JSONException;
import org.json.JSONObject;

import java.io.UnsupportedEncodingException;


public class DetailViewActivity extends ActionBarActivity {

    private int id_passed;
    private String url_data, url_comments;
    private final Context context = this;
    AndroidHttpClient httpClient;
    Handler mainHandler;
    TextView view_comments;
    RequestQueue queue;
    Cache.Entry cachedData, cachedComments;
    PostThread postThread;
    DatabaseHelper dbHelper;
    View dialog_layout;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_detail_view);
        getSupportActionBar().setDisplayShowHomeEnabled(true);
        getSupportActionBar().setLogo(R.drawable.ic_launcher);
        getSupportActionBar().setDisplayUseLogoEnabled(true);
    }

    @Override
    protected void onResume() {
        super.onResume();
        queue = Volley.newRequestQueue(this);
        httpClient = AndroidHttpClient.newInstance(null);
        dbHelper = new DatabaseHelper(context);
        postThread = new PostThread();
        postThread.start();

        id_passed = getIntent().getIntExtra("InstructorID", 0);

        if (id_passed != 0) {
            url_data = "http://bismarck.sdsu.edu/rateme/instructor/" + id_passed;
            url_comments = "http://bismarck.sdsu.edu/rateme/comments/" + id_passed;
        }

        if (isNetworkConnected()) {
            cachedData = queue.getCache().get(url_data);
            if (cachedData != null) {

                try {
                    JSONObject data_Again = new JSONObject(new String(cachedData.data, "UTF8"));
                    displayDetails(data_Again);
                } catch (UnsupportedEncodingException e) {
                    e.printStackTrace();
                } catch (JSONException e) {
                    e.printStackTrace();
                }
            } else {

                JsonObjectRequest jsonObjectRequest = new JsonObjectRequest(Request.Method.GET, url_data, null, new Response.Listener<JSONObject>() {
                    @Override
                    public void onResponse(JSONObject response) {

                        try {
                            final JSONObject jsonObject = new JSONObject(response.toString());
                            displayDetails(jsonObject);
                            dbHelper.AddInstructorData(jsonObject, id_passed);
                        } catch (JSONException e) {
                            e.printStackTrace();
                        }

                    }
                }, new Response.ErrorListener() {

                    @Override
                    public void onErrorResponse(VolleyError error) {
                        // TODO Auto-generated method stub
                        Log.d("info", "data" + error.toString());
                    }
                });
                queue.add(jsonObjectRequest);
            }

            cachedComments = queue.getCache().get(url_comments);
            if (cachedComments != null) {

                try {
                    JSONArray commentsArray = new JSONArray(new String(cachedComments.data, "UTF8"));
                    displayComments(commentsArray);
                } catch (UnsupportedEncodingException e) {
                    e.printStackTrace();
                } catch (JSONException e) {
                    e.printStackTrace();
                }

            } else {
                JsonArrayRequest jsonCommentsRequest = new JsonArrayRequest
                        (url_comments, new Response.Listener<JSONArray>() {

                            @Override
                            public void onResponse(JSONArray response) {
                                final JSONArray c_array = response;
                                displayComments(response);
                                AddComments(response, id_passed);//writes comments to database, invoked on a worker thread
                            }
                        }, new Response.ErrorListener() {

                            @Override
                            public void onErrorResponse(VolleyError error) {
                                // TODO Auto-generated method stub
                                Log.d("info", "comments" + error.toString());
                            }
                        });
                queue.add(jsonCommentsRequest);

            }
        } else {
            Log.i("info", "No network Connection");
            JSONObject getDataObject = dbHelper.GetInstructorData(id_passed);
            if (getDataObject.length() != 0) {
                displayDetails(getDataObject);
                JSONArray getCommentsArray = dbHelper.GetComments(id_passed);
                if (getCommentsArray.length() != 0) {
                    displayComments(getCommentsArray);
                }
            } else {
                Toast.makeText(DetailViewActivity.this, "No Database Record, Check For Internet Connection", Toast.LENGTH_SHORT).show();
            }
        }

        mainHandler = new Handler() {
            @Override
            public void handleMessage(Message msg) {
                if (msg.what == 1) {
                    queue.getCache().remove(url_data);
                    httpClient.close();
                    onResume();
                }
                if (msg.what == 2) {
                    queue.getCache().remove(url_comments);
                    httpClient.close();
                    onResume();
                }
            }
        };


        Button button_rate = (Button) findViewById(R.id.button_rate);
        button_rate.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {

                LayoutInflater inflater = getLayoutInflater();
                dialog_layout = inflater.inflate(R.layout.rating_dialog_layout, null);
                AlertDialog.Builder builder = new AlertDialog.Builder(context);
                builder.setTitle("Rate Professor")
                        .setView(dialog_layout)
                        .setNegativeButton("Cancel", new DialogInterface.OnClickListener() {
                            @Override
                            public void onClick(DialogInterface dialog, int which) {

                            }
                        })
                        .setPositiveButton("Add", new DialogInterface.OnClickListener() {
                            @Override
                            public void onClick(DialogInterface dialog, int which) {
                                if (isNetworkConnected()) {
                                    RatingBar instructorRatingBar = (RatingBar) dialog_layout.findViewById(R.id.ratingBar);
                                    int rating = (int) instructorRatingBar.getRating();
                                    Message send_rating = postThread.postThreadHandler.obtainMessage(1, id_passed, rating);
                                    postThread.postThreadHandler.sendMessage(send_rating);
                                } else {
                                    Toast.makeText(DetailViewActivity.this, "Check Network Connection", Toast.LENGTH_SHORT).show();
                                }
                            }
                        });

                AlertDialog rate_dialog = builder.create();
                rate_dialog.show();
            }
        });

        Button button_comment = (Button) findViewById(R.id.button_comment);
        button_comment.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                final EditText input = new EditText(context);
                AlertDialog.Builder builder = new AlertDialog.Builder(context);
                builder.setTitle("Write Comment")
                        .setView(input)
                        .setNegativeButton("Cancel", new DialogInterface.OnClickListener() {
                            @Override
                            public void onClick(DialogInterface dialog, int which) {
                            }
                        })
                        .setPositiveButton("Add", new DialogInterface.OnClickListener() {
                            @Override
                            public void onClick(DialogInterface dialog, int which) {
                                if (isNetworkConnected()) {
                                    Message send_comment = new Message();
                                    Bundle bundle = new Bundle();
                                    bundle.putString("comment", input.getText().toString());
                                    send_comment.setData(bundle);
                                    send_comment.what = 2;
                                    send_comment.arg1 = id_passed;
                                    postThread.postThreadHandler.sendMessage(send_comment);
                                } else {
                                    Toast.makeText(DetailViewActivity.this, "Check Network Connection", Toast.LENGTH_SHORT).show();
                                }
                            }
                        });
                AlertDialog comment_dialog = builder.create();
                comment_dialog.show();
                comment_dialog.getWindow().setSoftInputMode(WindowManager.LayoutParams.SOFT_INPUT_STATE_ALWAYS_VISIBLE);
            }
        });
    }


    private boolean isNetworkConnected() {
        ConnectivityManager manager = (ConnectivityManager) getSystemService(Context.CONNECTIVITY_SERVICE);
        NetworkInfo info = manager.getActiveNetworkInfo();
        if (info == null)
            return false;
        else
            return true;
    }


    public void displayDetails(JSONObject jsonDataObject) {
        try {
            String office = jsonDataObject.getString("office");
            String name = jsonDataObject.getString("firstName") + " " + jsonDataObject.getString("lastName");
            String phone = jsonDataObject.getString("phone");
            String email = jsonDataObject.getString("email");
            JSONObject jsonRating = new JSONObject(jsonDataObject.getString("rating"));

            TextView t_Name = (TextView) findViewById(R.id.tV_name);
            t_Name.setText(name);
            TextView t_office = (TextView) findViewById(R.id.tV_office);
            t_office.setText(office);
            TextView t_phone = (TextView) findViewById(R.id.tV_phone);
            t_phone.setText(phone);
            TextView t_email = (TextView) findViewById(R.id.tV_email);
            t_email.setText(email);
            TextView t_avg = (TextView) findViewById(R.id.tV_average);
            t_avg.setText("Average:");
            t_avg.append(" " + jsonRating.getString("average"));
            TextView t_rate = (TextView) findViewById(R.id.tV_total);
            t_rate.setText("Total Ratings:");
            t_rate.append(" " + jsonRating.getString("totalRatings"));
        } catch (JSONException e) {
            e.printStackTrace();
        }
    }

    public void displayComments(JSONArray jsonCommentsArray) {
        try {
            view_comments = (TextView) findViewById(R.id.tV_comments);
            view_comments.setText("");
            for (int x = 0; x < jsonCommentsArray.length(); x++) {
                JSONObject comment = (JSONObject) jsonCommentsArray.get(x);
                String text = comment.getString("text");
                String Date = comment.getString("date");
                view_comments.append(Date + " \t" + text + "\n");
            }
        } catch (JSONException e) {
            e.printStackTrace();
        }
    }

    @Override
    protected void onPause() {
        super.onPause();
        httpClient.close();
    }

    @Override
    protected void onDestroy() {
        super.onDestroy();
        queue.getCache().remove(url_data);
        queue.getCache().remove(url_comments);
    }

    @Override
    public boolean onCreateOptionsMenu(Menu menu) {
        getMenuInflater().inflate(R.menu.menu_detail_view, menu);
        return true;
    }

    @Override
    public boolean onOptionsItemSelected(MenuItem item) {

        int id = item.getItemId();

        if (id == R.id.action_settings) {
            return true;
        }

        return super.onOptionsItemSelected(item);
    }

    class PostThread extends Thread {
        Handler postThreadHandler;

        public PostThread() {

        }

        @Override
        public void run() {
            Looper.prepare();

            postThreadHandler = new Handler() {
                @Override
                public void handleMessage(Message msg) {
                    super.handleMessage(msg);
                    // 1 - handles post request of comment
                    if (msg.what == 1) {
                        String url = "http://bismarck.sdsu.edu/rateme/rating/" + msg.arg1 + "/" + msg.arg2;
                        try {
                            HttpPost postRequest = new HttpPost(url);
                            httpClient.execute(postRequest);
                            mainHandler.sendEmptyMessage(1);
                        } catch (Throwable t) {
                            Log.i("info", t.toString());
                        }
                    }
                    // 2 - handles post request of rating
                    if (msg.what == 2) {
                        String url = "http://bismarck.sdsu.edu/rateme/comment/" + msg.arg1;
                        HttpPost postCommentMethod = new HttpPost(url);
                        StringEntity comment;
                        try {
                            comment = new StringEntity(msg.getData().getString("comment"), HTTP.UTF_8);
                        } catch (UnsupportedEncodingException e) {
                            Log.d("info", e.toString());
                            return;
                        }
                        postCommentMethod.setHeader("Content-Type", "application/json;charset=UTF-8");
                        postCommentMethod.setEntity(comment);
                        try {
                            HttpResponse response = httpClient.execute(postCommentMethod);
                            Log.d("info", response.toString());
                            mainHandler.sendEmptyMessage(2);
                        } catch (Throwable t) {
                            Log.d("info", t.toString());
                        }
                    }
                }
            };
            Looper.loop();
        }
    }


    public void AddComments(JSONArray commentsArray, int instructor_id) {
        final JSONArray CommentsArray = commentsArray;
        final int id = instructor_id;

        Thread addCommentsThread = new Thread() {
            final String COMMENTS_TABLE = "i_comments";
            String I_ID = "i_id"
                    ,
                    DATE = "date"
                    ,
                    COMMENT = "comment";

            @Override
            public void run() {
                SQLiteDatabase db = dbHelper.getWritableDatabase();
                try {
                    String delete_comments = "DELETE FROM " + COMMENTS_TABLE + " WHERE EXISTS (SELECT " + I_ID +
                            " FROM " + COMMENTS_TABLE + " WHERE " + I_ID + "=" + id + " );";
                    db.execSQL(delete_comments); //deletes the existing comments of the instructor
                    for (int x = 0; x < CommentsArray.length(); x++) {
                        JSONObject jsonObject = (JSONObject) CommentsArray.get(x);
                        String date = jsonObject.getString("date");
                        String comment = jsonObject.getString("text");
                        ContentValues values = new ContentValues();
                        values.put(I_ID, id);
                        values.put(DATE, date);
                        values.put(COMMENT, comment);
                        try {
                            long row_num = db.insert(COMMENTS_TABLE, null, values);
                        } catch (Exception e) {
                            Log.i("info", "Error in Insertion " + e);
                        }
                    }

                } catch (JSONException e) {
                    Log.i("info", e.toString());
                }
                db.close();
            }
        };
        addCommentsThread.start();
    }

}
