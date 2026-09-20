package KarmaTute.KarmaTute.repository;
import KarmaTute.KarmaTute.entity.GeneratedQuestion;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;
import java.util.List;
@Repository
public interface GeneratedQuestionRepository extends JpaRepository<GeneratedQuestion, Long> {
    List<GeneratedQuestion> findBySessionIdAndLevel(Long sessionId, Integer level);
    List<GeneratedQuestion> findBySessionId(Long sessionId);
}
