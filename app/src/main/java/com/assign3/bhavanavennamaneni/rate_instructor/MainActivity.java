package com.assign3.bhavanavennamaneni.rate_instructor;

import android.content.Context;
import android.content.Intent;
import android.net.ConnectivityManager;
import android.net.NetworkInfo;
import android.os.Bundle;
import android.support.v7.app.ActionBarActivity;
import android.util.Log;
import android.view.Menu;
import android.view.MenuItem;
import android.view.View;
import android.widget.AdapterView;
import android.widget.ArrayAdapter;
import android.widget.ListView;

import com.android.volley.Cache;
import com.android.volley.RequestQueue;
import com.android.volley.Response;
import com.android.volley.VolleyError;
import com.android.volley.toolbox.JsonArrayRequest;
import com.android.volley.toolbox.Volley;

import org.json.JSONArray;
import org.json.JSONException;
import org.json.JSONObject;

import java.io.UnsupportedEncodingException;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.HashMap;


public class MainActivity extends ActionBarActivity {

    ArrayList<String> i_arrayList;
    HashMap<String, Integer> i_Map;
    ListView InstructorList;
    RequestQueue queue;
    DatabaseHelper dbHelper;
    String url_list;
    private Context context = this;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_main);
        InstructorList = (ListView) findViewById(R.id.Instructor_List);

        getSupportActionBar().setDisplayShowHomeEnabled(true);
        getSupportActionBar().setLogo(R.drawable.ic_launcher);
        getSupportActionBar().setDisplayUseLogoEnabled(true);
    }

    @Override
    protected void onResume() {
        super.onResume();
        queue = Volley.newRequestQueue(context);
        i_arrayList = new ArrayList<>();
        i_Map = new HashMap<>();
        dbHelper = new DatabaseHelper(context);
        url_list = "http://bismarck.sdsu.edu/rateme/list";

        if (isNetworkConnected()) {

            Cache.Entry cachedList = queue.getCache().get(url_list);

            if (cachedList != null) {
                Log.i("info", "cached data");
                try {
                    JSONArray listArray = new JSONArray(new String(cachedList.data, "UTF8"));
                    for (int x = 0; x < listArray.length(); x++) {
                        JSONObject jsObject = (JSONObject) listArray.get(x);
                        int id = jsObject.getInt("id");
                        String name = jsObject.getString("firstName") + " " + jsObject.getString("lastName");
                        i_arrayList.add(name);
                        i_Map.put(name, id);
                    }
                    ArrayAdapter adapter = new ArrayAdapter(MainActivity.this, android.R.layout.simple_list_item_activated_1, i_arrayList);
                    InstructorList.setAdapter(adapter);
                } catch (UnsupportedEncodingException e) {
                    e.printStackTrace();
                } catch (JSONException e) {
                    e.printStackTrace();
                }
            } else {
                Log.i("info", "cached data null");
                Response.Listener<JSONArray> success = new Response.Listener<JSONArray>() {
                    public void onResponse(JSONArray response) {

                        try {
                            JSONArray listArray = new JSONArray(response.toString());
                            for (int x = 0; x < listArray.length(); x++) {
                                JSONObject jsObject = (JSONObject) listArray.get(x);
                                int id = jsObject.getInt("id");
                                String name = jsObject.getString("firstName") + " " + jsObject.getString("lastName");
                                i_arrayList.add(name);
                                i_Map.put(name, id);
                            }
                            ArrayAdapter adapter = new ArrayAdapter(MainActivity.this, android.R.layout.simple_list_item_activated_1, i_arrayList);
                            InstructorList.setAdapter(adapter);
                            dbHelper.AddListValues(listArray);
                        } catch (JSONException e) {
                            e.printStackTrace();
                        }
                    }
                };
                Response.ErrorListener failure = new Response.ErrorListener() {
                    public void onErrorResponse(VolleyError error) {
                        Log.i("info", error.toString());
                    }
                };
                JsonArrayRequest getListRequest = new JsonArrayRequest(url_list, success, failure);
                queue.add(getListRequest);
            }
        } else {
            i_Map = dbHelper.GetListValues();
            String[] keys = i_Map.keySet().toArray(new String[0]);
            i_arrayList = new ArrayList<>(Arrays.asList(keys));
            ArrayAdapter adapter = new ArrayAdapter(MainActivity.this, android.R.layout.simple_list_item_activated_1, i_arrayList);
            InstructorList.setAdapter(adapter);
        }

        InstructorList.setOnItemClickListener(new AdapterView.OnItemClickListener() {
            @Override
            public void onItemClick(AdapterView<?> parent, View view, int position, long id) {
                String key = (String) InstructorList.getItemAtPosition(position);
                int id_selected = i_Map.get(key);
                Intent go = new Intent(MainActivity.this, DetailViewActivity.class);
                go.putExtra("InstructorID", id_selected);
                startActivity(go);
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

    @Override
    protected void onDestroy() {
        super.onDestroy();
        queue.getCache().remove(url_list);
    }

    @Override
    public boolean onCreateOptionsMenu(Menu menu) {
        getMenuInflater().inflate(R.menu.menu_main, menu);
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


}
