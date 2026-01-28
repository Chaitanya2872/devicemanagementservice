package com.bmsedge.device.repository;



import com.bmsedge.device.model.Location;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.List;
import java.util.Optional;

@Repository
public interface LocationRepository extends JpaRepository<Location, Long> {

    Optional<Location> findByLocationCode(String locationCode);

    List<Location> findByActive(Boolean active);

    List<Location> findByCity(String city);

    List<Location> findByCountry(String country);
}
