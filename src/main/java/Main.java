import org.apache.mina.filter.codec.ProtocolCodecException;
import quickfix.ConfigError;
import quickfix.InvalidMessage;

/**
 * Created by bogdan on 17.07.18.
 */
public class Main {
    public static void main(String[] args) throws InvalidMessage, ConfigError, ProtocolCodecException {

        Analizator an = new Analizator();
    }
}
