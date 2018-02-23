package com.hazelcast.jet.demo;

import com.hazelcast.jet.core.AbstractProcessor;
import com.hazelcast.jet.datamodel.TimestampedEntry;
import com.hazelcast.jet.pipeline.Sink;
import com.hazelcast.jet.pipeline.Sinks;
import java.io.OutputStream;
import java.net.Socket;
import java.nio.ByteBuffer;
import java.time.Instant;
import java.util.Map.Entry;
import org.python.core.PyFloat;
import org.python.core.PyInteger;
import org.python.core.PyList;
import org.python.core.PyString;
import org.python.core.PyTuple;
import org.python.modules.cPickle;

import static com.hazelcast.jet.core.ProcessorMetaSupplier.of;

/**
 * Sink implementation which forwards the items it receives to the Graphite.
 * Graphite's Pickle Protocol is used for communication between Jet and Graphite.
 */
public class GraphiteSink extends AbstractProcessor {

    private final String host;
    private final int port;
    private OutputStream outputStream;

    public GraphiteSink(String host, int port) {
        this.host = host;
        this.port = port;
    }

    @Override
    protected void init(Context context) throws Exception {
        Socket socket = new Socket(host, port);
        outputStream = socket.getOutputStream();
    }

    @Override
    protected boolean tryProcess(int ordinal, Object item) throws Exception {
        PyList list = new PyList();
        PyString metricName;
        PyInteger timestamp;
        PyFloat metricValue;
        if (item instanceof TimestampedEntry) {
            TimestampedEntry timestampedEntry = (TimestampedEntry) item;
            if (timestampedEntry.getKey() instanceof Long) {
                Entry<Long, Aircraft> aircraftEntry = (Entry<Long, Aircraft>) item;
                metricName = new PyString(aircraftEntry.getValue().getAirport().replace(" ", "_") + "." + aircraftEntry.getValue().verticalDirection.toString());
                timestamp = new PyInteger((int) Instant.ofEpochMilli(aircraftEntry.getValue().getPosTime()).getEpochSecond());
                metricValue = new PyFloat(1);
            } else {
                metricName = new PyString(((String) timestampedEntry.getKey()).replace(" ", "_"));
                timestamp = new PyInteger((int) Instant.ofEpochMilli(timestampedEntry.getTimestamp()).getEpochSecond());
                Object value = timestampedEntry.getValue();
                if (value instanceof Double) {
                    metricValue = new PyFloat((Double) value);
                } else {
                    metricValue = new PyFloat(((Entry<Aircraft, Integer>) value).getValue());
                }
            }
        } else {
            return true;
        }
        PyTuple metric = new PyTuple(metricName, new PyTuple(timestamp, metricValue));
        list.add(metric);

        PyString payload = cPickle.dumps(list, 2);
        byte[] header = ByteBuffer.allocate(4).putInt(payload.__len__()).array();

        outputStream.write(header);
        outputStream.write(payload.toBytes());
        outputStream.flush();
        return true;
    }

    @Override
    public boolean complete() {
        return false;
    }

    public static Sink sink(String host, int port) {
        return Sinks.fromProcessor("graphiteSink", of(() -> new GraphiteSink(host, port)));
    }
}
