package com.fastcampus.timesaleservice.repository;

import com.fastcampus.timesaleservice.domain.Product;
import org.springframework.data.jpa.repository.JpaRepository;

public interface ProductRepository extends JpaRepository<Product, Long> {
}
