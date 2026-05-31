package com.atendeja.repository;

import com.atendeja.model.TipoExame;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;
import java.util.List;

@Repository
public interface TipoExameRepository extends JpaRepository<TipoExame, Long> {
    List<TipoExame> findByUnidadeIdAndDisponivelTrue(Long unidadeId);
}