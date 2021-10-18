package com.example.pantryparserbackend.users;

import com.example.pantryparserbackend.Recipes.Recipe;
import com.example.pantryparserbackend.Recipes.RecipeRepository;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.web.bind.annotation.*;

import java.util.Arrays;
import java.util.List;
import java.util.Set;

@RestController
public class UserController {

    private String success = "{\"message\":\"success\"}";
    private String failure = "{\"message\":\"failure\"}";

    @Autowired
    private UserRepository userRepository;
    @Autowired
    private RecipeRepository recipeRepository;

    @GetMapping("/")
    public String welcome() {
        return "Pantry Parser Super Cool Homepage";
    }

    @GetMapping(path = "/user/{id}")
    public User getUserById(@PathVariable int id)
    {
        return userRepository.findById(id);
    }

    @PostMapping(path = "/user")
    String createUser(@RequestBody User users){
        if (users == null)
            return failure;
        userRepository.save(users);
        return success;
    }

    @PostMapping(path = "/login")
    public String login(@RequestBody Login login){
        if (login == null)
            return failure;
        User actual = userRepository.findByEmail(login.email);
        if (actual == null)
            // Email not found
            return failure;
        if(actual.authenticate(login.password)){
            return success;
        }else {
            // Password incorrect
            return failure;
        }
    }

    @GetMapping(path = "/user/{user_id}/recipes")
    public List<Recipe> allRecipes(@PathVariable int user_id){
        User u = userRepository.findById(user_id);
        return u.getRecipes();
    }

    @GetMapping(path = "/user/{user_id}/favorites")
    public List<Recipe> allFavorites(@PathVariable int user_id){
        User u = userRepository.findById(user_id);
        return u.getFavorites();
    }
    @PatchMapping(path = "/user/{user_id}/favorites/{recipe_id}")
    public String favorite(@PathVariable int user_id, @PathVariable int recipe_id){
        User u = userRepository.findById(user_id);
        Recipe r = recipeRepository.findById(recipe_id);
        u.favorite(r);
        userRepository.save(u);
        return success;
    }
    @DeleteMapping(path = "/user/{user_id}/favorites/{recipe_id}")
    public String unfavorite(@PathVariable int user_id, @PathVariable int recipe_id){
        User u = userRepository.findById(user_id);
        Recipe r = recipeRepository.findById(recipe_id);
        u.unfavorite(r);
        userRepository.save(u);
        return success;
    }
}
