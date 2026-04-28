package pack.repository;

import org.springframework.data.jpa.repository.JpaRepository;
import pack.entity.Inbody;
import pack.entity.User;

import java.util.List;
import java.util.Optional;

public interface InbodyRepository extends JpaRepository<Inbody, Long> {
    List<Inbody> findByUserOrderByMeasuredAtDesc(User user);
    Optional<Inbody> findByIdAndUser(Long id, User user);
}