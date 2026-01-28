package com.bmsedge.device.mapper;


import com.bmsedge.device.dto.SegmentDTO;
import com.bmsedge.device.model.Segment;
import org.mapstruct.BeanMapping;
import org.mapstruct.Mapper;
import org.mapstruct.Mapping;
import org.mapstruct.MappingTarget;
import org.mapstruct.NullValuePropertyMappingStrategy;
import org.mapstruct.ReportingPolicy;

@Mapper(componentModel = "spring", unmappedTargetPolicy = ReportingPolicy.IGNORE)
public interface SegmentMapper {

    SegmentDTO toDTO(Segment entity);

    @Mapping(target = "devices", ignore = true)
    @Mapping(target = "version", ignore = true)
    Segment toEntity(SegmentDTO dto);

    @BeanMapping(nullValuePropertyMappingStrategy = NullValuePropertyMappingStrategy.IGNORE)
    @Mapping(target = "id", ignore = true)
    @Mapping(target = "devices", ignore = true)
    @Mapping(target = "version", ignore = true)
    void updateEntityFromDTO(SegmentDTO dto, @MappingTarget Segment entity);
}
