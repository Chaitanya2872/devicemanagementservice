package com.bmsedge.device.mapper;



import com.bmsedge.device.dto.LocationDTO;
import com.bmsedge.device.model.Location;
import org.mapstruct.BeanMapping;
import org.mapstruct.Mapper;
import org.mapstruct.Mapping;
import org.mapstruct.MappingTarget;
import org.mapstruct.NullValuePropertyMappingStrategy;
import org.mapstruct.ReportingPolicy;

@Mapper(componentModel = "spring", unmappedTargetPolicy = ReportingPolicy.IGNORE)
public interface LocationMapper {

    LocationDTO toDTO(Location entity);

    @Mapping(target = "devices", ignore = true)
    @Mapping(target = "version", ignore = true)
    Location toEntity(LocationDTO dto);

    @BeanMapping(nullValuePropertyMappingStrategy = NullValuePropertyMappingStrategy.IGNORE)
    @Mapping(target = "id", ignore = true)
    @Mapping(target = "devices", ignore = true)
    @Mapping(target = "version", ignore = true)
    void updateEntityFromDTO(LocationDTO dto, @MappingTarget Location entity);
}
