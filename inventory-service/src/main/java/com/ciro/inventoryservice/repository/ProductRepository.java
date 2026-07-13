package com.ciro.inventoryservice.repository;

import com.ciro.inventoryservice.model.Product;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Modifying;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import java.util.Optional;

@Repository
public interface ProductRepository extends JpaRepository<Product, Long>
{
    Optional<Product> findBySku(String sku);

    // Atomic conditional decrement: the DB checks stock and subtracts in one locked operation,
    // so concurrent orders can't both pass the check (no lost update, even in READ COMMITTED).
    // Returns rows affected: 1 = decremented, 0 = not enough stock. That int is the business answer.
    @Modifying
    @Query("""
            UPDATE Product p
            SET p.availableQuantity = p.availableQuantity - :quantity
            WHERE p.id = :productId AND p.availableQuantity >= :quantity
            """)
    int decrementStock(@Param("productId") Long productId, @Param("quantity") int quantity);
}
