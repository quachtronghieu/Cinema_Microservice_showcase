package vn.edu.fpt.cinema.cinema.converters;

import jakarta.persistence.AttributeConverter;
import jakarta.persistence.Converter;
import vn.edu.fpt.cinema.cinema.enums.SeatType;

@Converter(autoApply = true)
public class SeatTypeConverter implements AttributeConverter<SeatType, String> {

    @Override
    public String convertToDatabaseColumn(SeatType attribute) {
        if (attribute == null) return null;
        return attribute.name();
    }

    @Override
    public SeatType convertToEntityAttribute(String dbData) {
        if (dbData == null || dbData.isBlank()) return null;
        try {
            return SeatType.valueOf(dbData.trim().toUpperCase());
        } catch (IllegalArgumentException e) {
            return SeatType.STANDARD;
        }
    }
}

