package com.remotejob.invoiceservice.mapper;

import com.remotejob.invoiceservice.dto.InvoiceDto;
import com.remotejob.invoiceservice.entity.Invoice;
import org.mapstruct.Mapper;
import org.mapstruct.MappingConstants;

/**
 * MapStruct mapper for converting between Invoice entities and DTOs.
 */
@Mapper(componentModel = MappingConstants.ComponentModel.SPRING)
public interface InvoiceMapper {
    /**
     * Converts an Invoice entity to an InvoiceDto.
     *
     * @param invoice The invoice entity
     * @return The invoice DTO
     */
    InvoiceDto toDto(Invoice invoice);

    /**
     * Converts an InvoiceDto to an Invoice entity.
     *
     * @param invoiceDto The invoice DTO
     * @return The invoice entity
     */
    Invoice toEntity(InvoiceDto invoiceDto);
}
