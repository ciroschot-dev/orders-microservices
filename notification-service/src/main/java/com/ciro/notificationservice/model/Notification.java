package com.ciro.notificationservice.model;

import lombok.AllArgsConstructor;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;
import org.springframework.data.annotation.Id;
import org.springframework.data.mongodb.core.mapping.Document;

import java.time.Instant;

/**
 * A stored/simulated notification. Modelled as a MongoDB document (no relations, schema-flexible) —
 * the textbook fit for NoSQL: we just append and read, we never JOIN.
 */
@Document(collection = "notifications")
@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
public class Notification
{
    @Id
    private String id;

    private Long orderId;

    private String message;

    private Instant createdAt;
}
