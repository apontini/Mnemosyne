package ap.mnemosyne.resources;

import ap.mnemosyne.enums.ParamsName;
import com.fasterxml.jackson.annotation.JsonCreator;
import com.fasterxml.jackson.annotation.JsonProperty;
import com.fasterxml.jackson.annotation.JsonTypeName;
import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.ObjectMapper;

import java.io.*;
import java.nio.charset.StandardCharsets;

@JsonTypeName("location-parameter")
public class LocationParameter extends Parameter
{
	private Point location;
	private int cellID;
	private String SSID;

	@JsonCreator
	public LocationParameter(@JsonProperty("name") ParamsName name, @JsonProperty("user")String userEmail,
	                         @JsonProperty("location") Point location,
	                         @JsonProperty("cellID") int cellID, @JsonProperty("ssid") String SSID)
	{
		super(name, userEmail);
		this.location = location;
		this.cellID = cellID;
		this.SSID = SSID;
	}

	public Point getLocation()
	{
		return location;
	}

	public int getCellID()
	{
		return cellID;
	}

	public String getSSID()
	{
		return SSID;
	}

	@Override
	public final void toJSON(final OutputStream out) throws IOException
	{
		PrintWriter pw = new PrintWriter(new OutputStreamWriter(out, StandardCharsets.UTF_8));
		ObjectMapper om = new ObjectMapper();
		om.findAndRegisterModules();
		pw.print(om.writeValueAsString(this));
		pw.flush();
		pw.close();
	}

	@Override
	public final void toJSON(final PrintWriter pw) throws IOException
	{
		ObjectMapper om = new ObjectMapper();
		pw.print(om.writeValueAsString(this));
		pw.flush();
	}

	@Override
	public final String toJSON() throws JsonProcessingException
	{
		ObjectMapper om = new ObjectMapper();
		om.findAndRegisterModules();
		return om.writeValueAsString(this);
	}
}
