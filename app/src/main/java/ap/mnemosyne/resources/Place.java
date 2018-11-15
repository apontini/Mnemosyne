package ap.mnemosyne.resources;

import com.fasterxml.jackson.annotation.JsonCreator;
import com.fasterxml.jackson.annotation.JsonProperty;
import com.fasterxml.jackson.annotation.JsonTypeName;
import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.ObjectMapper;
import org.joda.time.LocalTime;

import java.io.*;
import java.nio.charset.StandardCharsets;
import java.util.Objects;

@JsonTypeName("place")
public class Place extends Resource implements Serializable
{
	private String country;
	private String state;
	private String town;
	private String suburb;
	private int houseNumber;
	private String name;
	private String placeType;
	private Point coordinates;
	private LocalTime opening;
	private LocalTime closing;

	@JsonCreator
	public Place(@JsonProperty("country") String country, @JsonProperty("state") String state, @JsonProperty("town") String town, @JsonProperty("suburb") String suburb,
	             @JsonProperty("house-number") int houseNumber, @JsonProperty("name") String name, @JsonProperty("place-type") String placeType,
	             @JsonProperty("coordinates") Point coordinates, @JsonProperty("opening") LocalTime opening, @JsonProperty("closing") LocalTime closing)
	{
		this.country = country;
		this.state = state;
		this.town = town;
		this.suburb = suburb;
		this.houseNumber = houseNumber;
		this.name = name;
		this.placeType = placeType;
		this.coordinates = coordinates;
		this.opening = opening;
		this.closing = closing;
	}

	public String getCountry()
	{
		return country;
	}

	public String getState()
	{
		return state;
	}

	public String getTown()
	{
		return town;
	}

	public String getSuburb()
	{
		return suburb;
	}

	public int getHouseNumber()
	{
		return houseNumber;
	}

	public String getName()
	{
		return name;
	}

	public String getPlaceType()
	{
		return placeType;
	}

	public Point getCoordinates()
	{
		return coordinates;
	}

	public LocalTime getOpening()
	{
		return opening;
	}

	public LocalTime getClosing()
	{
		return closing;
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

	@Override
	public boolean equals(Object o)
	{
		if (this == o) return true;
		if (o == null || getClass() != o.getClass()) return false;
		Place place = (Place) o;
		return Objects.equals(coordinates, place.coordinates);
	}

	@Override
	public int hashCode()
	{
		return Objects.hash(coordinates);
	}

	@Override
	public String toString()
	{
		return "Place{" +
				"country='" + country + '\'' +
				", state='" + state + '\'' +
				", town='" + town + '\'' +
				", suburb='" + suburb + '\'' +
				", houseNumber=" + houseNumber +
				", name='" + name + '\'' +
				", placeType='" + placeType + '\'' +
				", coordinates=" + coordinates +
				", opening=" + opening +
				", closing=" + closing +
				'}';
	}

}
