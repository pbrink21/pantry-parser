package com.example.pantry_parser.RecyclerView;

import android.content.Intent;
import android.os.Bundle;
import android.view.View;
import android.widget.SearchView;
import android.widget.TextView;
import android.widget.Toast;

import androidx.annotation.NonNull;
import androidx.appcompat.app.AppCompatActivity;
import androidx.recyclerview.widget.LinearLayoutManager;
import androidx.recyclerview.widget.RecyclerView;

import com.android.volley.Request;
import com.android.volley.RequestQueue;
import com.android.volley.Response;
import com.android.volley.VolleyError;
import com.android.volley.toolbox.JsonObjectRequest;
import com.android.volley.toolbox.Volley;
import com.example.pantry_parser.R;
import com.example.pantry_parser.Models.Recipe;
import com.example.pantry_parser.Utilities.URLs;
import com.google.android.material.floatingactionbutton.FloatingActionButton;

import org.json.JSONArray;
import org.json.JSONException;
import org.json.JSONObject;

import java.util.ArrayList;

public class IngredientListView extends AppCompatActivity implements RecyclerViewAdapter.OnRecipeListener {
    RecyclerView recyclerView;
    RecyclerViewAdapter recyclerViewAdapter;
    ArrayList<Recipe> dataset = new ArrayList<>();
    public ArrayList<String> selected = new ArrayList<>();
    boolean isLoading = false;
    String URL_INGREDIENTS = "http://coms-309-032.cs.iastate.edu:8080/ingredients";
    String URL_USERS = "http://coms-309-032.cs.iastate.edu:8080/users";
    String URL_TO_USE;
    private RequestQueue queue;
    FloatingActionButton newRecipe;
    SearchView searchView;
    TextView textView;
    int pageNo;
    String viewType;

    /**
     *Create listview activity and instantiate elements
     * @param savedInstanceState
     */
    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_ingredient_list_view);
        String viewType = (String) getIntent().getSerializableExtra("SwitchView");
        this.viewType = viewType;
        switch(viewType) {
            case "ADMIN":
                URL_TO_USE = URL_USERS;
                break;
            case "INGREDIENTS":
                URL_TO_USE = URL_INGREDIENTS;
                break;
        }
        try {
            initializeElements(viewType);
        } catch (JSONException e) {
            e.printStackTrace();
        }
        setupRecycler();
        setupAdapter();
        popData();

    }

    /**
     * Initial setup of infinite recycler for recycler view
     */
    private void setupRecycler() {
        recyclerView = findViewById(R.id.recyclerView);
        LinearLayoutManager layoutManager = new LinearLayoutManager(this, LinearLayoutManager.VERTICAL, false);
        layoutManager.scrollToPosition(0);
        recyclerView.setLayoutManager(layoutManager);
        recyclerView.addOnScrollListener(new RecyclerView.OnScrollListener() {
            @Override
            public void onScrolled(@NonNull RecyclerView recyclerView, int dx, int dy) {
                super.onScrolled(recyclerView, dx, dy);
                LinearLayoutManager linearLayoutManager = (LinearLayoutManager) recyclerView.getLayoutManager();
                if (!isLoading) {
                    if (linearLayoutManager != null && linearLayoutManager.findLastCompletelyVisibleItemPosition() == dataset.size() - 1) {
                        isLoading = true;
                        getMoreData();
                    }
                }
            }
        });
    }

    /**
     *Inializes volley request queue and determines the dataset to fill list view with
     * @param viewType View to be initialized
     */
    private void initializeElements(String viewType) throws JSONException{
        queue = Volley.newRequestQueue(this);
        searchView = findViewById(R.id.searchIngredient);
        searchView.setQueryHint("Search for ingredient");

        searchView.setOnQueryTextListener(new SearchView.OnQueryTextListener() {
            @Override
            public boolean onQueryTextSubmit(String query) {
                try {
                    URL_TO_USE = URLs.updatePaginatedQueryUrl(URL_TO_USE, "query", query);
                    URL_TO_USE = URLs.updatePaginatedQueryUrl(URL_TO_USE, "pageNo", "0");
                    dataset.clear();
                    popData();
                } catch (Exception e) {
                    e.printStackTrace();
                    return false;
                }
                return true;
            }
            @Override
            public boolean onQueryTextChange(String newText) {
                return false;
            }
        });

        newRecipe = findViewById(R.id.createRecipe);
        newRecipe.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View view) {
                String q = selectedToString();
                Intent myRecipesIntent = new Intent(getApplicationContext(), ListView.class);
                myRecipesIntent.putExtra("SwitchView", q);
                startActivity(myRecipesIntent);
            }
        });

        textView = findViewById(R.id.selectedText);

    }

    private void getMoreData() {
        URL_TO_USE = URLs.getNextPaginatedQueryPageUrl(URL_TO_USE);
        popData();
        isLoading = false;
    }

    /**
     * Populates list view with recipes from selected database endpoint
     */
    private void popData() {
        JsonObjectRequest recipeRequest = new JsonObjectRequest(Request.Method.GET, URL_TO_USE, null, new Response.Listener<JSONObject>() {
            @Override
            public void onResponse(JSONObject response) {
                int pageSize = 0;
                if (response != null) {
                    int i = 0;
                    JSONArray recipeArray = null;
                    try {
                        recipeArray = response.getJSONArray("content");
                        pageSize = response.getInt("size");
                    } catch (JSONException e) {
                        e.printStackTrace();
                    }
                    while (!recipeArray.isNull(i) && i <=pageSize) {
                        try {
                            if(URL_TO_USE.contains(URL_USERS)) {
                                dataset.add(new Recipe(getUser(i, recipeArray)));
                            } else {
                                dataset.add(new Recipe(getIngredient(i, recipeArray)));
                            }
                            recyclerViewAdapter.notifyDataSetChanged();
                        } catch (JSONException e) {
                            Toast.makeText(IngredientListView.this, e.toString(), Toast.LENGTH_SHORT).show();
                        }
                        i++;
                    }

                }
            }
        }, new Response.ErrorListener() {
            /**
             *Does not fill recipes if no data is returned from database
             * @param error
             */
            @Override
            public void onErrorResponse(VolleyError error) {

            }
        });
        queue.add(recipeRequest);
    }

    @NonNull
    private String getIngredient(int i, JSONArray array) throws JSONException {
        JSONObject json = array.getJSONObject(i);
        return json.getString("name");
    }

    @NonNull
    private String getUser(int i, JSONArray array) throws JSONException {
        JSONObject json = array.getJSONObject(i);
        return json.getString("email") + " -- " + json.getString("role");
    }

    /**
     * Initialize recyclerView adapter
     */
    private void setupAdapter() {
        recyclerViewAdapter = new RecyclerViewAdapter(dataset, this, "i");
        recyclerView.setAdapter(recyclerViewAdapter);
    }

    /**
     *Opens new recipe view on click and passes recpe object to new activity
     * @param position Position of recipe in view
     */
    @Override
    public void onRecipeClick(int position) {
        if(URL_TO_USE.contains(URL_USERS)) {

        } else {
            String ingredient = dataset.get(position).getRecipeName();
            if(selected.contains(ingredient)) {
                selected.remove(ingredient);
            } else {
                selected.add(ingredient);
            }

            textView.setText(selectedToString());
            recyclerViewAdapter.notifyDataSetChanged();
        }
    }

    private String selectedToString(){
        String q = "";
        for(String s : selected) {
            q += s + ",";
        }
        return q;
    }

//    private String sendUserRequest(String role) {
//
//    }
}