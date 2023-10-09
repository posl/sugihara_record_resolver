/*
 * Copyright 2016 Crown Copyright
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

package stroom.job.client.presenter;

import stroom.alert.client.event.AlertEvent;
import stroom.cell.info.client.InfoHelpLinkColumn;
import stroom.cell.tickbox.client.TickBoxCell;
import stroom.cell.tickbox.shared.TickBoxState;
import stroom.cell.valuespinner.client.ValueSpinnerCell;
import stroom.cell.valuespinner.shared.EditableInteger;
import stroom.data.client.presenter.ColumnSizeConstants;
import stroom.data.client.presenter.RestDataProvider;
import stroom.data.grid.client.EndColumn;
import stroom.data.grid.client.MyDataGrid;
import stroom.data.grid.client.PagerView;
import stroom.dispatch.client.Rest;
import stroom.dispatch.client.RestFactory;
import stroom.job.client.JobTypeCell;
import stroom.job.shared.Job;
import stroom.job.shared.JobNode;
import stroom.job.shared.JobNode.JobType;
import stroom.job.shared.JobNodeInfo;
import stroom.job.shared.JobNodeResource;
import stroom.preferences.client.DateTimeFormatter;
import stroom.svg.client.Preset;
import stroom.svg.client.SvgPresets;
import stroom.ui.config.client.UiConfigCache;
import stroom.util.shared.ModelStringUtil;
import stroom.util.shared.ResultPage;

import com.google.gwt.cell.client.Cell.Context;
import com.google.gwt.cell.client.TextCell;
import com.google.gwt.core.client.GWT;
import com.google.gwt.dom.client.Element;
import com.google.gwt.dom.client.NativeEvent;
import com.google.gwt.user.cellview.client.Column;
import com.google.gwt.user.client.Window;
import com.google.inject.Inject;
import com.google.web.bindery.event.shared.EventBus;
import com.gwtplatform.mvp.client.MyPresenterWidget;

import java.util.HashMap;
import java.util.Map;
import java.util.function.Consumer;

public class JobNodeListPresenter extends MyPresenterWidget<PagerView> {

    private static final JobNodeResource JOB_NODE_RESOURCE = GWT.create(JobNodeResource.class);

    private final RestFactory restFactory;
    private final DateTimeFormatter dateTimeFormatter;
    private final SchedulePresenter schedulePresenter;
    private final UiConfigCache clientPropertyCache;

    private final RestDataProvider<JobNode, ResultPage<JobNode>> dataProvider;
    private final Map<JobNode, JobNodeInfo> latestNodeInfo = new HashMap<>();

    private final MyDataGrid<JobNode> dataGrid;

    private String jobName;

    @Inject
    public JobNodeListPresenter(final EventBus eventBus,
                                final PagerView view,
                                final RestFactory restFactory,
                                final DateTimeFormatter dateTimeFormatter,
                                final SchedulePresenter schedulePresenter,
                                final UiConfigCache clientPropertyCache) {
        super(eventBus, view);
        this.restFactory = restFactory;
        this.dateTimeFormatter = dateTimeFormatter;
        this.schedulePresenter = schedulePresenter;
        this.clientPropertyCache = clientPropertyCache;

        dataGrid = new MyDataGrid<>();
        dataGrid.addDefaultSelectionModel(true);
        view.setDataWidget(dataGrid);

        initTable();

        dataProvider = new RestDataProvider<JobNode, ResultPage<JobNode>>(eventBus) {
            @Override
            protected void exec(final Consumer<ResultPage<JobNode>> dataConsumer,
                                final Consumer<Throwable> throwableConsumer) {
                final Rest<ResultPage<JobNode>> rest = restFactory.create();
                rest.onSuccess(dataConsumer).onFailure(throwableConsumer).call(JOB_NODE_RESOURCE).list(jobName, null);
            }

            @Override
            protected void changeData(final ResultPage<JobNode> data) {
                // Ping each node.
                data.getValues().forEach(row -> {
                    final Rest<JobNodeInfo> rest = restFactory.create();
                    rest.onSuccess(info -> {
                        latestNodeInfo.put(row, info);
                        super.changeData(data);
                    }).onFailure(throwable -> {
                        latestNodeInfo.remove(row);
                        super.changeData(data);
                    }).call(JOB_NODE_RESOURCE).info(row.getJob().getName(), row.getNodeName());
                });
                super.changeData(data);
            }
        };
    }

    /**
     * Add the columns to the table.
     */
    private void initTable() {
        // Enabled.
        final Column<JobNode, TickBoxState> enabledColumn = new Column<JobNode, TickBoxState>(TickBoxCell.create(false,
                false)) {
            @Override
            public TickBoxState getValue(final JobNode row) {
                return TickBoxState.fromBoolean(row.isEnabled());
            }
        };
        enabledColumn.setFieldUpdater((index, row, value) -> {
            row.setEnabled(value.toBoolean());
            final Rest<JobNode> rest = restFactory.create();
            rest.call(JOB_NODE_RESOURCE).setEnabled(row.getId(), value.toBoolean());
        });
        dataGrid.addColumn(enabledColumn, "Enabled", 80);

        // Job Name
        final Column<JobNode, String> nameColumn = new Column<JobNode, String>(new TextCell()) {
            @Override
            public String getValue(final JobNode row) {
                return row.getJob().getName();
            }
        };
        dataGrid.addResizableColumn(nameColumn, "Job", 200);

        // Help
        dataGrid.addColumn(new InfoHelpLinkColumn<JobNode>() {
            @Override
            public Preset getValue(final JobNode row) {
                if (row != null) {
                    return SvgPresets.HELP;
                }
                return null;
            }

            @Override
            protected void showHelp(final JobNode row) {
                clientPropertyCache.get()
                        .onSuccess(result -> {
                            final String helpUrl = result.getHelpUrlJobs();
                            if (helpUrl != null && helpUrl.trim().length() > 0) {
                                // This is a bit fragile as if the headings change in the docs then the anchors
                                // won't work
                                final String url = helpUrl
                                        + formatAnchor(row.getJob().getName());
                                Window.open(url, "_blank", "");
                            } else {
                                AlertEvent.fireError(JobNodeListPresenter.this, "Help is not configured!", null);
                            }
                        })
                        .onFailure(caught -> AlertEvent.fireError(JobNodeListPresenter.this,
                                caught.getMessage(),
                                null));
            }

        }, "<br/>", 20);

        // Node Name
        final Column<JobNode, String> nodeColumn = new Column<JobNode, String>(new TextCell()) {
            @Override
            public String getValue(final JobNode row) {
                return row.getNodeName();
            }
        };
        dataGrid.addResizableColumn(nodeColumn, "Node", 200);

        // Schedule.
        final Column<JobNode, String> typeColumn = new Column<JobNode, String>(new TextCell()) {
            @Override
            public String getValue(final JobNode row) {
                final JobNode jobNode = row;
                final JobType jobType = jobNode.getJobType();
                if (JobType.CRON.equals(jobType)) {
                    return "Cron " + jobNode.getSchedule();
                } else if (JobType.FREQUENCY.equals(jobType)) {
                    return "Frequency " + jobNode.getSchedule();
                } else if (JobType.DISTRIBUTED.equals(jobType)) {
                    return "Distributed";
                }
                return null;
            }
        };
        dataGrid.addResizableColumn(typeColumn, "Type", 250);

        // Job Type.
        final Column<JobNode, JobType> typeEditColumn = new Column<JobNode, JobType>(new JobTypeCell()) {
            @Override
            public JobType getValue(final JobNode row) {
                if (row.getJobType() == null) {
                    return JobType.UNKNOWN;
                }
                return row.getJobType();
            }

            @Override
            public void onBrowserEvent(final Context context, final Element elem, final JobNode row,
                                       final NativeEvent event) {
                super.onBrowserEvent(context, elem, row, event);

                // Get the target element.
                final Element target = event.getEventTarget().cast();

                final String eventType = event.getType();
                if (row != null && "click".equals(eventType)) {
                    final String tagName = target.getTagName();
                    if ("img".equalsIgnoreCase(tagName)) {
                        final Rest<JobNodeInfo> rest = restFactory.create();
                        rest
                                .onSuccess(result -> setSchedule(row, result))
                                .onFailure(throwable -> setSchedule(row, null))
                                .call(JOB_NODE_RESOURCE)
                                .info(row.getJob().getName(), row.getNodeName());
                    }
                }
            }
        };
        dataGrid.addColumn(typeEditColumn, "", 20);

        // Max.
        final Column<JobNode, Number> maxColumn = new Column<JobNode, Number>(new ValueSpinnerCell(1, 1000)) {
            @Override
            public Number getValue(final JobNode row) {
                if (JobType.DISTRIBUTED.equals(row.getJobType())) {
                    return new EditableInteger(row.getTaskLimit());
                }
                return null;
            }
        };

        maxColumn.setFieldUpdater((index, row, value) -> {
            row.setTaskLimit(value.intValue());
            final Rest<JobNode> rest = restFactory.create();
            rest.call(JOB_NODE_RESOURCE).setTaskLimit(row.getId(), value.intValue());
        });
        dataGrid.addColumn(maxColumn, "Max", 59);

        // Cur.
        final Column<JobNode, String> curColumn = new Column<JobNode, String>(new TextCell()) {
            @Override
            public String getValue(final JobNode row) {
                final JobNodeInfo jobNodeInfo = latestNodeInfo.get(row);
                if (jobNodeInfo != null) {
                    return ModelStringUtil.formatCsv(jobNodeInfo.getCurrentTaskCount());
                } else {
                    return "?";
                }
            }
        };
        dataGrid.addColumn(curColumn, "Cur", 59);

        // Last executed.
        final Column<JobNode, String> lastExecutedColumn = new Column<JobNode, String>(new TextCell()) {
            @Override
            public String getValue(final JobNode row) {
                final JobNodeInfo jobNodeInfo = latestNodeInfo.get(row);
                if (jobNodeInfo != null) {
                    return dateTimeFormatter.format(jobNodeInfo.getLastExecutedTime());
                } else {
                    return "?";
                }
            }
        };
        dataGrid.addColumn(lastExecutedColumn, "Last Executed", ColumnSizeConstants.DATE_COL);

        dataGrid.addEndColumn(new EndColumn<>());
    }

    private void setSchedule(final JobNode jobNode, JobNodeInfo jobNodeInfo) {
        if (jobNodeInfo == null) {
            jobNodeInfo = new JobNodeInfo();
        }

        schedulePresenter.setSchedule(jobNode.getJobType(),
                jobNodeInfo.getScheduleReferenceTime(),
                jobNodeInfo.getLastExecutedTime(),
                jobNode.getSchedule());
        schedulePresenter.show(schedule -> {
            jobNode.setSchedule(schedule);
            final Rest<JobNode> rest = restFactory.create();
            rest
                    .onSuccess(result ->
                            dataProvider.refresh())
                    .call(JOB_NODE_RESOURCE)
                    .setSchedule(jobNode.getId(), schedule);
        });
    }

    public void read(final Job job) {
        if (jobName == null) {
            jobName = job.getName();
            dataProvider.addDataDisplay(dataGrid);
        } else {
            jobName = job.getName();
            dataProvider.refresh();
        }
    }
}
