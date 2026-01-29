package com.bmsedge.device.mapper;

import com.bmsedge.device.dto.LocationDTO;
import com.bmsedge.device.model.Location;
import javax.annotation.processing.Generated;
import org.springframework.stereotype.Component;

@Generated(
    value = "org.mapstruct.ap.MappingProcessor",
    date = "2026-01-29T12:48:00+0530",
    comments = "version: 1.5.5.Final, compiler: javac, environment: Java 17.0.12 (Oracle Corporation)"
)
@Component
public class LocationMapperImpl implements LocationMapper {

    @Override
    public LocationDTO toDTO(Location entity) {
        if ( entity == null ) {
            return null;
        }

        LocationDTO.LocationDTOBuilder locationDTO = LocationDTO.builder();

        locationDTO.id( entity.getId() );
        locationDTO.locationCode( entity.getLocationCode() );
        locationDTO.locationName( entity.getLocationName() );
        locationDTO.address( entity.getAddress() );
        locationDTO.city( entity.getCity() );
        locationDTO.state( entity.getState() );
        locationDTO.country( entity.getCountry() );
        locationDTO.postalCode( entity.getPostalCode() );
        locationDTO.latitude( entity.getLatitude() );
        locationDTO.longitude( entity.getLongitude() );
        locationDTO.description( entity.getDescription() );
        locationDTO.active( entity.getActive() );

        return locationDTO.build();
    }

    @Override
    public Location toEntity(LocationDTO dto) {
        if ( dto == null ) {
            return null;
        }

        Location.LocationBuilder location = Location.builder();

        location.id( dto.getId() );
        location.locationCode( dto.getLocationCode() );
        location.locationName( dto.getLocationName() );
        location.address( dto.getAddress() );
        location.city( dto.getCity() );
        location.state( dto.getState() );
        location.country( dto.getCountry() );
        location.postalCode( dto.getPostalCode() );
        location.latitude( dto.getLatitude() );
        location.longitude( dto.getLongitude() );
        location.description( dto.getDescription() );
        location.active( dto.getActive() );

        return location.build();
    }

    @Override
    public void updateEntityFromDTO(LocationDTO dto, Location entity) {
        if ( dto == null ) {
            return;
        }

        if ( dto.getLocationCode() != null ) {
            entity.setLocationCode( dto.getLocationCode() );
        }
        if ( dto.getLocationName() != null ) {
            entity.setLocationName( dto.getLocationName() );
        }
        if ( dto.getAddress() != null ) {
            entity.setAddress( dto.getAddress() );
        }
        if ( dto.getCity() != null ) {
            entity.setCity( dto.getCity() );
        }
        if ( dto.getState() != null ) {
            entity.setState( dto.getState() );
        }
        if ( dto.getCountry() != null ) {
            entity.setCountry( dto.getCountry() );
        }
        if ( dto.getPostalCode() != null ) {
            entity.setPostalCode( dto.getPostalCode() );
        }
        if ( dto.getLatitude() != null ) {
            entity.setLatitude( dto.getLatitude() );
        }
        if ( dto.getLongitude() != null ) {
            entity.setLongitude( dto.getLongitude() );
        }
        if ( dto.getDescription() != null ) {
            entity.setDescription( dto.getDescription() );
        }
        if ( dto.getActive() != null ) {
            entity.setActive( dto.getActive() );
        }
    }
}
