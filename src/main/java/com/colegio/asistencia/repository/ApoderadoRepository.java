package com.colegio.asistencia.repository;

import com.colegio.asistencia.model.Apoderado;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import java.util.List;
import java.util.Optional;

@Repository
public interface ApoderadoRepository extends JpaRepository<Apoderado, Long> {

    Optional<Apoderado> findByDni(String dni);

    Optional<Apoderado> findByUsuarioUsername(String username);

    List<Apoderado> findByEstudiantesId(Long estudianteId);

    boolean existsByDni(String dni);

    List<Apoderado> findAllByOrderByApellidosAscNombresAsc();

    @Query("SELECT a FROM Apoderado a WHERE " +
           "LOWER(a.dni) LIKE LOWER(CONCAT('%', :query, '%')) OR " +
           "LOWER(a.nombres) LIKE LOWER(CONCAT('%', :query, '%')) OR " +
           "LOWER(a.apellidos) LIKE LOWER(CONCAT('%', :query, '%')) " +
           "ORDER BY a.apellidos ASC, a.nombres ASC")
    List<Apoderado> buscarPorTermino(@Param("query") String query);
}
