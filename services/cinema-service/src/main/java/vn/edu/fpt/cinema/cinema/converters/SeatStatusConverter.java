package vn.edu.fpt.cinema.cinema.converters;

import jakarta.persistence.AttributeConverter;
import jakarta.persistence.Converter;
import vn.edu.fpt.cinema.cinema.enums.SeatStatus;

@Converter(autoApply = true)
public class SeatStatusConverter implements AttributeConverter<SeatStatus, String> {

    @Override
    public String convertToDatabaseColumn(SeatStatus attribute) {
        if (attribute == null) return null;
        return attribute.name();
    }

    @Override
    public SeatStatus convertToEntityAttribute(String dbData) {
        if (dbData == null || dbData.isBlank()) return null;
        try {
            return SeatStatus.valueOf(dbData.trim().toUpperCase());
        } catch (IllegalArgumentException e) {
            return SeatStatus.AVAILABLE;
        }
    }
}

