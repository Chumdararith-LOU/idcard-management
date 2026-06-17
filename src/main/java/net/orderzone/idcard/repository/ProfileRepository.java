package net.orderzone.idcard.repository;

import net.orderzone.idcard.model.Profile;
import net.orderzone.idcard.model.ProfileType;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import java.util.List;
import java.util.Optional;

@Repository
public interface ProfileRepository extends JpaRepository<Profile, Long> {

    // Find by the secure verification UUID
    Optional<Profile> findByUuid(String uuid);

    // Find by human-friendly registration number
    Optional<Profile> findByRegistrationNumber(String registrationNumber);

    // Check if registration number already exists (useful for validation during creation)
    boolean existsByRegistrationNumber(String registrationNumber);

    // Filter profiles by their type (STUDENT, EMPLOYEE, USER)
    List<Profile> findByType(ProfileType type);

    // Search profiles across multiple fields (name, registration number, department, or email)
    @Query("SELECT p FROM Profile p WHERE " +
           "LOWER(p.fullName) LIKE LOWER(CONCAT('%', :query, '%')) OR " +
           "LOWER(p.registrationNumber) LIKE LOWER(CONCAT('%', :query, '%')) OR " +
           "LOWER(p.department) LIKE LOWER(CONCAT('%', :query, '%')) OR " +
           "LOWER(p.email) LIKE LOWER(CONCAT('%', :query, '%'))")
    List<Profile> searchProfiles(@Param("query") String query);
}