package nl.basjes.sunspec.schema.generate;

import com.ghgande.j2mod.modbus.facade.AbstractModbusMaster;
import com.ghgande.j2mod.modbus.facade.ModbusTCPMaster;
import nl.basjes.modbus.device.api.ModbusDevice;
import nl.basjes.modbus.device.exception.ModbusException;
import nl.basjes.modbus.device.j2mod.ModbusDeviceJ2Mod;
import nl.basjes.modbus.schema.SchemaDevice;
import nl.basjes.sunspec.device.SunspecDevice;
import org.apache.maven.plugin.AbstractMojo;
import org.apache.maven.plugin.MojoExecutionException;
import org.apache.maven.plugins.annotations.LifecyclePhase;
import org.apache.maven.plugins.annotations.Mojo;
import org.apache.maven.plugins.annotations.Parameter;

import java.io.File;
import java.io.FileOutputStream;

import static nl.basjes.modbus.schema.YamlLoaderKt.toYaml;
import static nl.basjes.sunspec.ConstantsKt.SUNSPEC_STANDARD_UNITID;

@Mojo(name = "generate", defaultPhase = LifecyclePhase.GENERATE_SOURCES)
public class GenerateMojo
    extends AbstractMojo {

    @Parameter(defaultValue = "${project.build.directory}/generated-sources/modbus-schema/", required = true)
    private File outputDirectory;

    @Parameter(property = "outputFile", required = true)
    private String outputFilename;

    @Parameter(property = "host", required = true)
    private String host;

    @Parameter(property = "port", required = true)
    private Integer port;

    @Parameter(property = "deviceDescription", required = true)
    private String deviceDescription;

    public void execute()
        throws MojoExecutionException {
        String outputDirectoryPath = outputDirectory.getAbsolutePath();

        if (!outputDirectory.exists()) {
            if (!outputDirectory.mkdirs()) {
                throw new MojoExecutionException("Cannot create directory " + outputDirectoryPath);
            }
        }

        try {
            String yaml = getSchema(host, port, deviceDescription);
            FileOutputStream outputStream =
                new FileOutputStream(buildFullFileName(
                    outputDirectory.getAbsolutePath(), outputFilename));
            outputStream.write(yaml.getBytes());
            outputStream.close();
        } catch (Exception e) {
            throw new MojoExecutionException(e);
        }
    }

    private static String buildFullFileName(String directory, String filename) {
        String result = directory.trim() + '/' + filename.trim();
        return result.replaceAll("/+", "/");
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
