package KarmaTute.KarmaTute.repository;

import KarmaTute.KarmaTute.entity.UserLearningPreferences;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

@Repository
public interface UserLearningPreferencesRepository extends JpaRepository<UserLearningPreferences, Long> {
}
