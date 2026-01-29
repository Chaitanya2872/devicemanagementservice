package com.bmsedge.device.mapper;

import com.bmsedge.device.dto.DeviceDTO;
import com.bmsedge.device.model.Counter;
import com.bmsedge.device.model.Device;
import com.bmsedge.device.model.Location;
import com.bmsedge.device.model.Segment;
import javax.annotation.processing.Generated;
import org.springframework.stereotype.Component;

@Generated(
    value = "org.mapstruct.ap.MappingProcessor",
    date = "2026-01-29T12:48:01+0530",
    comments = "version: 1.5.5.Final, compiler: javac, environment: Java 17.0.12 (Oracle Corporation)"
)
@Component
public class DeviceMapperImpl implements DeviceMapper {

    @Override
    public DeviceDTO toDTO(Device entity) {
        if ( entity == null ) {
            return null;
        }

        DeviceDTO.DeviceDTOBuilder deviceDTO = DeviceDTO.builder();

        deviceDTO.location( entityLocationLocationName( entity ) );
        deviceDTO.segment( entitySegmentSegmentName( entity ) );
        deviceDTO.counterName( entityCounterCounterName( entity ) );
        deviceDTO.id( entity.getId() );
        deviceDTO.deviceId( entity.getDeviceId() );
        deviceDTO.deviceName( entity.getDeviceName() );
        deviceDTO.deviceType( entity.getDeviceType() );
        deviceDTO.manufacturer( entity.getManufacturer() );
        deviceDTO.model( entity.getModel() );
        deviceDTO.serialNumber( entity.getSerialNumber() );
        deviceDTO.status( entity.getStatus() );
        deviceDTO.installationDate( entity.getInstallationDate() );
        deviceDTO.lastMaintenanceDate( entity.getLastMaintenanceDate() );
        deviceDTO.createdAt( entity.getCreatedAt() );
        deviceDTO.updatedAt( entity.getUpdatedAt() );
        deviceDTO.createdBy( entity.getCreatedBy() );
        deviceDTO.updatedBy( entity.getUpdatedBy() );
        deviceDTO.active( entity.getActive() );
        deviceDTO.notes( entity.getNotes() );

        return deviceDTO.build();
    }

    @Override
    public Device toEntity(DeviceDTO dto) {
        if ( dto == null ) {
            return null;
        }

        Device.DeviceBuilder device = Device.builder();

        device.id( dto.getId() );
        device.deviceId( dto.getDeviceId() );
        device.deviceName( dto.getDeviceName() );
        device.deviceType( dto.getDeviceType() );
        device.manufacturer( dto.getManufacturer() );
        device.model( dto.getModel() );
        device.serialNumber( dto.getSerialNumber() );
        device.status( dto.getStatus() );
        device.installationDate( dto.getInstallationDate() );
        device.lastMaintenanceDate( dto.getLastMaintenanceDate() );
        device.createdAt( dto.getCreatedAt() );
        device.updatedAt( dto.getUpdatedAt() );
        device.createdBy( dto.getCreatedBy() );
        device.updatedBy( dto.getUpdatedBy() );
        device.active( dto.getActive() );
        device.notes( dto.getNotes() );

        return device.build();
    }

    @Override
    public void updateEntityFromDTO(DeviceDTO dto, Device entity) {
        if ( dto == null ) {
            return;
        }

        if ( dto.getDeviceId() != null ) {
            entity.setDeviceId( dto.getDeviceId() );
        }
        if ( dto.getDeviceName() != null ) {
            entity.setDeviceName( dto.getDeviceName() );
        }
        if ( dto.getDeviceType() != null ) {
            entity.setDeviceType( dto.getDeviceType() );
        }
        if ( dto.getManufacturer() != null ) {
            entity.setManufacturer( dto.getManufacturer() );
        }
        if ( dto.getModel() != null ) {
            entity.setModel( dto.getModel() );
        }
        if ( dto.getSerialNumber() != null ) {
            entity.setSerialNumber( dto.getSerialNumber() );
        }
        if ( dto.getStatus() != null ) {
            entity.setStatus( dto.getStatus() );
        }
        if ( dto.getInstallationDate() != null ) {
            entity.setInstallationDate( dto.getInstallationDate() );
        }
        if ( dto.getLastMaintenanceDate() != null ) {
            entity.setLastMaintenanceDate( dto.getLastMaintenanceDate() );
        }
        if ( dto.getUpdatedAt() != null ) {
            entity.setUpdatedAt( dto.getUpdatedAt() );
        }
        if ( dto.getUpdatedBy() != null ) {
            entity.setUpdatedBy( dto.getUpdatedBy() );
        }
        if ( dto.getActive() != null ) {
            entity.setActive( dto.getActive() );
        }
        if ( dto.getNotes() != null ) {
            entity.setNotes( dto.getNotes() );
        }
    }

    private String entityLocationLocationName(Device device) {
        if ( device == null ) {
            return null;
        }
        Location location = device.getLocation();
        if ( location == null ) {
            return null;
        }
        String locationName = location.getLocationName();
        if ( locationName == null ) {
            return null;
        }
        return locationName;
    }

    private String entitySegmentSegmentName(Device device) {
        if ( device == null ) {
            return null;
        }
        Segment segment = device.getSegment();
        if ( segment == null ) {
            return null;
        }
        String segmentName = segment.getSegmentName();
        if ( segmentName == null ) {
            return null;
        }
        return segmentName;
    }

    private String entityCounterCounterName(Device device) {
        if ( device == null ) {
            return null;
        }
        Counter counter = device.getCounter();
        if ( counter == null ) {
            return null;
        }
        String counterName = counter.getCounterName();
        if ( counterName == null ) {
            return null;
        }
        return counterName;
    }
}
