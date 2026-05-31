package com.atendeja.repository;

import com.atendeja.model.Guiche;
import com.atendeja.enums.StatusGuiche;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;
import java.util.List;

@Repository
public interface GuicheRepository extends JpaRepository<Guiche, Long> {
    List<Guiche> findByUnidadeId(Long unidadeId);
    List<Guiche> findByUnidadeIdAndStatus(Long unidadeId, StatusGuiche status);
}