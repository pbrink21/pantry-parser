package com.example.pantryparserbackend.users;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Controller;
import org.springframework.web.bind.annotation.*;

import java.util.Optional;

@RestController
public class UsersController {

    private String success = "{\"message\":\"success\"}";
    private String failure = "{\"message\":\"failure\"}";

    @Autowired
    private UsersRepository usersRepository;

    @GetMapping("/")
    public String welcome() {
        return "Pantry Parser Super Cool Homepage";
    }

    @GetMapping(path = "/users/{id}")
    public Users getUserById(@PathVariable int id)
    {
        return usersRepository.findById(id);
    }

    @PostMapping(path = "/users")
    String createUser(@RequestBody Users users){
        if (users == null)
            return failure;
        usersRepository.save(users);
        return success;
    }

    @PostMapping(path = "/login")
    public String login(@RequestBody Login login){
        if (login == null)
            return failure;
        Users actual = usersRepository.findByEmail(login.email);
        if(actual.authenticate(login.password)){
            return success;
        }else {
            return failure;
        }

    }
}
