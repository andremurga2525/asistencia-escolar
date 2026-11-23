package com.colegio.asistencia.repository;

import com.colegio.asistencia.model.Estudiante;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import java.util.List;
import java.util.Optional;

@Repository
public interface EstudianteRepository extends JpaRepository<Estudiante, Long> {

    Optional<Estudiante> findByDni(String dni);

    Optional<Estudiante> findByCodigoQr(String codigoQr);

    boolean existsByDni(String dni);

    List<Estudiante> findAllByOrderByApellidosAscNombresAsc();

    @Query("SELECT e FROM Estudiante e WHERE " +
           "LOWER(e.dni) LIKE LOWER(CONCAT('%', :query, '%')) OR " +
           "LOWER(e.nombres) LIKE LOWER(CONCAT('%', :query, '%')) OR " +
           "LOWER(e.apellidos) LIKE LOWER(CONCAT('%', :query, '%')) " +
           "ORDER BY e.apellidos ASC, e.nombres ASC")
    List<Estudiante> buscarPorTermino(@Param("query") String query);
}
