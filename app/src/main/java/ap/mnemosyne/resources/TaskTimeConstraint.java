package ap.mnemosyne.resources;

import ap.mnemosyne.enums.ConstraintTemporalType;
import ap.mnemosyne.enums.ParamsName;
import com.fasterxml.jackson.annotation.JsonCreator;
import com.fasterxml.jackson.annotation.JsonProperty;
import com.fasterxml.jackson.annotation.JsonTypeName;
import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.ObjectMapper;
import org.joda.time.LocalTime;

import java.io.IOException;
import java.io.OutputStream;
import java.io.OutputStreamWriter;
import java.io.PrintWriter;
import java.nio.charset.StandardCharsets;

@JsonTypeName("task-time-constraint")
public class TaskTimeConstraint extends TaskConstraint
{
	private LocalTime fromTime, toTime;

	@JsonCreator
	public TaskTimeConstraint(@JsonProperty("from_time") LocalTime fromTime, @JsonProperty("to_time") LocalTime toTime ,
	                          @JsonProperty("param-name") ParamsName paramsName , @JsonProperty("type") ConstraintTemporalType type)
	{
		super(paramsName, type);
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
		om.findAndRegisterModules();
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
