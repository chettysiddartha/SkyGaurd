package com.example.skyguard;
import org.springframework.data.jpa.repository.JpaRepository; import java.util.Optional;
public interface AircraftRepository extends JpaRepository<Aircraft,Long>{Optional<Aircraft> findByRegistration(String registration);}
