package com.colegio.asistencia.repository;

import com.colegio.asistencia.model.Matricula;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.List;
import java.util.Optional;

@Repository
public interface MatriculaRepository extends JpaRepository<Matricula, Long> {

    Optional<Matricula> findByEstudianteDniAndAnioEscolarActivoTrue(String dni);

    Optional<Matricula> findByEstudianteCodigoQrAndAnioEscolarActivoTrue(String codigoQr);

    Optional<Matricula> findByEstudianteIdAndAnioEscolarActivoTrue(Long estudianteId);

    List<Matricula> findBySeccionIdAndAnioEscolarActivoTrue(Long seccionId);

    List<Matricula> findByAnioEscolarActivoTrue();
}
