package ap.mnemosyne.resources;

import ap.mnemosyne.enums.ParamsName;
import com.fasterxml.jackson.annotation.JsonProperty;
import com.fasterxml.jackson.annotation.JsonSubTypes;
import com.fasterxml.jackson.annotation.JsonTypeInfo;
import com.fasterxml.jackson.databind.ObjectMapper;

import java.io.*;
import java.nio.charset.Charset;
import java.nio.charset.StandardCharsets;

@JsonTypeInfo(use = JsonTypeInfo.Id.NAME,
		include = JsonTypeInfo.As.WRAPPER_OBJECT)
@JsonSubTypes({
		@JsonSubTypes.Type(value = TimeParameter.class),
		@JsonSubTypes.Type(value = LocationParameter.class)
})
public abstract class Parameter extends Resource
{
	private ParamsName name;
	private String userEmail;

	public Parameter(@JsonProperty("name") ParamsName name, @JsonProperty("type") String userEmail)
	{
		this.name = name;
		this.userEmail = userEmail;
	}

	public ParamsName getName()
	{
		return name;
	}

	public String getUserEmail()
	{
		return userEmail;
	}

	public static Parameter fromJSON(InputStream in) throws IOException
	{
		StringBuilder textBuilder = new StringBuilder();
		try (Reader reader = new BufferedReader(new InputStreamReader
				(in, Charset.forName(StandardCharsets.UTF_8.name())))) {
			int c = 0;
			while ((c = reader.read()) != -1) {
				textBuilder.append((char) c);
			}
		}
		ObjectMapper om = new ObjectMapper();
		om.findAndRegisterModules();
		Parameter p = om.readValue(textBuilder.toString(), Parameter.class);
		return p;
	}
}
