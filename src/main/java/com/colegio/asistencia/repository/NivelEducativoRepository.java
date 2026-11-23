package com.colegio.asistencia.repository;

import com.colegio.asistencia.model.EnumNivel;
import com.colegio.asistencia.model.NivelEducativo;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.Optional;

@Repository
public interface NivelEducativoRepository extends JpaRepository<NivelEducativo, Long> {

    Optional<NivelEducativo> findByCodigo(EnumNivel codigo);
}
