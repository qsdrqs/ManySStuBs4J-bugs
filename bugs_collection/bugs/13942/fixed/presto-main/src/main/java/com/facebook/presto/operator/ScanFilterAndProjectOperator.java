/*
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
package com.facebook.presto.operator;

import com.facebook.presto.metadata.ColumnHandle;
import com.facebook.presto.metadata.Split;
import com.facebook.presto.spi.RecordCursor;
import com.facebook.presto.spi.type.Type;
import com.facebook.presto.split.DataStreamProvider;
import com.facebook.presto.sql.planner.plan.PlanNodeId;
import com.google.common.base.Suppliers;
import com.google.common.collect.ImmutableList;
import com.google.common.util.concurrent.ListenableFuture;
import io.airlift.units.DataSize;

import javax.annotation.concurrent.GuardedBy;

import java.io.Closeable;
import java.util.List;

import static com.google.common.base.Preconditions.checkNotNull;
import static com.google.common.base.Preconditions.checkState;
import static io.airlift.units.DataSize.Unit.BYTE;

public class ScanFilterAndProjectOperator
        implements SourceOperator, Closeable
{
    private static final int ROWS_PER_PAGE = 16384;

    private final OperatorContext operatorContext;
    private final PlanNodeId planNodeId;
    private final DataStreamProvider dataStreamProvider;
    private final List<Type> types;
    private final List<ColumnHandle> columns;
    private final PageBuilder pageBuilder;
    private final CursorProcessor cursorProcessor;
    private final PageProcessor pageProcessor;

    @GuardedBy("this")
    private RecordCursor cursor;

    @GuardedBy("this")
    private Operator operator;

    private Page currentPage;
    private int currentPosition;

    private boolean finishing;

    private long completedBytes;
    private long readTimeNanos;

    protected ScanFilterAndProjectOperator(
            OperatorContext operatorContext,
            PlanNodeId sourceId,
            DataStreamProvider dataStreamProvider,
            CursorProcessor cursorProcessor,
            PageProcessor pageProcessor,
            Iterable<ColumnHandle> columns,
            Iterable<Type> types)
    {
        this.cursorProcessor = checkNotNull(cursorProcessor, "cursorProcessor is null");
        this.pageProcessor = checkNotNull(pageProcessor, "pageProcessor is null");
        this.operatorContext = checkNotNull(operatorContext, "operatorContext is null");
        this.planNodeId = checkNotNull(sourceId, "sourceId is null");
        this.dataStreamProvider = checkNotNull(dataStreamProvider, "dataStreamProvider is null");
        this.types = ImmutableList.copyOf(checkNotNull(types, "types is null"));
        this.columns = ImmutableList.copyOf(checkNotNull(columns, "columns is null"));

        this.pageBuilder = new PageBuilder(getTypes());
    }

    @Override
    public OperatorContext getOperatorContext()
    {
        return operatorContext;
    }

    @Override
    public PlanNodeId getSourceId()
    {
        return planNodeId;
    }

    @Override
    public synchronized void addSplit(Split split)
    {
        checkNotNull(split, "split is null");
        checkState(cursor == null && operator == null, "split already set");

        Operator dataStream = dataStreamProvider.createNewDataStream(operatorContext, split, columns);
        if (dataStream instanceof RecordProjectOperator) {
            cursor = ((RecordProjectOperator) dataStream).getCursor();
        }
        else {
            operator = dataStream;
        }

        Object splitInfo = split.getInfo();
        if (splitInfo != null) {
            operatorContext.setInfoSupplier(Suppliers.ofInstance(splitInfo));
        }
    }

    @Override
    public synchronized void noMoreSplits()
    {
        if (cursor == null && operator == null) {
            finishing = true;
        }
    }

    @Override
    public final List<Type> getTypes()
    {
        return types;
    }

    @Override
    public final void finish()
    {
        close();
    }

    @Override
    public void close()
    {
        if (operator != null) {
            operator.finish();
        }
        else if (cursor != null) {
            cursor.close();
        }
        finishing = true;
    }

    @Override
    public final boolean isFinished()
    {
        if (operator != null && operator.isFinished()) {
            finishing = true;
        }

        return finishing && pageBuilder.isEmpty();
    }

    @Override
    public ListenableFuture<?> isBlocked()
    {
        if (operator != null) {
            return operator.isBlocked();
        }
        return NOT_BLOCKED;
    }

    @Override
    public final boolean needsInput()
    {
        return false;
    }

    @Override
    public final void addInput(Page page)
    {
        throw new UnsupportedOperationException();
    }

    @Override
    public Page getOutput()
    {
        if (!finishing) {
            if (cursor != null) {
                int rowsProcessed = cursorProcessor.process(operatorContext.getSession(), cursor, ROWS_PER_PAGE, pageBuilder);
                long bytesProcessed = cursor.getCompletedBytes() - completedBytes;
                long elapsedNanos = cursor.getReadTimeNanos() - readTimeNanos;
                operatorContext.recordGeneratedInput(new DataSize(bytesProcessed, BYTE), rowsProcessed, elapsedNanos);
                completedBytes = cursor.getCompletedBytes();
                readTimeNanos = cursor.getReadTimeNanos();

                if (rowsProcessed == 0) {
                    finishing = true;
                }
            }
            else {
                if (currentPage == null) {
                    currentPage = operator.getOutput();
                    currentPosition = 0;
                }

                if (currentPage != null) {
                    currentPosition = pageProcessor.process(operatorContext.getSession(), currentPage, currentPosition, currentPage.getPositionCount(), pageBuilder);
                    if (currentPosition == currentPage.getPositionCount()) {
                        currentPage = null;
                        currentPosition = 0;
                    }
                }
            }
        }

        // only return a full page if buffer is full or we are finishing
        if (pageBuilder.isEmpty() || (!finishing && !pageBuilder.isFull())) {
            return null;
        }

        Page page = pageBuilder.build();
        pageBuilder.reset();
        return page;
    }

    public static class ScanFilterAndProjectOperatorFactory
            implements SourceOperatorFactory
    {
        private final int operatorId;
        private final CursorProcessor cursorProcessor;
        private final PageProcessor pageProcessor;
        private final PlanNodeId sourceId;
        private final DataStreamProvider dataStreamProvider;
        private final List<ColumnHandle> columns;
        private final List<Type> types;
        private boolean closed;

        public ScanFilterAndProjectOperatorFactory(
                int operatorId,
                PlanNodeId sourceId,
                DataStreamProvider dataStreamProvider,
                CursorProcessor cursorProcessor,
                PageProcessor pageProcessor,
                Iterable<ColumnHandle> columns,
                List<Type> types)
        {
            this.operatorId = operatorId;
            this.cursorProcessor = checkNotNull(cursorProcessor, "cursorProcessor is null");
            this.pageProcessor = checkNotNull(pageProcessor, "pageProcessor is null");
            this.sourceId = checkNotNull(sourceId, "sourceId is null");
            this.dataStreamProvider = checkNotNull(dataStreamProvider, "dataStreamProvider is null");
            this.columns = ImmutableList.copyOf(checkNotNull(columns, "columns is null"));
            this.types = checkNotNull(types, "types is null");
        }

        @Override
        public PlanNodeId getSourceId()
        {
            return sourceId;
        }

        @Override
        public List<Type> getTypes()
        {
            return types;
        }

        @Override
        public SourceOperator createOperator(DriverContext driverContext)
        {
            checkState(!closed, "Factory is already closed");
            OperatorContext operatorContext = driverContext.addOperatorContext(operatorId, FilterAndProjectOperator.class.getSimpleName());
            return new ScanFilterAndProjectOperator(
                    operatorContext,
                    sourceId,
                    dataStreamProvider,
                    cursorProcessor,
                    pageProcessor,
                    columns,
                    types);
        }

        @Override
        public void close()
        {
            closed = true;
        }
    }
}
