package com.betleopard;

import com.fasterxml.jackson.core.JsonGenerator;
import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.JsonSerializer;
import com.fasterxml.jackson.databind.SerializerProvider;
import com.betleopard.domain.Leg;
import com.betleopard.domain.OddsType;
import java.io.IOException;

/**
 * Serializer class for bet legs. To work around problems with serialization
 * of optional fields
 *
 * @author kittylyst
 */
public class CustomLegSerializer extends JsonSerializer<Leg> {

    /**
     * Perform custom JSON serialization
     * 
     * @param leg                      the bet leg to be serialized
     * @param jgen                     needed to conform to Jackson interface
     * @param sp                       needed to conform to Jackson interface
     * @throws IOException             if any fields could not be written
     * @throws JsonProcessingException if the JSON could not be produced
     */
    @Override
    public void serialize(final Leg leg, final JsonGenerator jgen, final SerializerProvider sp) throws IOException, JsonProcessingException {
        jgen.writeStartObject();
        jgen.writeNumberField("race", leg.getRace().getID());
        jgen.writeNumberField("backing", leg.getBacking().getID());
        jgen.writeNumberField("oddsVersion", leg.getOddsVersion());
        final OddsType ot = leg.getoType();
        jgen.writeStringField("oddsType", "" + ot);
        if (ot == OddsType.FIXED_ODDS) {
            jgen.writeNumberField("odds", leg.odds());
        }
        if (leg.hasStake()) {
            jgen.writeNumberField("stake", leg.stake());
        }
        jgen.writeEndObject();
    }

    /**
     * Returns the type that this serializer will accept
     * 
     * @return the accepted type
     */
    @Override
    public Class<Leg> handledType() {
        return Leg.class;
    }
}
