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
package models;

import java.util.Map;

/**
 * @author Lennart Koopmann <lennart@torch.sh>
 */
public class Converter {

    public enum Type {
        NUMERIC,
        DATE,
        HASH,
        SPLIT_AND_COUNT,
        IP_ANONYMIZER,
        SYSLOG_PRI_LEVEL,
        SYSLOG_PRI_FACILITY,
        TOKENIZER,
        CSV;

        public static Type fromString(String type) {
            return valueOf(type.toUpperCase());
        }
    }

    private final Type type;
    private final Map<String, Object> config;

    public Converter(Type type, Map<String, Object> config) {
        this.type = type;
        this.config = config;
    }

    public String getType() {
        return this.type.toString().toLowerCase();
    }

    public Map<String, Object> getConfig() {
        return config;
    }

}
