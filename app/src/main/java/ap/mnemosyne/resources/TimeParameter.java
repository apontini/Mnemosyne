package ap.mnemosyne.resources;

import ap.mnemosyne.enums.ParamsName;
import com.fasterxml.jackson.annotation.JsonCreator;
import com.fasterxml.jackson.annotation.JsonProperty;
import com.fasterxml.jackson.annotation.JsonTypeName;
import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.ObjectMapper;
import org.joda.time.LocalTime;

import java.io.*;
import java.nio.charset.StandardCharsets;

@JsonTypeName("time-parameter")
public class TimeParameter extends Parameter
{
	private LocalTime fromTime;
	private LocalTime toTime;

	@JsonCreator
	public TimeParameter(@JsonProperty("name") ParamsName name, @JsonProperty("user") String userEmail,
	                     @JsonProperty("from-time") LocalTime fromTime, @JsonProperty("to-time") LocalTime toTime)
	{
		super(name, userEmail);
		this.fromTime = fromTime;
		this.toTime = toTime;
	}

	public LocalTime getFromTime()
	{
		return fromTime;
	}

	public LocalTime getToTime()
	{
		return toTime;
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
