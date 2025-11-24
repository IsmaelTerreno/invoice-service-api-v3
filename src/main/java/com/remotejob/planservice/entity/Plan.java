package com.remotejob.planservice.entity;

import com.fasterxml.jackson.databind.JsonNode;
import io.hypersistence.utils.hibernate.type.json.JsonBinaryType;
import jakarta.persistence.*;
import lombok.*;
import org.hibernate.annotations.Type;

import java.time.Instant;
import java.util.UUID;

/**
 * Subscription Plan entity mirroring the NestJS `plans` table schema.
 */
@Getter
@Setter
@AllArgsConstructor
@NoArgsConstructor
@Entity
@Table(name = "plan")
public class Plan {
    @Id
    @GeneratedValue(strategy = GenerationType.UUID)
    @Column(name = "id", updatable = false, nullable = false)
    @EqualsAndHashCode.Include
    private UUID id;

    @Column(name = "user_id", nullable = false)
    private String userId;

    @Column(name = "invoice_id", nullable = false)
    private UUID invoiceId;

    @Column(name = "description", nullable = false)
    private String description;

    @Column(name = "is_active", nullable = false)
    private Boolean isActive;

    @Convert(converter = JsonDynamicConverter.class)
    @Type(JsonBinaryType.class)
    @Column(name = "items", columnDefinition = "jsonb", nullable = false)
    private JsonNode items;

    @Column(name = "status", nullable = false)
    private String status;

    @Column(name = "duration_in_days", nullable = false)
    private Integer durationInDays;

    @Column(name = "expires_at")
    private Instant expiresAt; // nullable
}