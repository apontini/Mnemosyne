package ap.mnemosyne.resources;

import com.fasterxml.jackson.annotation.*;
import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.ObjectMapper;

import java.io.*;
import java.nio.charset.StandardCharsets;
import java.util.Set;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

@JsonTypeName("task")
public class Task extends Resource implements Serializable
{
	private int id;
	private String user;
	private String name;
	private TaskConstraint constr;
	private boolean possibleAtWork;
	private boolean repeatable;
	private boolean doneToday;
	private boolean failed;
	private Set<Place> placesToSatisfy;

	@JsonCreator
	public Task(@JsonProperty("id") int id,@JsonProperty("user") String user, @JsonProperty("name") String name, @JsonProperty("constr") TaskConstraint constr,
	            @JsonProperty("possibleAtWork") boolean possibleAtWork, @JsonProperty("repeatable") boolean repeatable, @JsonProperty("doneToday") boolean doneToday,
	            @JsonProperty("failed") boolean failed, @JsonProperty("placesToSatisfy") Set<Place> placesToSatisfy)
	{
		this.user = user;
		this.name = name;
		this.constr = constr;
		this.id = id;
		this.possibleAtWork = possibleAtWork;
		this.repeatable = repeatable;
		this.doneToday = doneToday;
		this.failed = failed;
		this.placesToSatisfy = placesToSatisfy;
	}

	public String getName()
	{
		return name;
	}

	public String getUser()
	{
		return user;
	}

	public TaskConstraint getConstr()
	{
		return constr;
	}

	public int getId()
	{
		return id;
	}

	public boolean isPossibleAtWork()
	{
		return possibleAtWork;
	}

	public boolean isRepeatable()
	{
		return repeatable;
	}

	public boolean isDoneToday()
	{
		return doneToday;
	}

	public boolean isFailed()
	{
		return failed;
	}

	public Set<Place> getPlacesToSatisfy()
	{
		return placesToSatisfy;
	}

	@Override
	public String toString()
	{
		return "Task{" +
				"name='" + name + '\'' +
				", constr=" + constr +
				", id=" + id +
				", possibleAtWork=" + possibleAtWork +
				", repeatable=" + repeatable +
				", doneToday=" + doneToday +
				", failed=" + failed +
				", placesToSatisfy=" + placesToSatisfy +
				'}';
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

	public static final Task fromJSON(InputStream in) throws IOException
	{
		String regex = "\\{\"task\":(\\{.*\\})\\}";
		StringBuilder textBuilder = new StringBuilder();
		Reader reader = new BufferedReader(new InputStreamReader(in));
		int c = 0;
		while ((c = reader.read()) != -1)
		{
			textBuilder.append((char) c);
		}

		if(!textBuilder.toString().matches(regex)) throw new IOException("No Task object found (needed: " + regex + ", found: " + textBuilder.toString() + ")");

		Pattern pattern = Pattern.compile(regex);
		Matcher matcher = pattern.matcher(textBuilder.toString());
		matcher.find();

		ObjectMapper om = new ObjectMapper();
		om.findAndRegisterModules();
		Task t = om.readValue(matcher.group(0), Task.class);
		return t;
	}
}
