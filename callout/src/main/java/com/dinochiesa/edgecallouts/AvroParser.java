// AvroParser.java
//
// This is the source code for a Java callout for Apigee Edge.  This
// callout is very simple - it parses the inbound message, which is
// expected to be in Avro format, and produces a Java Map. It then sets
// that Java map into a context variable, and then returns SUCCESS.
//
// ------------------------------------------------------------------

package com.dinochiesa.edgecallouts;


import com.apigee.flow.execution.ExecutionContext;
import com.apigee.flow.execution.ExecutionResult;
import com.apigee.flow.execution.spi.Execution;
import com.apigee.flow.message.Message;
import com.apigee.flow.message.MessageContext;
import com.dinochiesa.util.AvroUtil;
import com.dinochiesa.util.TemplateString;
import com.fasterxml.jackson.databind.ObjectMapper;
import java.io.InputStreamReader;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import org.apache.avro.Schema;
import org.apache.avro.file.DataFileReader;
import org.apache.avro.file.FileReader;
import org.apache.avro.file.SeekableByteArrayInput;
import org.apache.avro.file.SeekableInput;
import org.apache.avro.generic.GenericData;
import org.apache.avro.generic.GenericDatumReader;
import org.apache.avro.generic.GenericRecord;
import org.apache.avro.io.DatumReader;
import org.apache.commons.io.IOUtils;
import org.apache.commons.lang.StringUtils;
import org.apache.commons.lang.exception.ExceptionUtils;
import org.apache.commons.lang.text.StrSubstitutor;


public class AvroParser implements Execution {
    private final static String varprefix= "avro_";

    private final ObjectMapper om = new ObjectMapper();

    private Map properties; // read-only

    public AvroParser(Map properties) {
        this.properties = properties;
    }

    private String getSchema(MessageContext msgCtxt) throws Exception {
        String schema = (String) this.properties.get("schema");
        if (schema == null || schema.equals("")) {
            throw new IllegalStateException("schema is null or empty.");
        }
        schema = resolvePropertyValue(schema, msgCtxt);
        if (schema == null || schema.equals("")) {
            throw new IllegalStateException("schema resolves to null or empty.");
        }
        return schema;
    }

    // If the value of a property value begins and ends with curlies,
    // eg, {apiproxy.name}, then "resolve" the value by de-referencing
    // the context variable whose name appears between the curlies.
    private String resolvePropertyValue(String spec, MessageContext msgCtxt) {
        if (spec.indexOf('{') > -1 && spec.indexOf('}')>-1) {
            // Replace ALL curly-braced items in the spec string with
            // the value of the corresponding context variable.
            TemplateString ts = new TemplateString(spec);
            Map<String,String> valuesMap = new HashMap<String,String>();
            for (String s : ts.variableNames) {
                valuesMap.put(s, (String) msgCtxt.getVariable(s));
            }
            StrSubstitutor sub = new StrSubstitutor(valuesMap);
            String resolvedString = sub.replace(ts.template);
            return resolvedString;
        }
        return spec;
    }


    public ExecutionResult execute (final MessageContext msgCtxt,
                                    final ExecutionContext execContext) {
        Message msg = msgCtxt.getMessage();
        String varName = null;
        try {

            // 1. read the schema from the configuration
            String schemaString = getSchema(msgCtxt);
            Schema schema = new Schema.Parser().parse(schemaString);
            GenericRecord rec;
            //GenericRecord rec = new GenericData.Record(schema);

            // 2. get the content as a SeekableInput
            // this loads the message content into an array, but unavoidable, as
            // Avro needs seekable input.
            SeekableInput input = new SeekableByteArrayInput(IOUtils.toByteArray(msg.getContentAsStream()));


            // 3. read the inbound payload and transform into a list of maps
            DatumReader<GenericRecord> datumReader = new GenericDatumReader<GenericRecord>(schema);

            FileReader<GenericRecord> fileReader = DataFileReader.openReader(input, datumReader);

            List<Map<String,Object>> list = new ArrayList<Map<String,Object>>();
            while (fileReader.hasNext()) {
                rec = fileReader.next();  // GenericRecord derives from HashMap
                System.out.println(rec);
                // convert the avro version of the event into a regular java Map
                Map<String, Object> map = new HashMap<String, Object>();
                AvroUtil.copyRecord(rec, map);
                list.add(map);
            }

            // 4. set a variable to hold the generated Map<String, Map>
            msgCtxt.setVariable(varprefix + "result_java", list);

            // 5. for diagnostic purposes, serialize to JSON as well
            String jsonResult = om.writer()
                .withDefaultPrettyPrinter()
                .writeValueAsString(list);
            msgCtxt.setVariable(varprefix + "result_json", jsonResult);
        }
        catch (java.lang.Exception exc1) {
            //exc1.printStackTrace(); // will go to stdout of message processor
            varName = varprefix + "error";
            msgCtxt.setVariable(varName, exc1.getMessage());
            varName = varprefix + "stacktrace";
            msgCtxt.setVariable(varName, ExceptionUtils.getStackTrace(exc1));
            return ExecutionResult.ABORT;
        }

        return ExecutionResult.SUCCESS;
    }
}
