package com.ciro.orderservice.model;

import jakarta.persistence.*;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

import java.math.BigDecimal;

/**
 * A single line of an order (a product, its quantity and the price at purchase time).
 * It only exists as part of an Order — it is a child of the aggregate, never standalone.
 */
@Entity
@Getter
@Setter
@NoArgsConstructor
@Table(name = "order_items")
public class OrderItem
{
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    private String productName;
    private Long productId;
    private int quantity;

    // Money is BigDecimal, never double: doubles carry binary rounding errors (0.1 + 0.2 != 0.3),
    // unacceptable for prices.
    private BigDecimal unitPrice;

    // Owning side of the relationship: the foreign key (order_id) lives in this table, so this is
    // the side Hibernate reads when persisting the link.
    // LAZY: loading an item does NOT drag in the whole Order unless it's actually accessed by for example item.getOrder()
    // (@ManyToOne defaults to EAGER, so we force LAZY on purpose).
    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "order_id")
    private Order order;
}
