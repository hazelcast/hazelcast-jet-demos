package com.hazelcast.jet.demo;

import com.hazelcast.com.eclipsesource.json.JsonObject;
import com.hazelcast.internal.management.JsonSerializable;
import com.hazelcast.jet.demo.types.AltitudeType;
import com.hazelcast.jet.demo.types.EngineMount;
import com.hazelcast.jet.demo.types.EngineTypes;
import com.hazelcast.jet.demo.types.Species;
import com.hazelcast.jet.demo.types.SpeedType;
import com.hazelcast.jet.demo.types.TransponderType;
import com.hazelcast.jet.demo.types.VerticalSpeedType;
import com.hazelcast.jet.demo.types.WakeTurbulanceCategory;
import com.hazelcast.jet.impl.util.Util;

import java.io.Serializable;
import java.util.Arrays;
import java.util.List;

import static com.hazelcast.jet.demo.util.Util.asBoolean;
import static com.hazelcast.jet.demo.util.Util.asDouble;
import static com.hazelcast.jet.demo.util.Util.asDoubleList;
import static com.hazelcast.jet.demo.util.Util.asFloat;
import static com.hazelcast.jet.demo.util.Util.asInt;
import static com.hazelcast.jet.demo.util.Util.asLong;
import static com.hazelcast.jet.demo.util.Util.asString;
import static com.hazelcast.jet.demo.util.Util.asStringArray;

public class Aircraft implements JsonSerializable, Serializable {

    enum VerticalDirection {
        UNKNOWN,
        CRUISE,
        ASCENDING,
        DESCENDING
    }

    /**
     * The unique identifier of the aircraft.
     */
    long id;
    /**
     * The number of seconds that the aircraft has been tracked for.
     */
    long tsecs;
    long rcvr;
    String icao;
    boolean bad;
    String reg;
    long alt;
    long galt;
    double inhg;
    AltitudeType altt;
    long talt;
    String call;
    boolean callSus;
    float lat;
    float lon;
    long posTime;
    boolean mLat;
    boolean posStale;
    boolean isTisb;
    float spd;
    SpeedType spdType;
    long vsi;
    VerticalSpeedType vsiType;
    double trak;
    boolean trkH;
    double tTrk;
    String type;
    String mdl;
    String man;
    String cNum;
    String from;
    String to;
    String[] stops;
    String op;
    String opCode;
    int sqk;
    boolean help;
    double dst;
    double brng;
    WakeTurbulanceCategory wtc;
    String engines;
    EngineTypes engtype;
    EngineMount engMount;
    Species species;
    boolean mil;
    String cou;
    boolean hasPic;
    long flightsCount;
    long cMessages;
    boolean gnd;
    boolean interested;
    String tt;
    TransponderType trt;
    String year;
    List<Double> cos;
    List<Double> cot;
    boolean resetTrail;
    boolean hasSig;
    long sig;
    String airport;
    VerticalDirection verticalDirection = VerticalDirection.UNKNOWN;


    public long getId() {
        return id;
    }

    public long getTsecs() {
        return tsecs;
    }

    public long getRcvr() {
        return rcvr;
    }

    public String getIcao() {
        return icao;
    }

    public boolean isBad() {
        return bad;
    }

    public String getReg() {
        return reg;
    }

    public long getAlt() {
        return alt;
    }

    public long getGalt() {
        return galt;
    }

    public double getInhg() {
        return inhg;
    }

    public AltitudeType getAltt() {
        return altt;
    }

    public long getTalt() {
        return talt;
    }

    public String getCall() {
        return call;
    }

    public boolean isCallSus() {
        return callSus;
    }

    public float getLat() {
        return lat;
    }

    public float getLon() {
        return lon;
    }

    public long getPosTime() {
        return posTime;
    }

    public boolean ismLat() {
        return mLat;
    }

    public boolean isPosStale() {
        return posStale;
    }

    public boolean isTisb() {
        return isTisb;
    }

    public float getSpd() {
        return spd;
    }

    public SpeedType getSpdType() {
        return spdType;
    }

    public long getVsi() {
        return vsi;
    }

    public VerticalSpeedType getVsiType() {
        return vsiType;
    }

    public double getTrak() {
        return trak;
    }

    public boolean isTrkH() {
        return trkH;
    }

    public double gettTrk() {
        return tTrk;
    }

    public String getType() {
        return type;
    }

    public String getMdl() {
        return mdl;
    }

    public String getMan() {
        return man;
    }

    public String getcNum() {
        return cNum;
    }

    public String getFrom() {
        return from;
    }

    public String getTo() {
        return to;
    }

    public String[] getStops() {
        return stops;
    }

    public String getOp() {
        return op;
    }

    public String getOpCode() {
        return opCode;
    }

    public int getSqk() {
        return sqk;
    }

    public boolean isHelp() {
        return help;
    }

    public double getDst() {
        return dst;
    }

    public double getBrng() {
        return brng;
    }

    public WakeTurbulanceCategory getWtc() {
        return wtc;
    }

    public String getEngines() {
        return engines;
    }

    public EngineTypes getEngtype() {
        return engtype;
    }

    public EngineMount getEngMount() {
        return engMount;
    }

    public Species getSpecies() {
        return species;
    }

    public boolean isMil() {
        return mil;
    }

    public String getCou() {
        return cou;
    }

    public boolean isHasPic() {
        return hasPic;
    }

    public long getFlightsCount() {
        return flightsCount;
    }

    public long getcMessages() {
        return cMessages;
    }

    public boolean isGnd() {
        return gnd;
    }

    public boolean isInterested() {
        return interested;
    }

    public String getTt() {
        return tt;
    }

    public TransponderType getTrt() {
        return trt;
    }

    public String getYear() {
        return year;
    }

    public List<Double> getCos() {
        return cos;
    }

    public List<Double> getCot() {
        return cot;
    }

    public boolean isResetTrail() {
        return resetTrail;
    }

    public boolean isHasSig() {
        return hasSig;
    }

    public long getSig() {
        return sig;
    }

    public String getAirport() {
        return airport;
    }

    public void setAirport(String airport) {
        this.airport = airport;
    }

    public VerticalDirection getVerticalDirection() {
        return verticalDirection;
    }

    public void setVerticalDirection(VerticalDirection verticalDirection) {
        this.verticalDirection = verticalDirection;
    }

    @Override
    public JsonObject toJson() {
        throw new UnsupportedOperationException();
    }

    @Override
    public void fromJson(JsonObject json) {
        id = asLong(json.get("Id"));
        tsecs = asLong(json.get("TSecs"));
        rcvr = asLong(json.get("Rcvr"));
        icao = asString(json.get("Icao"));
        bad = asBoolean(json.get("Bad"));
        reg = asString(json.get("Reg"));
        alt = asLong(json.get("Alt"));
        galt = asLong(json.get("GAlt"));
        inhg = asDouble(json.get("InHg"));
        altt = AltitudeType.fromId(asInt(json.get("AltT")));
        talt = asLong(json.get("TAlt"));
        call = asString(json.get("Call"));
        callSus = asBoolean(json.get("CallSus"));
        lat = asFloat(json.get("Lat"));
        lon = asFloat(json.get("Long"));
        posTime = asLong(json.get("PosTime"));
        mLat = asBoolean(json.get("Mlat"));
        posStale = asBoolean(json.get("PosStale"));
        isTisb = asBoolean(json.get("IsTisb"));
        spd = asFloat(json.get("Spd"));
        spdType = SpeedType.fromId(asInt(json.get("SpdTyp")));
        vsi = asLong(json.get("Vsi"));
        vsiType = VerticalSpeedType.fromId(asInt(json.get("VsiT")));
        trak = asDouble(json.get("Trak"));
        trkH = asBoolean(json.get("TrkH"));
        tTrk = asDouble(json.get("TTrk"));
        type = asString(json.get("Type"));
        mdl = asString(json.get("Mdl"));
        man = asString(json.get("Man"));
        cNum = asString(json.get("CNum"));
        from = asString(json.get("From"));
        to = asString(json.get("To"));
        stops = asStringArray(json.get("Stops"));
        op = asString(json.get("Op"));
        opCode = asString(json.get("OpCode"));
        sqk = asInt(json.get("Sqk"));
        help = asBoolean(json.get("Help"));
        dst = asDouble(json.get("Dst"));
        brng = asDouble(json.get("Brng"));
        wtc = WakeTurbulanceCategory.fromId(asInt(json.get("WTC")));
        engines = asString(json.get("Engines"));
        engtype = EngineTypes.fromId(asInt(json.get("EngType")));
        engMount = EngineMount.fromId(asInt(json.get("EngMount")));
        species = Species.fromId(asInt(json.get("Species")));
        mil = asBoolean(json.get("Mil"));
        cou = asString(json.get("Cou"));
        hasPic = asBoolean(json.get("hasPic"));
        flightsCount = asLong(json.get("FlightsCount"));
        cMessages = asLong(json.get("CMsgs"));
        gnd = asBoolean(json.get("Gnd"));
        interested = asBoolean(json.get("Interested"));
        tt = asString(json.get("TT"));
        trt = TransponderType.fromId(asInt(json.get("Trt")));
        year = asString(json.get("Year"));
        cos = asDoubleList(json.get("Cos"));
        cot = asDoubleList(json.get("Cot"));
        resetTrail = asBoolean(json.get("ResetTrail"));
        hasSig = asBoolean(json.get("HasSig"));
        sig = asLong(json.get("Sig"));
    }

    @Override
    public String toString() {
        return "Aircraft{" +
                "reg='" + reg + '\'' +
                ", alt=" + alt +
                ", coord=" + String.format("%.2f,%.2f", lat, lon) +
                ", mdl='" + mdl + '\'' +
                ", city='" + airport + '\'' +
                ", posTime=" + Util.toLocalDateTime(posTime).toLocalTime() +
                '}';
    }

    public String fullString() {
        return "Aircraft{" +
                "id=" + id +
                ", tsecs=" + tsecs +
                ", rcvr=" + rcvr +
                ", icao='" + icao + '\'' +
                ", bad=" + bad +
                ", reg='" + reg + '\'' +
                ", alt=" + alt +
                ", galt=" + galt +
                ", inhg=" + inhg +
                ", altt=" + altt +
                ", talt=" + talt +
                ", call='" + call + '\'' +
                ", callSus=" + callSus +
                ", lat=" + lat +
                ", lon=" + lon +
                ", city=" + airport +
                ", posTime=" + Util.toLocalDateTime(posTime) +
                ", posTimeTs=" + posTime +
                ", mLat=" + mLat +
                ", posStale=" + posStale +
                ", isTisb=" + isTisb +
                ", spd=" + spd +
                ", spdType=" + spdType +
                ", vsi=" + vsi +
                ", vsiType=" + vsiType +
                ", trak=" + trak +
                ", trkH=" + trkH +
                ", tTrk=" + tTrk +
                ", type='" + type + '\'' +
                ", mdl='" + mdl + '\'' +
                ", man='" + man + '\'' +
                ", cNum='" + cNum + '\'' +
                ", from='" + from + '\'' +
                ", to='" + to + '\'' +
                ", stops=" + Arrays.toString(stops) +
                ", op='" + op + '\'' +
                ", opCode='" + opCode + '\'' +
                ", sqk=" + sqk +
                ", help=" + help +
                ", dst=" + dst +
                ", brng=" + brng +
                ", wtc=" + wtc +
                ", engines='" + engines + '\'' +
                ", engtype=" + engtype +
                ", engMount=" + engMount +
                ", species=" + species +
                ", mil=" + mil +
                ", cou='" + cou + '\'' +
                ", hasPic=" + hasPic +
                ", flightsCount=" + flightsCount +
                ", cMessages=" + cMessages +
                ", gnd=" + gnd +
                ", interested=" + interested +
                ", tt='" + tt + '\'' +
                ", trt=" + trt +
                ", year='" + year + '\'' +
                ", cos=" + cos +
                ", cot=" + cot +
                ", resetTrail=" + resetTrail +
                ", hasSig=" + hasSig +
                ", sig=" + sig +
                '}';
    }
}
