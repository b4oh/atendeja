package com.atendeja.repository;

import com.atendeja.model.UnidadeSaude;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;
import java.util.List;

@Repository
public interface UnidadeSaudeRepository extends JpaRepository<UnidadeSaude, Long> {
    List<UnidadeSaude> findByAtivaTrue();
    boolean existsByCnes(String cnes);
}