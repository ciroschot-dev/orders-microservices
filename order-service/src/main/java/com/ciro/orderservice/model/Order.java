package com.ciro.orderservice.model;

import com.ciro.orderservice.enums.OrderStatus;
import jakarta.persistence.*;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;
import org.hibernate.annotations.CreationTimestamp;

import java.time.Instant;
import java.util.ArrayList;
import java.util.List;

/**
 * Aggregate root of the ordering domain: an Order owns its items and controls
 * their lifecycle (they are created, persisted and removed through the order).
 */
@Entity
@Getter
@Setter
@NoArgsConstructor
// "order" is a reserved word in SQL, so the table is named "orders" to avoid clashes.
@Table(name = "orders")
public class Order
{
    @Id
    // IDENTITY delegates id generation to Postgres' auto-increment column — predictable and simple.
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    private String customerName;

    // STRING persists the enum name ("PENDING"); the default (ORDINAL) stores its position,
    // which silently corrupts data if the enum is ever reordered.
    @Enumerated(EnumType.STRING)
    private OrderStatus status;

    // Filled in automatically by Hibernate on the first persist — no need to set it by hand.
    @CreationTimestamp
    private Instant createdAt;

    // Inverse side of the relationship: "mappedBy" points to the OrderItem.order field,
    // which is the owner that actually holds the order_id foreign key.
    // cascade = ALL  -> saving/deleting the order propagates to its items (they don't live alone).
    // orphanRemoval  -> dropping an item from this list deletes it from the DB, not just nulls its FK.
    @OneToMany(mappedBy = "order", cascade = CascadeType.ALL, orphanRemoval = true)
    private List<OrderItem> items = new ArrayList<>();

    // Helper that keeps BOTH sides in sync: the owning side (item.order) so Hibernate writes the
    // correct FK, and the inverse side (this list) so the in-memory object stays consistent.
    public void addItem(OrderItem item)
    {
        items.add(item);
        item.setOrder(this);
    }

    // Cutting the back-reference + orphanRemoval is what triggers the item's deletion.
    public void removeItem(OrderItem item)
    {
        items.remove(item);
        item.setOrder(null);
    }
}
