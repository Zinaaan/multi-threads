package unsafeBuffer;

import org.agrona.DirectBuffer;
import org.agrona.concurrent.UnsafeBuffer;

import java.nio.ByteBuffer;

/**
 * @author lzn
 * @date 2023/10/14 16:55
 * @description
 */
public class MetricBuffer extends UnsafeBuffer {

    public MetricBuffer() {
        super();
    }

    public MetricBuffer(byte[] buffer) {
        super(buffer);
    }

    public MetricBuffer(byte[] buffer, int offset, int length) {
        super(buffer, offset, length);
    }

    public MetricBuffer(ByteBuffer buffer) {
        super(buffer);
    }

    public MetricBuffer(ByteBuffer buffer, int offset, int length) {
        super(buffer, offset, length);
    }

    public MetricBuffer(DirectBuffer buffer) {
        super(buffer);
    }

    public MetricBuffer(DirectBuffer buffer, int offset, int length) {
        super(buffer, offset, length);
    }

    public MetricBuffer(long address, int length) {
        super(address, length);
    }

    public int getClientId(){
        return getInt(0);
    }

    public int getPoint(){
        return getInt(4);
    }

    public long getLatency(){
        return getLong(8);
    }

    @Override
    public boolean equals(Object obj) {
        if(this == obj){
            return true;
        }
        if(getClass() != obj.getClass()){
            return false;
        }

        MetricBuffer that = (MetricBuffer) obj;
        return getClientId() == that.getClientId() && getPoint() == that.getPoint();
    }

    @Override
    public int hashCode() {
        return Integer.hashCode(getClientId()) + Integer.hashCode(getPoint());
    }
}
