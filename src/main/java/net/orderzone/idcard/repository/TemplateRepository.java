package net.orderzone.idcard.repository;

import net.orderzone.idcard.model.Template;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import java.util.List;
import java.util.Optional;

@Repository
public interface TemplateRepository extends JpaRepository<Template, Long> {

    // Check existence by unique template code
    boolean existsByCode(String code);

    // Find template by its unique code
    Optional<Template> findByCode(String code);

    // Flexible search by name or code (case-insensitive)
    @Query("SELECT t FROM Template t WHERE LOWER(t.name) LIKE LOWER(CONCAT('%', :keyword, '%')) OR LOWER(t.code) LIKE LOWER(CONCAT('%', :keyword, '%'))")
    List<Template> searchTemplates(@Param("keyword") String keyword);
}

