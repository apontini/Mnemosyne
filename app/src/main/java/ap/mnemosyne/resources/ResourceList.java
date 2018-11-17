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

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;

import java.io.*;
import java.nio.charset.StandardCharsets;
import java.util.ArrayList;
import java.util.List;

public final class ResourceList<T extends Resource> extends Resource {

    private final Iterable<T> list;

    public ResourceList(final Iterable<T> list) {
        this.list = list;
    }

    @Override
    public final void toJSON(final OutputStream out) throws IOException
    {
        PrintWriter pw = new PrintWriter(new OutputStreamWriter(out, StandardCharsets.UTF_8));
        pw.print(this.toJSON());
        pw.flush();
        pw.close();
    }

    @Override
    public final void toJSON(final PrintWriter pw) throws IOException
    {
        pw.print(this.toJSON());
        pw.flush();
    }

    @Override
    public final String toJSON() throws IOException
    {
        //TODO: change with latest implementation
        String toRet="";
        toRet += ("{\"resource-list\":[");
        for(Resource r: list)
        {
            toRet += r.toJSON() + ",";
        }
        if(toRet.charAt(toRet.length()-1) == ',') toRet = toRet.substring(0,toRet.length()-1);
        toRet += "]}";
        return toRet;
    }

    public static final List<Resource> fromJSON(String json) throws IOException
    {
        List<Resource> toRet = new ArrayList<>();
        ObjectMapper map = new ObjectMapper();
        JsonNode obj = map.readTree(json).get("resource-list");

        for(int i=0; i<obj.size(); i++)
        {
            InputStream stream = new ByteArrayInputStream(obj.get(i).toString().getBytes(StandardCharsets.UTF_8));
            toRet.add(Resource.fromJSON(stream));
        }
        return toRet;
    }
}
