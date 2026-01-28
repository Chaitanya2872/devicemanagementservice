package com.bmsedge.device.mapper;

import com.bmsedge.device.dto.CounterDTO;
import com.bmsedge.device.model.Counter;
import org.mapstruct.BeanMapping;
import org.mapstruct.Mapper;
import org.mapstruct.Mapping;
import org.mapstruct.MappingTarget;
import org.mapstruct.NullValuePropertyMappingStrategy;
import org.mapstruct.ReportingPolicy;

@Mapper(componentModel = "spring", unmappedTargetPolicy = ReportingPolicy.IGNORE)
public interface CounterMapper {

    CounterDTO toDTO(Counter entity);

    @Mapping(target = "devices", ignore = true)
    @Mapping(target = "version", ignore = true)
    Counter toEntity(CounterDTO dto);

    @BeanMapping(nullValuePropertyMappingStrategy = NullValuePropertyMappingStrategy.IGNORE)
    @Mapping(target = "id", ignore = true)
    @Mapping(target = "devices", ignore = true)
    @Mapping(target = "version", ignore = true)
    void updateEntityFromDTO(CounterDTO dto, @MappingTarget Counter entity);
}