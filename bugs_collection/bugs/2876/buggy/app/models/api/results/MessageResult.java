/*
/*
 * Copyright 2013 TORCH UG
 *
 * This file is part of Graylog2.
 *
 * Graylog2 is free software: you can redistribute it and/or modify
 * it under the terms of the GNU General Public License as published by
 * the Free Software Foundation, either version 3 of the License, or
 * (at your option) any later version.
 *
 * Graylog2 is distributed in the hope that it will be useful,
 * but WITHOUT ANY WARRANTY; without even the implied warranty of
 * MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.  See the
 * GNU General Public License for more details.
 *
 * You should have received a copy of the GNU General Public License
 * along with Graylog2.  If not, see <http://www.gnu.org/licenses/>.
 */
package models.api.results;

import com.google.common.base.Predicate;
import com.google.common.collect.ImmutableSet;
import com.google.common.collect.Lists;
import com.google.common.collect.Maps;
import models.FieldMapper;
import org.joda.time.DateTime;

import javax.annotation.Nullable;
import java.text.DecimalFormat;
import java.util.*;

public class MessageResult {
    private final static Set<String> HIDDEN_FIELDS = ImmutableSet.of(
            "_id",
            "timestamp",
            "streams",
            "gl2_source_input",
            "gl2_source_node",
            "gl2_source_radio",
            "gl2_source_radio_input"
    );

    private final Map<String, Object> fields;
    private final String index;
    private final String id;
    private final DateTime timestamp;
    private final String sourceNodeId;
    private final String sourceInputId;
    private final String sourceRadioId;
    private final String sourceRadioInputId;
    private final List<String> streamIds;
    private final FieldMapper fieldMapper;

    public MessageResult(Map<String, Object> message, String index, FieldMapper fieldMapper) {
        this.fieldMapper = fieldMapper;
        // this comparator sorts fields alphabetically, but always leaves full_message at the end.
        // it really is interface, but I don't want to put it into the template either.
        // doing it here also means we don't have to copy the entire map when sorting...
        fields = new TreeMap<>(new Comparator<String>() {
            @Override
            public int compare(String left, String right) {
                // full_message is always "larger" than anything else, except itself
                final boolean rightIsFullMessage = right.equals("full_message");
                final boolean leftisFullMessage = left.equals("full_message");
                if (leftisFullMessage) {
                    if (rightIsFullMessage) {
                        return 0; // SAME
                    } else {
                        return 1; // LEFT is larger
                    }
                } else {
                    if (rightIsFullMessage) {
                        return -1; // RIGHT is larger
                    }
                }
                return left.compareTo(right);
            }
        });
        for (Map.Entry<String, Object> f : message.entrySet()) {
            fields.put(f.getKey(), f.getValue());
        }

        this.id = (String) message.get("_id");
        final Object timestamp1 = message.get("timestamp");
        this.timestamp = new DateTime(timestamp1);

        this.sourceNodeId = (String) message.get("gl2_source_node");
        this.sourceInputId = (String) message.get("gl2_source_input");
        this.index = index;
        this.streamIds = (List<String>) message.get("streams");

        if(message.containsKey("gl2_source_radio")) {
            sourceRadioId = (String) message.get("gl2_source_radio");
            sourceRadioInputId = (String) message.get("gl2_source_radio_input");
        } else {
            sourceRadioId = null;
            sourceRadioInputId = null;
        }
    }

    public String getId() {
        return id;
    }

    public DateTime getTimestamp() {
        return timestamp;
    }

    public Map<String, Object> getFilteredFields() {
        // return a _view_ of the fields map, do not make a copy, because subsequent manipulation would get lost!
        return Maps.filterEntries(getFields(), new Predicate<Map.Entry<String, Object>>() {
            @Override
            public boolean apply(@Nullable Map.Entry<String, Object> input) {
                return input != null && !HIDDEN_FIELDS.contains(input.getKey());
            }
        });
    }

    public Map<String, Object> getFields() {
        return fields;
    }

    public Map<String, Object> getFormattedFields() {
        final DecimalFormat doubleFormatter = new DecimalFormat("#");

        return Maps.transformEntries(getFilteredFields(), new Maps.EntryTransformer<String, Object, Object>() {
            @Override
            public Object transformEntry(@Nullable String key, @Nullable Object value) {
                // Get rid of .0 of doubles. 9001.0 becomes "9001", 9001.25 becomes "9001.25"
                // Never format a double in scientific notation.
                if(value instanceof Double) {
                    Double d = (Double) value;
                    if (d.longValue() == d) {
                        // preserve the "numberness" of the value, so the field mappers can take this into account
                        // basically wait with stringification until the last moment in the template
                        value = d.longValue();
                    } else {
                        value = doubleFormatter.format(d);
                    }
                }
                return fieldMapper.map(key, value);
            }
        });
    }

    public String getIndex() {
        return index;
    }

    public String getSourceNodeId() {
        return sourceNodeId;
    }

    public String getSourceInputId() {
        return sourceInputId;
    }

    public boolean viaRadio() {
        return sourceRadioId != null;
    }

    public String getSourceRadioId() {
        return sourceRadioId;
    }

    public String getSourceRadioInputId() {
        return sourceRadioInputId;
    }

    public List<String> getStreamIds() {
        if (this.streamIds != null) {
            return this.streamIds;
        } else {
            return Lists.newArrayList();
        }
    }

}
