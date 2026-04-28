package pack.repository;

import org.springframework.data.jpa.repository.JpaRepository;
import pack.entity.Chat;
import pack.entity.ChatMessage;

import java.util.List;

public interface ChatMessageRepository extends JpaRepository<ChatMessage, Long> {

    List<ChatMessage> findByChatOrderByCreatedAt(Chat chat);
}