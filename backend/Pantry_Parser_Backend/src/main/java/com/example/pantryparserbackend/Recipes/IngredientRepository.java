package com.example.pantryparserbackend.Recipes;

import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.repository.PagingAndSortingRepository;

/**
 * Repository for ingredients
 */
public interface IngredientRepository extends PagingAndSortingRepository<Ingredient, Long> {

    Ingredient findById(int id);
    Ingredient findByName(String name);
}
