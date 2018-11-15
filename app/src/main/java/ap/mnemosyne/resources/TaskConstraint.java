package ap.mnemosyne.resources;

import ap.mnemosyne.enums.ConstraintTemporalType;
import ap.mnemosyne.enums.ParamsName;
import com.fasterxml.jackson.annotation.JsonProperty;
import com.fasterxml.jackson.annotation.JsonSubTypes;
import com.fasterxml.jackson.annotation.JsonTypeInfo;

import java.io.Serializable;

@JsonTypeInfo(use = JsonTypeInfo.Id.NAME,
		include = JsonTypeInfo.As.WRAPPER_OBJECT)
@JsonSubTypes({
		@JsonSubTypes.Type(value = TaskPlaceConstraint.class),
		@JsonSubTypes.Type(value = TaskTimeConstraint.class)
})
public abstract class TaskConstraint extends Resource implements Serializable
{
	private ParamsName paramName;
	private ConstraintTemporalType type;

	public TaskConstraint(@JsonProperty("param-name") ParamsName paramName,  @JsonProperty("type") ConstraintTemporalType type)
	{
		this.paramName = paramName;
		this.type = type;
	}

	public ParamsName getParamName()
	{
		return paramName;
	}

	public ConstraintTemporalType getType()
	{
		return type;
	}

}
