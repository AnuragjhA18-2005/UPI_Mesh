package com.example.UPImesh.repositories;

import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import com.example.UPImesh.model.Transaction;

@Repository
public interface TransactionRepo extends JpaRepository<Transaction,Long>{
    boolean existsByPacketId(String packetId);
}

