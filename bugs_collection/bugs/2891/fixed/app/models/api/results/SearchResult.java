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

import com.google.common.collect.Lists;
import lib.Field;
import lib.timeranges.TimeRange;
import models.FieldMapper;
import models.api.responses.MessageSummaryResponse;
import models.api.responses.SearchResultResponse;

import java.util.List;

public class SearchResult {
	
	private final String originalQuery;
    private final String builtQuery;
    private final TimeRange timeRange;
	private final long totalResultCount;
	private final int tookMs;
	private final List<MessageResult> results;
    private final SearchResultResponse.QueryError error;
    private final List<Field> fields;
    private final List<String> usedIndices;
    private List<Field> allFields;

    public SearchResult(String originalQuery,
                        String builtQuery,
                        TimeRange timeRange,
                        long totalResultCount,
                        int tookMs,
                        List<MessageSummaryResponse> summaryResponses,
                        List<String> fields,
                        List<String> usedIndices,
                        SearchResultResponse.QueryError error,
                        FieldMapper fieldMapper) {
        this.originalQuery = originalQuery;
        this.builtQuery = builtQuery;
        this.timeRange = timeRange;
		this.totalResultCount = totalResultCount;
		this.tookMs = tookMs;
        this.error = error;
        this.fields = buildFields(fields);
        this.usedIndices = usedIndices;

        // convert MessageSummaryResponses to MessageResult because of the post processing that happens there
        // otherwise we'd have to duplicate it everywhere.
        results = Lists.newArrayList();
        if (summaryResponses != null) {
            for (MessageSummaryResponse response : summaryResponses) {
                results.add(new MessageResult(response.message, response.index, fieldMapper));
            }
        }
    }
	
	public List<MessageResult> getMessages() {
		return results;
	}
	
	public String getOriginalQuery() {
		return originalQuery;
	}

    public TimeRange getTimeRange() {
        return timeRange;
    }

    public int getTookMs() {
		return tookMs;
	}
	
	public long getTotalResultCount() {
		return totalResultCount;
	}
	
	public List<Field> getPageFields() {
		return fields;
	}

    public List<String> getUsedIndices() {
        return usedIndices;
    }

    public void setAllFields(List<Field> allFields) {
        this.allFields = allFields;
    }

    public List<Field> getAllFields() {
        return allFields;
    }

    private List<Field> buildFields(List<String> sFields) {
		List<Field> fields = Lists.newArrayList();

        if (sFields != null) {
            for (String field : sFields) {
                fields.add(new Field(field));
            }
        }
		
		return fields;
	}

    public String getBuiltQuery() {
        return builtQuery;
    }

    public SearchResultResponse.QueryError getError() {
        return error;
    }

}
