package com.rinha.repository;

import com.rinha.model.Payment;
import org.springframework.data.repository.CrudRepository;

import java.time.Instant;
import java.util.List;
import java.util.UUID;

public interface PaymentRepository extends CrudRepository<Payment, UUID> {

    List<Payment> findByRequestedAtBetween(Instant from, Instant to);
}
