package com.mealbot.repository;

import org.springframework.data.jpa.repository.JpaRepository;
import com.mealbot.entity.Inbody;
import com.mealbot.entity.User;

import java.util.List;
import java.util.Optional;

public interface InbodyRepository extends JpaRepository<Inbody, Long> {
    List<Inbody> findByUserOrderByMeasuredAtDesc(User user);
    Optional<Inbody> findByIdAndUser(Long id, User user);
}