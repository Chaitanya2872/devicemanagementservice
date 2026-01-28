package com.bmsedge.device.mapper;

import com.bmsedge.device.dto.SegmentDTO;
import com.bmsedge.device.model.Segment;
import javax.annotation.processing.Generated;
import org.springframework.stereotype.Component;

@Generated(
    value = "org.mapstruct.ap.MappingProcessor",
    date = "2026-01-26T08:15:01+0530",
    comments = "version: 1.5.5.Final, compiler: javac, environment: Java 17.0.12 (Oracle Corporation)"
)
@Component
public class SegmentMapperImpl implements SegmentMapper {

    @Override
    public SegmentDTO toDTO(Segment entity) {
        if ( entity == null ) {
            return null;
        }

        SegmentDTO.SegmentDTOBuilder segmentDTO = SegmentDTO.builder();

        segmentDTO.id( entity.getId() );
        segmentDTO.segmentCode( entity.getSegmentCode() );
        segmentDTO.segmentName( entity.getSegmentName() );
        segmentDTO.description( entity.getDescription() );
        segmentDTO.category( entity.getCategory() );
        segmentDTO.businessUnit( entity.getBusinessUnit() );
        segmentDTO.department( entity.getDepartment() );
        segmentDTO.active( entity.getActive() );

        return segmentDTO.build();
    }

    @Override
    public Segment toEntity(SegmentDTO dto) {
        if ( dto == null ) {
            return null;
        }

        Segment.SegmentBuilder segment = Segment.builder();

        segment.id( dto.getId() );
        segment.segmentCode( dto.getSegmentCode() );
        segment.segmentName( dto.getSegmentName() );
        segment.description( dto.getDescription() );
        segment.category( dto.getCategory() );
        segment.businessUnit( dto.getBusinessUnit() );
        segment.department( dto.getDepartment() );
        segment.active( dto.getActive() );

        return segment.build();
    }

    @Override
    public void updateEntityFromDTO(SegmentDTO dto, Segment entity) {
        if ( dto == null ) {
            return;
        }

        if ( dto.getSegmentCode() != null ) {
            entity.setSegmentCode( dto.getSegmentCode() );
        }
        if ( dto.getSegmentName() != null ) {
            entity.setSegmentName( dto.getSegmentName() );
        }
        if ( dto.getDescription() != null ) {
            entity.setDescription( dto.getDescription() );
        }
        if ( dto.getCategory() != null ) {
            entity.setCategory( dto.getCategory() );
        }
        if ( dto.getBusinessUnit() != null ) {
            entity.setBusinessUnit( dto.getBusinessUnit() );
        }
        if ( dto.getDepartment() != null ) {
            entity.setDepartment( dto.getDepartment() );
        }
        if ( dto.getActive() != null ) {
            entity.setActive( dto.getActive() );
        }
    }
}
