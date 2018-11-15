package ap.mnemosyne.resources;

import ap.mnemosyne.enums.ConstraintTemporalType;
import ap.mnemosyne.enums.NormalizedActions;
import ap.mnemosyne.enums.ParamsName;
import com.fasterxml.jackson.annotation.JsonCreator;
import com.fasterxml.jackson.annotation.JsonProperty;
import com.fasterxml.jackson.annotation.JsonTypeName;
import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.ObjectMapper;

import java.io.IOException;
import java.io.OutputStream;
import java.io.OutputStreamWriter;
import java.io.PrintWriter;
import java.nio.charset.StandardCharsets;

@JsonTypeName("task-place-constraint")
public class TaskPlaceConstraint extends TaskConstraint
{
	private Place constraintPlace;
	private NormalizedActions normalizedAction;

	@JsonCreator
	public TaskPlaceConstraint(@JsonProperty("constraint-place") Place constraintPlace, @JsonProperty("param-name") ParamsName paramName ,
	                           @JsonProperty("temporal-type") ConstraintTemporalType type, @JsonProperty("normalized-action") NormalizedActions normalizedAction)
	{
		super(paramName, type);
		this.constraintPlace = constraintPlace;
		this.normalizedAction = normalizedAction;
	}

	public Place getConstraintPlace()
	{
		return constraintPlace;
	}

	public NormalizedActions getNormalizedAction()
	{
		return normalizedAction;
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
