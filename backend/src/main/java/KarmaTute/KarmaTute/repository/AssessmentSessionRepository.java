package KarmaTute.KarmaTute.repository;
import KarmaTute.KarmaTute.entity.AssessmentSession;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;
import java.util.List;
import java.util.Optional;
@Repository
public interface AssessmentSessionRepository extends JpaRepository<AssessmentSession, Long> {
    List<AssessmentSession> findByUserId(Long userId);
    Optional<AssessmentSession> findTopByUserIdOrderByStartedAtDesc(Long userId);
}
