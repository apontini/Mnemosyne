package ap.mnemosyne.resources;

import com.fasterxml.jackson.annotation.JsonTypeName;
import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.ObjectMapper;
import org.geotools.geometry.jts.JTSFactoryFinder;
import org.locationtech.jts.geom.Coordinate;
import org.locationtech.jts.geom.GeometryFactory;

import java.io.*;
import java.nio.charset.StandardCharsets;
import java.util.Objects;

@JsonTypeName("point")
public class Point extends Resource implements Serializable
{
	private double lat;
	private double lon;

	public Point(){}

	public Point(double lat, double lon)
	{
		this.lat = lat;
		this.lon = lon;
	}

	public double getLat()
	{
		return lat;
	}

	public double getLon()
	{
		return lon;
	}

	public org.locationtech.jts.geom.Point toJTSPoint()
	{
		GeometryFactory builder = JTSFactoryFinder.getGeometryFactory();
		return builder.createPoint(new Coordinate(this.getLat(),this.getLon()));
	}

	@Override
	public String toString()
	{
		return "Point{" +
				"lat=" + lat +
				", lon=" + lon +
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
		Point point = (Point) o;
		return Double.compare(point.lat, lat) == 0 &&
				Double.compare(point.lon, lon) == 0;
	}

	@Override
	public int hashCode()
	{
		return Objects.hash(lat, lon);
	}

}
