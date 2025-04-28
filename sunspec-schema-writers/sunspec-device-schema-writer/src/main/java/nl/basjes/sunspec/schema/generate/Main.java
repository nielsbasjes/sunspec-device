package nl.basjes.sunspec.schema.generate;

import com.ghgande.j2mod.modbus.facade.AbstractModbusMaster;
import com.ghgande.j2mod.modbus.facade.ModbusTCPMaster;
import nl.basjes.modbus.device.api.ModbusDevice;
import nl.basjes.modbus.device.exception.ModbusException;
import nl.basjes.modbus.device.j2mod.ModbusDeviceJ2Mod;
import nl.basjes.modbus.schema.SchemaDevice;
import nl.basjes.sunspec.device.SunspecDevice;
import picocli.CommandLine;
import picocli.CommandLine.Command;
import picocli.CommandLine.Model.CommandSpec;
import picocli.CommandLine.Option;
import picocli.CommandLine.Spec;

import java.util.concurrent.Callable;

import static nl.basjes.modbus.schema.YamlLoaderKt.toYaml;
import static nl.basjes.sunspec.ConstantsKt.SUNSPEC_STANDARD_UNITID;
import static picocli.CommandLine.Help.Visibility.ON_DEMAND;

@Command(name = "SunSpecDump", version = "1.0", mixinStandardHelpOptions = true)
public class Main implements Callable<Integer> {

    @Option(names = {"-ip", "--ipaddress"}, description = "The hostname/IP address of the modbus device", required = true)
    private String ipaddress;

    @Option(names = {"-p", "--port"}, description = "Use modbus port", showDefaultValue = ON_DEMAND, defaultValue = "502")
    private int port;

    @Option(names = {"-desc", "--description"}, description = "The description of this device in the generated Schema file", showDefaultValue = ON_DEMAND, defaultValue = "A SunSpec Device")
    private String deviceDescription;

    @Spec
    private CommandSpec spec;

    public static void main(String[] args) {
        System.exit(new CommandLine(new Main()).execute(args));
    }

    @Override
    public Integer call() throws Exception {
        try (ModbusDevice modbusDevice = getModbusDevice(ipaddress, port)) {
            String schema = getSchema(modbusDevice, deviceDescription);

            System.out.println("#-------------- BEGIN GENERATED CODE SNIPPET --------------");
            System.out.println(schema);
            System.out.println("#-------------- END GENERATED CODE SNIPPET --------------");
        }
        return 0;
    }

    public static String getSchema(String host, int port, String description) throws Exception {
        return getSchema(getModbusDevice(host, port), description);
    }

    /**
     * Load the data from a real device using the provided ipaddress
     */
    public static ModbusDevice getModbusDevice(String host, int port) throws Exception {
        AbstractModbusMaster master = new ModbusTCPMaster(host, port);
        master.connect();
        System.out.println("Connected.");
        return new ModbusDeviceJ2Mod(master, SUNSPEC_STANDARD_UNITID);
    }

    public static String getSchema(ModbusDevice modbusDevice, String description) throws ModbusException {
        SchemaDevice schemaDevice = SunspecDevice.generate(modbusDevice, description);
        if (schemaDevice == null) {
            throw new ModbusException("Unable to obtain the schema");
        }
        return toYaml(schemaDevice);
    }

}
