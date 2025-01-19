package com.fastcampus.timesaleservice.domain;

import jakarta.persistence.*;
import lombok.Builder;
import lombok.Getter;
import lombok.NoArgsConstructor;
import org.hibernate.proxy.HibernateProxy;
import org.springframework.data.annotation.CreatedDate;
import org.springframework.data.annotation.LastModifiedDate;
import org.springframework.data.jpa.domain.support.AuditingEntityListener;

import java.time.LocalDateTime;

@Entity
@Table(name = "time_sales")
@Getter
@NoArgsConstructor
@EntityListeners(AuditingEntityListener.class)
public class TimeSale {
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "product_id", nullable = false)
    private Product product;

    @Column(nullable = false)
    private Long quantity;

    @Column(nullable = false)
    private Long remainingQuantity;

    @Column(nullable = false)
    private Long discountPrice;

    @Column(nullable = false)
    private LocalDateTime startAt;

    @Column(nullable = false)
    private LocalDateTime endAt;

    @Enumerated(EnumType.STRING)
    @Column(nullable = false)
    private TimeSaleStatus status = TimeSaleStatus.ACTIVE;

    @Version
    private Long version = 0L;

    @CreatedDate
    @Column(nullable = false, updatable = false)
    private LocalDateTime createdAt;

    @LastModifiedDate
    @Column(nullable = false)
    private LocalDateTime updatedAt;

    @Builder
    public TimeSale(Long id, Product product, Long quantity, Long remainingQuantity, Long discountPrice, LocalDateTime startAt, LocalDateTime endAt, TimeSaleStatus status) {
        this.id = id;
        this.product = product;
        this.quantity = quantity;
        this.remainingQuantity = remainingQuantity;
        this.discountPrice = discountPrice;
        this.startAt = startAt;
        this.endAt = endAt;
        this.status = status;
        this.version = 0L;
    }

    public boolean isActive() {
        return status == TimeSaleStatus.ACTIVE;
    }

    public void purchase(Long quantity) {
        validatePurchase(quantity);
        this.remainingQuantity -= quantity;
    }

    private void validatePurchase(Long quantity) {
        validateStatus();
        validateQuantity(quantity);
        validatePeriod();
    }

    private void validateStatus() {
        if (status != TimeSaleStatus.ACTIVE) {
            throw new IllegalStateException("Time sale is not active");
        }
    }

    private void validateQuantity(Long quantity) {
        if (remainingQuantity < quantity) {
            throw new IllegalStateException("Not enough quantity available");
        }
    }

    private void validatePeriod() {
        LocalDateTime now = LocalDateTime.now();
        if (now.isBefore(startAt) || now.isAfter(endAt)) {
            throw new IllegalStateException("Time sale is not in valid period");
        }
    }

    public Product getProduct() {
        if (this.product instanceof HibernateProxy) {
            return (Product) ((HibernateProxy) this.product).getHibernateLazyInitializer().getImplementation();
        }
        return this.product;
    }
}
