package com.bmsedge.device.repository;

import com.bmsedge.device.model.Device;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.JpaSpecificationExecutor;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import java.util.List;
import java.util.Optional;

@Repository
public interface DeviceRepository extends JpaRepository<Device, Long>, JpaSpecificationExecutor<Device> {

    Optional<Device> findByDeviceId(String deviceId);

    List<Device> findByLocationId(Long locationId);

    List<Device> findBySegmentId(Long segmentId);

    List<Device> findByCounterId(Long counterId);

    List<Device> findByActive(Boolean active);

    @Query("SELECT d FROM Device d WHERE d.location.locationCode = :locationCode")
    List<Device> findByLocationCode(@Param("locationCode") String locationCode);

    @Query("SELECT d FROM Device d WHERE d.segment.segmentCode = :segmentCode")
    List<Device> findBySegmentCode(@Param("segmentCode") String segmentCode);

    @Query("SELECT d FROM Device d WHERE d.counter.counterCode = :counterCode")
    List<Device> findByCounterCode(@Param("counterCode") String counterCode);

    @Query("SELECT d FROM Device d WHERE " +
            "LOWER(d.deviceName) LIKE LOWER(CONCAT('%', :searchTerm, '%')) OR " +
            "LOWER(d.deviceId) LIKE LOWER(CONCAT('%', :searchTerm, '%'))")
    List<Device> searchDevices(@Param("searchTerm") String searchTerm);
}
