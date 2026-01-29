package com.bmsedge.device.mapper;

import com.bmsedge.device.dto.CounterDTO;
import com.bmsedge.device.model.Counter;
import javax.annotation.processing.Generated;
import org.springframework.stereotype.Component;

@Generated(
    value = "org.mapstruct.ap.MappingProcessor",
    date = "2026-01-29T12:48:01+0530",
    comments = "version: 1.5.5.Final, compiler: javac, environment: Java 17.0.12 (Oracle Corporation)"
)
@Component
public class CounterMapperImpl implements CounterMapper {

    @Override
    public CounterDTO toDTO(Counter entity) {
        if ( entity == null ) {
            return null;
        }

        CounterDTO.CounterDTOBuilder counterDTO = CounterDTO.builder();

        counterDTO.id( entity.getId() );
        counterDTO.counterCode( entity.getCounterCode() );
        counterDTO.counterName( entity.getCounterName() );
        counterDTO.description( entity.getDescription() );
        counterDTO.counterType( entity.getCounterType() );
        counterDTO.measurementUnit( entity.getMeasurementUnit() );
        counterDTO.currentValue( entity.getCurrentValue() );
        counterDTO.maxValue( entity.getMaxValue() );
        counterDTO.minValue( entity.getMinValue() );
        counterDTO.active( entity.getActive() );

        return counterDTO.build();
    }

    @Override
    public Counter toEntity(CounterDTO dto) {
        if ( dto == null ) {
            return null;
        }

        Counter.CounterBuilder counter = Counter.builder();

        counter.id( dto.getId() );
        counter.counterCode( dto.getCounterCode() );
        counter.counterName( dto.getCounterName() );
        counter.description( dto.getDescription() );
        counter.counterType( dto.getCounterType() );
        counter.measurementUnit( dto.getMeasurementUnit() );
        counter.currentValue( dto.getCurrentValue() );
        counter.maxValue( dto.getMaxValue() );
        counter.minValue( dto.getMinValue() );
        counter.active( dto.getActive() );

        return counter.build();
    }

    @Override
    public void updateEntityFromDTO(CounterDTO dto, Counter entity) {
        if ( dto == null ) {
            return;
        }

        if ( dto.getCounterCode() != null ) {
            entity.setCounterCode( dto.getCounterCode() );
        }
        if ( dto.getCounterName() != null ) {
            entity.setCounterName( dto.getCounterName() );
        }
        if ( dto.getDescription() != null ) {
            entity.setDescription( dto.getDescription() );
        }
        if ( dto.getCounterType() != null ) {
            entity.setCounterType( dto.getCounterType() );
        }
        if ( dto.getMeasurementUnit() != null ) {
            entity.setMeasurementUnit( dto.getMeasurementUnit() );
        }
        if ( dto.getCurrentValue() != null ) {
            entity.setCurrentValue( dto.getCurrentValue() );
        }
        if ( dto.getMaxValue() != null ) {
            entity.setMaxValue( dto.getMaxValue() );
        }
        if ( dto.getMinValue() != null ) {
            entity.setMinValue( dto.getMinValue() );
        }
        if ( dto.getActive() != null ) {
            entity.setActive( dto.getActive() );
        }
    }
}
