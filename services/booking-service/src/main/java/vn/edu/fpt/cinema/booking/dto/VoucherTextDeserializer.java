package vn.edu.fpt.cinema.booking.dto;

import com.fasterxml.jackson.core.JsonParser;
import com.fasterxml.jackson.core.JsonToken;
import com.fasterxml.jackson.databind.DeserializationContext;
import com.fasterxml.jackson.databind.deser.std.StdDeserializer;
import java.io.IOException;

/** Jackson's scalar-coercion setting alone still allows numbers/booleans as String values. */
public class VoucherTextDeserializer extends StdDeserializer<String> {
    public VoucherTextDeserializer() { super(String.class); }

    @Override
    public String deserialize(JsonParser parser, DeserializationContext context) throws IOException {
        if (!parser.hasToken(JsonToken.VALUE_STRING)) {
            return context.reportInputMismatch(String.class, "Expected a JSON string");
        }
        return parser.getText();
    }
}
