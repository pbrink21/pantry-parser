package com.example.pantryparserbackend.Users;

import com.example.pantryparserbackend.Requests.UserRequest;
import com.example.pantryparserbackend.Services.EmailService;
import com.example.pantryparserbackend.Services.IPService;
import com.example.pantryparserbackend.Services.PermissionService;
import com.example.pantryparserbackend.Utils.*;
import com.example.pantryparserbackend.Requests.LoginRequest;
import com.example.pantryparserbackend.Recipes.Recipe;
import com.example.pantryparserbackend.Recipes.RecipeRepository;
import com.example.pantryparserbackend.Websockets.FavoriteSocket;
import io.swagger.annotations.Api;
import io.swagger.annotations.ApiOperation;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Pageable;
import org.springframework.data.domain.Sort;
import org.springframework.web.bind.annotation.*;

import javax.servlet.http.HttpServletRequest;

/**
 * User controller, responsible for all user stuff
 */
@Api(value = "User Controller", description = "Contains all of the calls for the users")
@RestController
public class UserController {

    @Autowired
    private UserRepository userRepository;
    @Autowired
    private RecipeRepository recipeRepository;
    @Autowired
    private FavoriteSocket favoriteSocket;
    @Autowired
    private IPService ipService;
    @Autowired
    private PermissionService permissionService;
    @Autowired
    private EmailService emailService;

    /**
     * this is just a test method to show us our server is up
     */
    @ApiOperation(value = "The super cool homepage")
    @GetMapping("/")
    public String welcome() {
        return "Pantry Parser Super Cool Homepage";
    }

    /**
     * gets a list of all users in the database
     * @return list of users
     */
    @ApiOperation(value = "Gets all of the users in the database")
    @GetMapping(path = "/users")
    public Page<User> getAllUsers(@RequestParam(defaultValue = "0") Integer pageNo, @RequestParam(defaultValue = "15") Integer perPage, HttpServletRequest request) {
        if(!permissionService.canUser("ViewAny", null, request))
            return null;
        Pageable page = PageRequest.of(pageNo, perPage, Sort.by("email"));
        return userRepository.findAll(page);
    }

    /**
     * gets a user from the database by the provided id
     * @param user_id input user id
     * @return user
     */
    @ApiOperation(value = "Gets the given user's information")
    @GetMapping(path = "/user/{user_id}")
    public User getUserById(@PathVariable int user_id)
    {
        return userRepository.findById(user_id);
    }

    /**
     * finds a user by the input email
     * @param email input email
     * @return user
     * @throws Exception if the email doesn't exist in the database
     */
    @ApiOperation(value = "Finds a user by a given email")
    @GetMapping(path = "/user/email/{email}")
    public User getUserByEmail(@PathVariable String email) {
        return userRepository.findByEmail(email);
    }

    /**
     * creates a new user
     * @param user new user input data
     * @return either success or a failure message
     */
    @ApiOperation(value = "Creates a new user")
    @PostMapping(path = "/users")
    String createUser(@RequestBody User user, HttpServletRequest request){
        if(!permissionService.canUser("Create", null, request))
            return MessageUtil.newResponseMessage(false, "You do not have permission to do that");

        if (user == null)
            return MessageUtil.newResponseMessage(false, "User was null");

        try {
            userRepository.save(user);
        }
        catch(Exception ex) {
            if(userRepository.findByEmail(user.getEmail()) != null)
                return MessageUtil.newResponseMessage(false, "Email already used");
            return MessageUtil.newResponseMessage(false, "some fields were left empty");
        }

        return emailService.sendEmail("VerifyEmail", user);
    }

    /**
     * Updates the user's information
     * @param userRequest user object that is to be updated
     * @return either success or a failure message
     */
    @ApiOperation(value = "Updates a given user")
    @PatchMapping(path = "/user/{user_id}/update")
    String updateUser(@RequestBody UserRequest userRequest, @PathVariable int user_id, HttpServletRequest request){
        User user = userRepository.findById(user_id);
        if(user == null)
            return MessageUtil.newResponseMessage(false, "User not found");
        if(!permissionService.canUser("Update", user, request))
            return MessageUtil.newResponseMessage(false, "You do not have permission to do that");

        if(user.authenticate(userRequest.password)){
            try {
                user.setEmail(userRequest.email);
                user.setDisplayName(userRequest.displayName);
                userRepository.save(user);
            }
            catch(Exception ex) {
                return MessageUtil.newResponseMessage(false, "Error updating information");
            }

            return MessageUtil.newResponseMessage(true, "User updated");

        }else {
            return MessageUtil.newResponseMessage(false, "Incorrect Login");
        }
    }

    /**
     * attempts to log a user in by checking their credentials against what's in the database
     * @param login a special model with the input email and password
     * @return either success with the user id or a failure message
     */
    @ApiOperation(value = "Logs in a user")
    @PostMapping(path = "/login")
    public String login(@RequestBody LoginRequest login, HttpServletRequest request){
        if (login == null)
            return MessageUtil.newResponseMessage(false, "no login info detected");
        User actual = userRepository.findByEmail(login.email);
        if (actual == null)
            // Email not found
            return MessageUtil.newResponseMessage(false, "email incorrect");
        if(actual.authenticate(login.password)){
            try {
                ipService.saveIP(actual, request);
                return MessageUtil.newResponseMessage(true, "" + actual.getId());
            } catch (Exception ex) {
                ex.printStackTrace();
                return MessageUtil.newResponseMessage(false, "Error getting the IP");
            }

        }else {
            // Password incorrect
            return MessageUtil.newResponseMessage(false, "password incorrect");
        }
    }

    /**
     * gets a list of recipes the provided user has created
     * @param user_id the id of the user
     * @return list of recipes that user created
     */
    @ApiOperation(value = "gets all recipes of a user")
    @GetMapping(path = "/user/{user_id}/recipes")
    public Page<Recipe> allRecipes(@RequestParam(defaultValue = "0") Integer pageNo, @RequestParam(defaultValue = "15") Integer perPage, @RequestParam(defaultValue = "") String query, @PathVariable int user_id){
        User u = userRepository.findById(user_id);
        if(u == null){
            return null;
        }
        Pageable page = PageRequest.of(pageNo, perPage, Sort.by("rating"));
        return query.equals("") ? recipeRepository.getUserCreated(u, page) : recipeRepository.getUserCreatedSearch(u, query, page);
    }

    /**
     * gets a list of a certain user's favorites
     * @param user_id the id of the user
     * @return a list of the user's favorite recipes
     */
    @ApiOperation(value = "Gets all of the favorites of a user")
    @GetMapping(path = "/user/{user_id}/favorites")
    public Page<Recipe> allFavorites(@RequestParam(defaultValue = "0") Integer pageNo, @RequestParam(defaultValue = "15") Integer perPage, @RequestParam(defaultValue = "") String query, @PathVariable int user_id){
        User u = userRepository.findById(user_id);
        if(u == null){
            return null;
        }
        Pageable page = PageRequest.of(pageNo, perPage, Sort.by("rating"));
        return query.equals("") ? recipeRepository.getUserFavorites(user_id, page) : recipeRepository.getUserFavoritesSearch(user_id, query, page);
    }

    /**
     * the route for a user to favorite a recipe
     * @param user_id the id of the user
     * @param recipe_id the id of the recipe
     * @return either success or a failure message
     */
    @ApiOperation(value = "Route to favorite a recipe")
    @PatchMapping(path = "/user/{user_id}/favorite/{recipe_id}")
    public String favorite(@PathVariable int user_id, @PathVariable int recipe_id, HttpServletRequest request){
        User u = ipService.getCurrentUser(request);
        Recipe r = recipeRepository.findById(recipe_id);

        if(!permissionService.canUser("Update", u, request)) {
            return MessageUtil.newResponseMessage(false, "You do not have permission to do that");
        }

        if(u == null || r == null){
            return MessageUtil.newResponseMessage(false, (u == null ? "user " : "recipe ") + "does not exist");
        } else if(u.getFavorites().contains(r)) {
            return MessageUtil.newResponseMessage(false, "releationship already exists");
        } else {
            favoriteSocket.onFavorite(r, u);
            return MessageUtil.newResponseMessage(true, "favorited");
        }
    }
    /**
     * the route for a user to unfavorite a recipe
     * @param user_id the id of the user
     * @param recipe_id the id of the recipe
     * @return either success or a failure message
     */
    @ApiOperation(value = "The route for a user to unfavorite a recipe")
    @DeleteMapping(path = "/user/{user_id}/favorite/{recipe_id}")
    public String unfavorite(@PathVariable int user_id, @PathVariable int recipe_id, HttpServletRequest request){
        User u = ipService.getCurrentUser(request);
        Recipe r = recipeRepository.findById(recipe_id);

        if(!permissionService.canUser("Update", u, request)) {
            return MessageUtil.newResponseMessage(false, "You do not have permission to do that");
        }
        if(u == null || r == null){
            return MessageUtil.newResponseMessage(false, (u == null ? "user " : "recipe ") + "does not exist");
        } else if(!u.getFavorites().contains(r)) {
            return MessageUtil.newResponseMessage(false, "relationship does not exist");
        } else {
            favoriteSocket.onUnfavorite(r, u);
            return MessageUtil.newResponseMessage(true, "successfully unfavorited");
        }
    }


}
