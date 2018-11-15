/*
 * Copyright 2018 University of Padua, Italy
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *     http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */

package ap.mnemosyne.resources;

import com.fasterxml.jackson.annotation.JsonSubTypes;
import com.fasterxml.jackson.annotation.JsonTypeInfo;
import com.fasterxml.jackson.databind.ObjectMapper;

import java.io.*;
import java.nio.charset.Charset;
import java.nio.charset.StandardCharsets;

@JsonTypeInfo(use = JsonTypeInfo.Id.NAME,
		include = JsonTypeInfo.As.WRAPPER_OBJECT)
@JsonSubTypes({
		@JsonSubTypes.Type(value = Task.class),
		@JsonSubTypes.Type(value = User.class),
		@JsonSubTypes.Type(value = Message.class),
		@JsonSubTypes.Type(value = TaskConstraint.class),
		@JsonSubTypes.Type(value = Point.class),
		@JsonSubTypes.Type(value = Place.class),
		@JsonSubTypes.Type(value = Hint.class),
		@JsonSubTypes.Type(value = Parameter.class)
})
public abstract class Resource
{

	public Resource() {}

	public abstract void toJSON(final OutputStream out) throws IOException;

	public abstract String toJSON() throws IOException;

	public abstract void toJSON(final PrintWriter pw) throws IOException;

	public static Resource fromJSON(InputStream in) throws IOException
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
		Resource p = om.readValue(textBuilder.toString(), Resource.class);
		return p;
	}
}
