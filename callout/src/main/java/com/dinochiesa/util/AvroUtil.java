package com.dinochiesa.util;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import org.apache.avro.Schema;
import org.apache.avro.generic.GenericArray;
import org.apache.avro.generic.GenericRecord;
import org.apache.avro.util.Utf8;

public class AvroUtil {

    public static Map<String, Object> copyRecord(GenericRecord avroRecord, Map<String, Object> record) {
        for (Schema.Field field : avroRecord.getSchema().getFields()) {
            String fieldName = field.name();
            Object value = avroRecord.get(fieldName);
            if (value == null) {
                continue; // it is expected that the key set will point to null
                          // values
            }

            if (value instanceof GenericRecord) {
                record.put(fieldName,
                           copyRecord((GenericRecord) value,
                                      new HashMap<String, Object>()));
            } else if (value instanceof GenericArray) {
                GenericArray avroArray = (GenericArray) value;
                List<Map<String, Object>> list = new ArrayList<Map<String, Object>>((int) avroArray.size());
                record.put(fieldName, list);
                AvroUtil.copyArray(avroArray, list);
            } else if (value instanceof Utf8) {
                record.put(fieldName, ((Utf8) value).toString());
            } else {
                record.put(fieldName, value);
            }
        }
        return record;
    }

    public static List<Map<String, Object>> copyArray(GenericArray<GenericRecord> avroArray, List<Map<String, Object>> list) {
        for (GenericRecord avroRecord : avroArray) {
            Map<String, Object> record = new HashMap<String, Object>();
            list.add(copyRecord(avroRecord, record));
        }
        return list;
    }
}
