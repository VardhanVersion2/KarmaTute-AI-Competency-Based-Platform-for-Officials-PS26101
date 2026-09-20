package KarmaTute.KarmaTute.repository;
import KarmaTute.KarmaTute.entity.AssessmentMaterial;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;
import java.util.List;
import java.util.Optional;
@Repository
public interface AssessmentMaterialRepository extends JpaRepository<AssessmentMaterial, Long> {
    List<AssessmentMaterial> findByUserId(Long userId);
    Optional<AssessmentMaterial> findByUserIdAndProcessingStatus(Long userId, String status);
    Optional<AssessmentMaterial> findByFileHash(String fileHash);
}
