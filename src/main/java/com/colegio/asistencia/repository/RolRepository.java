package com.colegio.asistencia.repository;

import com.colegio.asistencia.model.EnumRol;
import com.colegio.asistencia.model.Rol;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.Optional;

@Repository
public interface RolRepository extends JpaRepository<Rol, Long> {

    Optional<Rol> findByNombre(EnumRol nombre);
}
