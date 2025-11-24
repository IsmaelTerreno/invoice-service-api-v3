package com.remotejob.planservice.mapper;

import com.remotejob.planservice.dto.PlanDto;
import com.remotejob.planservice.entity.Plan;
import org.mapstruct.Mapper;

@Mapper(componentModel = "spring")
public interface PlanMapper {
    PlanDto toDto(Plan entity);
    Plan fromDto(PlanDto dto);
}
