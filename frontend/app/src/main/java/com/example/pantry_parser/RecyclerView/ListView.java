package com.example.pantry_parser.RecyclerView;

import android.content.Intent;
import android.os.Bundle;
import android.view.View;
import android.widget.CompoundButton;
import android.widget.SearchView;
import android.widget.Switch;
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
import com.example.pantry_parser.Pages.Recipe_Page;
import com.example.pantry_parser.Pages.Settings.Settings_Page;
import com.example.pantry_parser.R;
import com.example.pantry_parser.Recipe;
import com.google.android.material.floatingactionbutton.FloatingActionButton;
import com.squareup.picasso.Picasso;

import org.json.JSONArray;
import org.json.JSONException;
import org.json.JSONObject;

import java.util.ArrayList;

public class ListView extends AppCompatActivity implements RecyclerViewAdapter.OnRecipeListener {
    RecyclerView recyclerView;
    RecyclerViewAdapter recyclerViewAdapter;
    ArrayList<Recipe> dataset = new ArrayList<>();
    boolean isLoading = false;
    private static final String URL_RECIPES = "http://coms-309-032.cs.iastate.edu:8080/recipes?pageNo=0";
    private static final String URL_USER = "http://coms-309-032.cs.iastate.edu:8080/user/1/recipes/";
    private static final String URL_FAV = "http://coms-309-032.cs.iastate.edu:8080/user/1/favorites/";
    String URL_TO_USE;
    private RequestQueue queue;
    FloatingActionButton newRecipe;
    SearchView searchView;
    Switch toggle;

    /**
     *Create listview activity and instantiate elements
     * @param savedInstanceState
     */
    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_list_view);
        String viewType = (String) getIntent().getSerializableExtra("SwitchView");

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
                        //getMoreData();
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
        searchView = findViewById(R.id.searchRecipe);
        searchView.setQueryHint("Search by Recipe");
        toggle = findViewById(R.id.toggle);

        toggle.setOnCheckedChangeListener(new CompoundButton.OnCheckedChangeListener() {
            @Override
            public void onCheckedChanged(CompoundButton buttonView, boolean isChecked) {
                if (isChecked == true){
                    searchView.setQueryHint("Search By Recipe");
                }
                else {
                    searchView.setQueryHint("Search By Ingredient");
                }
            }
        });


        searchView.setOnQueryTextListener(new SearchView.OnQueryTextListener() {
            @Override
            public boolean onQueryTextSubmit(String query) {
                try {
                    searchByIngredient(query);
                } catch (JSONException e) {
                    e.printStackTrace();
                }
                return true;
            }
            @Override
            public boolean onQueryTextChange(String newText) {
                return false;
            }
        });
        newRecipe = findViewById(R.id.addRecipeButton);
        newRecipe.setOnClickListener(new View.OnClickListener() {

            /**
             *
             * @param view
             */
            @Override
            public void onClick(View view) {
                Intent intent = new Intent(getApplicationContext(), Settings_Page.class);
            }
        });
        newRecipe.hide();

        switch (viewType){
            case ("ALL_RECIPES"):
                URL_TO_USE = URL_RECIPES;
                break;

            case ("MY_RECIPES"):
            URL_TO_USE = URL_USER;
                newRecipe.show();
                break;

            case ("FAV_RECIPES"):
                URL_TO_USE = URL_FAV;
                break;
        }
    }

    /**
     * Method to get more recipes once the user scrolls to the end of the current view
     */
//    private void getMoreData() {
//        if (URL_TO_USE == URL_RECIPES) {
//            dataset.add(null);
//            recyclerViewAdapter.notifyItemInserted(dataset.size() - 1);
//            dataset.remove(dataset.size() - 1);
//            int currentSize = dataset.size();
//            int nextSize = currentSize + 10;
//            while (currentSize < nextSize) {
//                Recipe recipe = new Recipe("Recipe " + currentSize);
//                recipe.setTimeToMake(currentSize);
//                recipe.setRating((float) currentSize / 5);
//                dataset.add(recipe);
//                currentSize++;
//            }
//            recyclerViewAdapter.notifyDataSetChanged();
//            isLoading = false;
//        }
//    }

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
                            Recipe recipe = getRecipe(i, recipeArray);
                            dataset.add(recipe);
                            recyclerViewAdapter.notifyDataSetChanged();
                        } catch (JSONException e) {
                            Toast.makeText(ListView.this, e.toString(), Toast.LENGTH_SHORT).show();
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
    private Recipe getRecipe(int i, JSONArray recipeArray) throws JSONException {
        JSONObject JSONRecipe = recipeArray.getJSONObject(i);
        Recipe recipe = new Recipe(JSONRecipe.getString("name"));
        recipe.setRecipeID(JSONRecipe.getString("id"));
        recipe.setTimeToMake(JSONRecipe.getInt("time"));
        recipe.setSummary(JSONRecipe.getString("summary"));
        recipe.setAuthor(JSONRecipe.getString("creatorName"));
        recipe.setUserId(JSONRecipe.getInt("creatorId"));
        recipe.setChefVerified(JSONRecipe.getBoolean("chef_verified"));
        recipe.setRating((float) JSONRecipe.getDouble("rating"));
        ArrayList<String> ingredients = new ArrayList<>();
        JSONArray jsonIngredients = JSONRecipe.getJSONArray("ingredients");
        for (int j = 0; j< jsonIngredients.length();j++){
            ingredients.add(jsonIngredients.getJSONObject(j).getString("name"));
        }
        recipe.setIngredients(ingredients);

        ArrayList<String> steps = new ArrayList<>();
        JSONArray jsonSteps = JSONRecipe.getJSONArray("steps");
        for (int j = 0; j< jsonSteps.length();j++){
            steps.add(jsonSteps.getJSONObject(j).getString("name"));
        }
        recipe.setSteps(steps);
        if (JSONRecipe.getString("imagePath") != "null") {
            Picasso.get().load("http://coms-309-032.cs.iastate.edu:8080/recipe/" + recipe.getRecipeID() + "/image").into(recipe.getImage());
        }
        return recipe;
    }

    /**
     * Initialize recyclerView adapter
     */
    private void setupAdapter() {
        recyclerViewAdapter = new RecyclerViewAdapter(dataset, this);
        recyclerView.setAdapter(recyclerViewAdapter);
    }

    /**
     *Opens new recipe view on click and passes recpe object to new activity
     * @param position Position of recipe in view
     */
    @Override
    public void onRecipeClick(int position) {
        dataset.get(position);
        Intent intent = new Intent(this, Recipe_Page.class);
        intent.putExtra("Recipe", dataset.get(position));
        startActivity(intent);
    }

    public void searchByIngredient(String query) throws JSONException {
        dataset.clear();
        recyclerViewAdapter.notifyDataSetChanged();
        JSONArray jsonArray = new JSONArray();
        jsonArray.put(query);

        JSONObject jsonObject = new JSONObject();
        jsonObject.put("ingredients", jsonArray);

        JsonObjectRequest recipeByIng = new JsonObjectRequest(Request.Method.PUT, "http://coms-309-032.cs.iastate.edu:8080/pantry-parser", jsonObject, new Response.Listener<JSONObject>() {
            @Override
            public void onResponse(JSONObject response) {
                if (response != null) {
                    int i = 0;
                    JSONArray recipeArray = null;
                    try {
                        recipeArray = response.getJSONArray("content");
                    } catch (JSONException e) {
                        e.printStackTrace();
                    }
                    while (!recipeArray.isNull(i) && i < recipeArray.length()) {
                        try {
                            Recipe recipe = getRecipe(i, recipeArray);
                            dataset.add(recipe);
                            recyclerViewAdapter.notifyDataSetChanged();
                        } catch (JSONException e) {
                            Toast.makeText(ListView.this, e.toString(), Toast.LENGTH_SHORT).show();
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
                System.out.println("Fail");
            }
        });
        queue.add(recipeByIng);
    }
}