/**
 * Licensed to the Apache Software Foundation (ASF) under one
 * or more contributor license agreements.  See the NOTICE file
 * distributed with this work for additional information
 * regarding copyright ownership.  The ASF licenses this file
 * to you under the Apache License, Version 2.0 (the
 * "License"); you may not use this file except in compliance
 * with the License.  You may obtain a copy of the License at
 *
 *     http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */
package org.apache.sqoop.json.util;

import org.apache.sqoop.schema.NullSchema;
import org.apache.sqoop.schema.Schema;
import org.apache.sqoop.schema.type.AbstractComplexListType;
import org.apache.sqoop.schema.type.AbstractPrimitiveType;
import org.apache.sqoop.schema.type.AbstractString;
import org.apache.sqoop.schema.type.Array;
import org.apache.sqoop.schema.type.Binary;
import org.apache.sqoop.schema.type.Bit;
import org.apache.sqoop.schema.type.Column;
import org.apache.sqoop.schema.type.ColumnType;
import org.apache.sqoop.schema.type.Date;
import org.apache.sqoop.schema.type.DateTime;
import org.apache.sqoop.schema.type.Decimal;
import org.apache.sqoop.schema.type.Enum;
import org.apache.sqoop.schema.type.FixedPoint;
import org.apache.sqoop.schema.type.FloatingPoint;
import org.apache.sqoop.schema.type.Map;
import org.apache.sqoop.schema.type.Set;
import org.apache.sqoop.schema.type.Text;
import org.apache.sqoop.schema.type.Time;
import org.apache.sqoop.schema.type.Unknown;
import org.json.simple.JSONArray;
import org.json.simple.JSONObject;

/**
 *
 */
public class SchemaSerialization {

  // common attributes of all column types
  private static final String NAME = "name";
  private static final String CREATION_DATE = "created";
  private static final String NOTE = "note";
  private static final String COLUMNS = "columns";
  private static final String TYPE = "type";
  private static final String NULLABLE = "nullable";
  // size attribute is relevant to String and Array type only
  private static final String SIZE = "size";
  // maps and enum attributes
  private static final String MAP = "map";
  private static final String KEY = "key";
  private static final String VALUE = "value";
  // arrays and set attribute
  private static final String LIST = "list";
  private static final String LIST_TYPE = "listType";
  // number attribute
  private static final String BYTE_SIZE = "byteSize";
  // string attribute
  private static final String CHAR_SIZE = "charSize";

  private static final String FRACTION = "fraction";
  private static final String TIMEZONE = "timezone";
  private static final String PRECISION = "precision";
  private static final String SCALE = "scale";
  private static final String UNSIGNED = "unsigned";
  private static final String JDBC_TYPE = "jdbc-type";

  @SuppressWarnings("unchecked")
  public static JSONObject extractSchema(Schema schema) {
    JSONObject object = new JSONObject();
    // just a defensive check
    if (schema != null) {
      object.put(NAME, schema.getName());
      object.put(CREATION_DATE, schema.getCreationDate().getTime());
      if (schema.getNote() != null) {
        object.put(NOTE, schema.getNote());
      }
      JSONArray columnArray = new JSONArray();
      for (Column column : schema.getColumns()) {
        columnArray.add(extractColumn(column));
      }
      object.put(COLUMNS, columnArray);
    }
    return object;
  }

  public static Schema restoreSchema(JSONObject jsonObject) {
    // if the object is empty return a empty schema
    if (jsonObject == null || jsonObject.isEmpty()) {
      return NullSchema.getInstance();
    }
    String name = (String) jsonObject.get(NAME);
    String note = (String) jsonObject.get(NOTE);
    java.util.Date date = new java.util.Date((Long) jsonObject.get(CREATION_DATE));

    Schema schema = new Schema(name).setNote(note).setCreationDate(date);
    JSONArray columnsArray = (JSONArray) jsonObject.get(COLUMNS);
    for (Object obj : columnsArray) {
      schema.addColumn(restoreColumn((JSONObject) obj));
    }
    return schema;
  }

  @SuppressWarnings("unchecked")
  private static JSONObject extractColumn(Column column) {
    JSONObject ret = new JSONObject();

    ret.put(NAME, column.getName());
    ret.put(NULLABLE, column.getNullable());
    ret.put(TYPE, column.getType().name());

    switch (column.getType()) {
    case MAP:
      JSONObject map = new JSONObject();
      ret.put(MAP, map);
      map.put(KEY, extractColumn(((Map) column).getKey()));
      map.put(VALUE, extractColumn(((Map) column).getValue()));
      break;
    case ENUM:
    case SET:
      JSONObject list = new JSONObject();
      ret.put(LIST, list);
      list.put(LIST_TYPE, extractColumn(((AbstractComplexListType) column).getListType()));
      break;
    case ARRAY:
      JSONObject arrayList = new JSONObject();
      ret.put(LIST, arrayList);
      arrayList.put(SIZE, ((Array) column).getSize());
      arrayList.put(LIST_TYPE, extractColumn(((Array) column).getListType()));
      break;
    case BINARY:
    case TEXT:
      ret.put(CHAR_SIZE, ((AbstractString) column).getCharSize());
      break;
    case DATE_TIME:
      ret.put(FRACTION, ((DateTime) column).getFraction());
      ret.put(TIMEZONE, ((DateTime) column).getTimezone());
      break;
    case DECIMAL:
      ret.put(PRECISION, ((Decimal) column).getPrecision());
      ret.put(SCALE, ((Decimal) column).getScale());
      break;
    case FIXED_POINT:
      ret.put(BYTE_SIZE, ((FixedPoint) column).getByteSize());
      ret.put(UNSIGNED, ((FixedPoint) column).getUnsigned());
      break;
    case FLOATING_POINT:
      ret.put(BYTE_SIZE, ((FloatingPoint) column).getByteSize());
      break;
    case TIME:
      ret.put(FRACTION, ((Time) column).getFraction());
      break;
    case UNKNOWN:
      ret.put(JDBC_TYPE, ((Unknown) column).getJdbcType());
      break;
    case DATE:
    case BIT:
      break;
    default:
      // TODO(jarcec): Throw an exception of unsupported type?
    }

    return ret;
  }

  private static Column restoreColumn(JSONObject obj) {
    String name = (String) obj.get(NAME);

    Boolean nullable = (Boolean) obj.get(NULLABLE);
    AbstractPrimitiveType key = null;
    Column value = null;
    Long arraySize = null;
    Column listType = null;
    // complex type attribute
    if (obj.containsKey(MAP)) {
      JSONObject map = (JSONObject) obj.get(MAP);

      if (map.containsKey(KEY)) {
        key = (AbstractPrimitiveType) restoreColumn((JSONObject) map.get(KEY));
      }
      if (map.containsKey(VALUE)) {
        value = restoreColumn((JSONObject) map.get(VALUE));
      }
    }
    if (obj.containsKey(LIST)) {
      JSONObject list = (JSONObject) obj.get(LIST);
      if (list.containsKey(LIST_TYPE)) {
        listType = restoreColumn((JSONObject) list.get(LIST_TYPE));
      }
      arraySize = (Long) list.get(SIZE);
    }
    ColumnType type = ColumnType.valueOf((String) obj.get(TYPE));
    Column output = null;
    switch (type) {
    case ARRAY:
      output = new Array(name, listType).setSize(arraySize);
      break;
    case BINARY:
      Long charSize = (Long) obj.get(CHAR_SIZE);
      output = new Binary(name).setCharSize(charSize);
      break;
    case BIT:
      output = new Bit(name);
      break;
    case DATE:
      output = new Date(name);
      break;
    case DATE_TIME:
      Boolean fraction = (Boolean) obj.get(FRACTION);
      Boolean timezone = (Boolean) obj.get(TIMEZONE);
      output = new DateTime(name).setFraction(fraction).setTimezone(timezone);
      break;
    case DECIMAL:
      Long precision = (Long) obj.get(PRECISION);
      Long scale = (Long) obj.get(SCALE);
      output = new Decimal(name).setPrecision(precision).setScale(scale);
      break;
    case ENUM:
      output = new Enum(name).setListType(listType);
      break;
    case FIXED_POINT:
      Boolean unsigned = (Boolean) obj.get(UNSIGNED);
      Long fixedPointByteSize = (Long) obj.get(BYTE_SIZE);
      output = new FixedPoint(name).setByteSize(fixedPointByteSize).setUnsigned(unsigned);
      break;
    case FLOATING_POINT:
      Long floatingPointByteSize = (Long) obj.get(BYTE_SIZE);
      output = new FloatingPoint(name).setByteSize(floatingPointByteSize);
      break;
    case MAP:
      output = new Map(name, key, value);
      break;
    case SET:
      output = new Set(name).setListType(listType);
      break;
    case TEXT:
      charSize = (Long) obj.get(CHAR_SIZE);
      output = new Text(name).setCharSize(charSize);
      break;
    case TIME:
      Boolean timeFraction = (Boolean) obj.get(FRACTION);
      output = new Time(name).setFraction(timeFraction);
      break;
    case UNKNOWN:
      Long jdbcType = (Long) obj.get(JDBC_TYPE);
      output = new Unknown(name).setJdbcType(jdbcType);
      break;
    default:
      // TODO(Jarcec): Throw an exception of unsupported type?
    }
    output.setNullable(nullable);

    return output;
  }

  private SchemaSerialization() {
    // Serialization is prohibited
  }

}
