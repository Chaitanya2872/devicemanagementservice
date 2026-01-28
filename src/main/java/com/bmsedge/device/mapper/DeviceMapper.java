package com.bmsedge.device.mapper;

import com.bmsedge.device.dto.DeviceDTO;
import com.bmsedge.device.model.Device;
import org.mapstruct.BeanMapping;
import org.mapstruct.Mapper;
import org.mapstruct.Mapping;
import org.mapstruct.MappingTarget;
import org.mapstruct.NullValuePropertyMappingStrategy;
import org.mapstruct.ReportingPolicy;

@Mapper(componentModel = "spring", unmappedTargetPolicy = ReportingPolicy.IGNORE)
public interface DeviceMapper {

    @Mapping(source = "location.locationName", target = "location")
    @Mapping(source = "segment.segmentName", target = "segment")
    @Mapping(source = "counter.counterName", target = "counterName")
    DeviceDTO toDTO(Device entity);

    @Mapping(target = "location", ignore = true)
    @Mapping(target = "segment", ignore = true)
    @Mapping(target = "counter", ignore = true)
    @Mapping(target = "version", ignore = true)
    Device toEntity(DeviceDTO dto);

    @BeanMapping(nullValuePropertyMappingStrategy = NullValuePropertyMappingStrategy.IGNORE)
    @Mapping(target = "id", ignore = true)
    @Mapping(target = "location", ignore = true)
    @Mapping(target = "segment", ignore = true)
    @Mapping(target = "counter", ignore = true)
    @Mapping(target = "createdAt", ignore = true)
    @Mapping(target = "createdBy", ignore = true)
    @Mapping(target = "version", ignore = true)
    void updateEntityFromDTO(DeviceDTO dto, @MappingTarget Device entity);
}