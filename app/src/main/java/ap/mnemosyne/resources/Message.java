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

import com.fasterxml.jackson.annotation.JsonCreator;
import com.fasterxml.jackson.annotation.JsonProperty;
import com.fasterxml.jackson.annotation.JsonTypeName;
import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.ObjectMapper;

import java.io.*;
import java.nio.charset.StandardCharsets;

/**
 * Represents a message or an error message.
 * 
 * @author Nicola Ferro (ferro@dei.unipd.it)
 * @version 1.00
 * @since 1.00
 */

@JsonTypeName("message")
public class Message extends Resource {

	/**
	 * The message
	 */
	private final String message;

	/**
	 * The code of the error, if any
	 */
	private final String errorCode;

	/**
	 * Additional details about the error, if any
	 */
	private final String errorDetails;

	/**
	 * Creates an error message.
	 * 
	 * @param message
	 *            the message.
	 * @param errorCode
	 *            the code of the error.
	 * @param errorDetails
	 *            additional details about the error.
	 */
	@JsonCreator
	public Message(@JsonProperty("message") final String message, @JsonProperty("error-code") final String errorCode, @JsonProperty("error-error-details") final String errorDetails) {
		this.message = message;
		this.errorCode = errorCode;
		this.errorDetails = errorDetails;
	}


	/**
	 * Creates a generic message.
	 * 
	 * @param message
	 *            the message.
	 */
	public Message(final String message) {
		this.message = message;
		this.errorCode = null;
		this.errorDetails = null;
	}


	/**
	 * Returns the message.
	 * 
	 * @return the message.
	 */
	public final String getMessage() {
		return message;
	}

	/**
	 * Returns the code of the error, if any.
	 * 
	 * @return the code of the error, if any, {@code null} otherwise.
	 */
	public final String getErrorCode() {
		return errorCode;
	}
	
	/**
	 * Returns additional details about the error, if any.
	 * 
	 * @return additional details about the error, if any, {@code null} otherwise.
	 */
	public final String getErrorDetails() {
		return errorDetails;
	}

	@Override
	public final void toJSON(final OutputStream out) throws IOException {

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
