package com.colegio.asistencia.repository;

import com.colegio.asistencia.model.Seccion;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.List;
import java.util.Optional;

@Repository
public interface SeccionRepository extends JpaRepository<Seccion, Long> {

    List<Seccion> findByGradoId(Long gradoId);

    Optional<Seccion> findByNombreAndGradoId(String nombre, Long gradoId);
}
