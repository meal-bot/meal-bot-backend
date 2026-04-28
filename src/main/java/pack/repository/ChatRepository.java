package pack.repository;

import org.springframework.data.jpa.repository.JpaRepository;
import pack.entity.Chat;
import pack.entity.User;

import java.util.List;
import java.util.Optional;

public interface ChatRepository extends JpaRepository<Chat, Long> {

    List<Chat> findByUserOrderByCreatedAtDesc(User user);

    Optional<Chat> findByIdAndUser(Long id, User user);
}
