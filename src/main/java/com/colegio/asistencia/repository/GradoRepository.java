package com.colegio.asistencia.repository;

import com.colegio.asistencia.model.EnumNivel;
import com.colegio.asistencia.model.Grado;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.List;
import java.util.Optional;

@Repository
public interface GradoRepository extends JpaRepository<Grado, Long> {

    List<Grado> findByNivelCodigoOrderByNumeroAsc(EnumNivel nivel);

    Optional<Grado> findByNombreAndNivelCodigo(String nombre, EnumNivel nivel);
}
