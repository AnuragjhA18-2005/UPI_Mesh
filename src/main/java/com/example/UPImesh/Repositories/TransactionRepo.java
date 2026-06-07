package com.example.UPImesh.Repositories;

import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import com.example.UPImesh.Model.Transaction;

@Repository
public interface TransactionRepo extends JpaRepository<Transaction,Long>{
    boolean existsByPacketId(String packetId);
}
