/*
 * Copyright 2017 Crown Copyright
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

package stroom.query.client;

import stroom.datasource.api.v2.AbstractField;
import stroom.datasource.api.v2.DocRefField;
import stroom.datasource.api.v2.FieldTypes;
import stroom.dispatch.client.RestFactory;
import stroom.docref.DocRef;
import stroom.explorer.client.presenter.EntityDropDownPresenter;
import stroom.item.client.ItemListBox;
import stroom.query.api.v2.ExpressionTerm.Condition;
import stroom.util.shared.EqualsUtil;
import stroom.widget.customdatebox.client.MyDateBox;

import com.google.gwt.core.client.Scheduler;
import com.google.gwt.dom.client.Style.Unit;
import com.google.gwt.event.dom.client.ClickEvent;
import com.google.gwt.event.dom.client.InputEvent;
import com.google.gwt.event.dom.client.KeyCodes;
import com.google.gwt.event.dom.client.KeyDownHandler;
import com.google.gwt.user.client.ui.Composite;
import com.google.gwt.user.client.ui.FlowPanel;
import com.google.gwt.user.client.ui.Label;
import com.google.gwt.user.client.ui.SuggestBox;
import com.google.gwt.user.client.ui.Widget;
import com.google.web.bindery.event.shared.HandlerRegistration;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;

public class TermEditor extends Composite {

    private static final int WIDE_VALUE = 400;
    private static final int NARROW_VALUE = 175;

    private final FlowPanel layout;
    private final ItemListBox<AbstractField> fieldListBox;
    private final ItemListBox<Condition> conditionListBox;
    private final Label andLabel;
    private final SuggestBox value;
    private final SuggestBox valueFrom;
    private final SuggestBox valueTo;
    private final MyDateBox date;
    private final MyDateBox dateFrom;
    private final MyDateBox dateTo;
    private final Widget docRefWidget;
    private final EntityDropDownPresenter docRefPresenter;
    private final List<Widget> activeWidgets = new ArrayList<>();
    private final List<HandlerRegistration> registrations = new ArrayList<>();

    private Term term;
    private List<AbstractField> fields;
    private boolean reading;
    private boolean editing;
    private ExpressionUiHandlers uiHandlers;

    private final AsyncSuggestOracle suggestOracle = new AsyncSuggestOracle();

    public TermEditor(final EntityDropDownPresenter docRefPresenter) {
        this.docRefPresenter = docRefPresenter;
        if (docRefPresenter != null) {
            docRefWidget = docRefPresenter.getWidget();
        } else {
            docRefWidget = new Label();
        }

        fixStyle(docRefWidget, 200);
        docRefWidget.getElement().getStyle().setMarginTop(1, Unit.PX);
        docRefWidget.setVisible(false);

        fieldListBox = createFieldBox();
        conditionListBox = createConditionBox();

        andLabel = createLabel(" and ");
        andLabel.setVisible(false);

        value = createTextBox(WIDE_VALUE);
        value.setVisible(false);
        valueFrom = createTextBox(NARROW_VALUE);
        valueFrom.setVisible(false);
        valueTo = createTextBox(NARROW_VALUE);
        valueTo.setVisible(false);

        date = createDateBox(NARROW_VALUE);
        date.setVisible(false);
        dateFrom = createDateBox(NARROW_VALUE);
        dateFrom.setVisible(false);
        dateTo = createDateBox(NARROW_VALUE);
        dateTo.setVisible(false);

        layout = new FlowPanel();
        layout.add(fieldListBox);
        layout.add(conditionListBox);
        layout.add(value);
        layout.add(valueFrom);
        layout.add(date);
        layout.add(dateFrom);
        layout.add(andLabel);
        layout.add(valueTo);
        layout.add(dateTo);
        layout.add(docRefWidget);

        layout.setVisible(false);
        layout.setStyleName("termEditor-layout");
        initWidget(layout);
    }

    public void setUtc(final boolean utc) {
        date.setUtc(utc);
        dateFrom.setUtc(utc);
        dateTo.setUtc(utc);
    }

    public void init(final RestFactory restFactory, final DocRef dataSource, final List<AbstractField> fields) {
        suggestOracle.setRestFactory(restFactory);
        suggestOracle.setDataSource(dataSource);
        this.fields = fields;
        fieldListBox.clear();
        if (fields != null) {
            fieldListBox.addItems(fields);
        }
    }

    public void startEdit(final Term term) {
        if (!editing) {
            this.term = term;

            read(term);

            Scheduler.get().scheduleDeferred(() -> {
                bind();
                layout.setVisible(true);
            });

            editing = true;
        }
    }

    public void endEdit() {
        if (editing) {
            write(term);
            unbind();
            layout.setVisible(false);
            editing = false;
        }
    }

    private void read(final Term term) {
        reading = true;

        // Select the current value.
        AbstractField termField = null;
        if (fields != null && fields.size() > 0) {
            termField = fields.get(0);
            for (final AbstractField field : fields) {
                if (field.getName().equals(term.getField())) {
                    termField = field;
                    break;
                }
            }
        }

        fieldListBox.setSelectedItem(termField);
        conditionListBox.setSelectedItem(null);
        changeField(termField, false);

        reading = false;
    }

    private void write(final Term term) {
        if (fieldListBox.getSelectedItem() != null && conditionListBox.getSelectedItem() != null) {
            DocRef docRef = null;

            term.setField(fieldListBox.getSelectedItem().getName());
            term.setCondition(conditionListBox.getSelectedItem());

            final StringBuilder sb = new StringBuilder();
            for (final Widget widget : activeWidgets) {
                if (widget instanceof SuggestBox) {
                    sb.append(((SuggestBox) widget).getText());
                    sb.append(",");
                } else if (widget instanceof MyDateBox) {
                    sb.append(((MyDateBox) widget).getValue());
                    sb.append(",");
                } else if (widget.equals(docRefWidget)) {
                    if (docRefPresenter != null) {
                        docRef = docRefPresenter.getSelectedEntityReference();
                        if (docRef != null) {
                            sb.append(docRef.getName());
                        }
                    }
                    sb.append(",");
                }
            }

            if (sb.length() > 0) {
                sb.setLength(sb.length() - 1);
            }

            term.setValue(sb.toString());
            term.setDocRef(docRef);
        }
    }

    private void changeField(final AbstractField field, final boolean useDefaultCondition) {
        suggestOracle.setField(field);
        final List<Condition> conditions = getConditions(field);

        Condition selected = conditionListBox.getSelectedItem();
        conditionListBox.clear();
        conditionListBox.addItems(conditions);

        if (selected == null || !conditions.contains(selected)) {
            if (!useDefaultCondition && term.getCondition() != null && conditions.contains(term.getCondition())) {
                selected = term.getCondition();
            } else if (conditions.contains(Condition.IS_DOC_REF)) {
                selected = Condition.IS_DOC_REF;
            } else if (conditions.contains(Condition.EQUALS)) {
                selected = Condition.EQUALS;
            } else {
                selected = conditions.get(0);
            }
        }

        conditionListBox.setSelectedItem(selected);
        changeCondition(field, selected);
    }

    private List<Condition> getConditions(final AbstractField field) {
        List<Condition> conditions;

        if (field == null) {
            conditions = Arrays.asList(
                    Condition.EQUALS,
                    Condition.IN,
                    Condition.IN_DICTIONARY
            );

        } else if (field.getConditions() != null && field.getConditions().size() > 0) {
            conditions = field.getConditions();
        } else {
            if (FieldTypes.DOC_REF.equals(field.getType())) {
                conditions = Arrays.asList(
                        Condition.EQUALS,
                        Condition.IN,
                        Condition.IN_DICTIONARY,
                        Condition.IS_DOC_REF);
            } else if (field.isNumeric()) {
                conditions = Arrays.asList(
                        Condition.EQUALS,
                        Condition.GREATER_THAN,
                        Condition.GREATER_THAN_OR_EQUAL_TO,
                        Condition.LESS_THAN,
                        Condition.LESS_THAN_OR_EQUAL_TO,
                        Condition.BETWEEN,
                        Condition.IN,
                        Condition.IN_DICTIONARY
                );

            } else if (FieldTypes.DATE.equals(field.getType())) {
                conditions = Arrays.asList(
                        Condition.EQUALS,
                        Condition.GREATER_THAN,
                        Condition.GREATER_THAN_OR_EQUAL_TO,
                        Condition.LESS_THAN,
                        Condition.LESS_THAN_OR_EQUAL_TO,
                        Condition.BETWEEN,
                        Condition.IN,
                        Condition.IN_DICTIONARY
                );

            } else {
                conditions = Arrays.asList(
                        Condition.EQUALS,
                        Condition.IN,
                        Condition.IN_DICTIONARY
                );
            }
        }

        // We no longer use CONTAINS.
        conditions.remove(Condition.CONTAINS);

        return conditions;
    }

    private void changeCondition(final AbstractField field,
                                 final Condition condition) {
        String indexFieldType = null;
        if (fieldListBox.getSelectedItem() != null) {
            indexFieldType = fieldListBox.getSelectedItem().getType();
        }

        if (indexFieldType == null) {
            setActiveWidgets();

        } else {
            switch (condition) {
                case EQUALS:
                    if (FieldTypes.DATE.equals(indexFieldType)) {
                        enterDateMode();
                    } else {
                        enterTextMode();
                    }
                    break;
//                case CONTAINS:
//                    enterTextMode();
//                    break;
                case IN:
                    enterTextMode();
                    break;
                case BETWEEN:
                    if (FieldTypes.DATE.equals(indexFieldType)) {
                        enterDateRangeMode();
                    } else {
                        enterTextRangeMode();
                    }
                    break;
                case LESS_THAN:
                    if (FieldTypes.DATE.equals(indexFieldType)) {
                        enterDateMode();
                    } else {
                        enterTextMode();
                    }
                    break;
                case LESS_THAN_OR_EQUAL_TO:
                    if (FieldTypes.DATE.equals(indexFieldType)) {
                        enterDateMode();
                    } else {
                        enterTextMode();
                    }
                    break;
                case GREATER_THAN:
                    if (FieldTypes.DATE.equals(indexFieldType)) {
                        enterDateMode();
                    } else {
                        enterTextMode();
                    }
                    break;
                case GREATER_THAN_OR_EQUAL_TO:
                    if (FieldTypes.DATE.equals(indexFieldType)) {
                        enterDateMode();
                    } else {
                        enterTextMode();
                    }
                    break;
                case IN_DICTIONARY:
                    enterDocRefMode(field, condition);
                    break;
                case IN_FOLDER:
                    enterDocRefMode(field, condition);
                    break;
                case IS_DOC_REF:
                    enterDocRefMode(field, condition);
                    break;
                case MATCHES_REGEX:
                    enterTextMode();
                    break;
            }
        }
    }

    private void enterTextMode() {
        setActiveWidgets(value);
        value.setText(term.getValue());
    }

    private void enterTextRangeMode() {
        setActiveWidgets(valueFrom, andLabel, valueTo);
        updateTextBoxes();
    }

    private void enterDateMode() {
        setActiveWidgets(date);
        updateDateBoxes();
    }

    private void enterDateRangeMode() {
        setActiveWidgets(dateFrom, andLabel, dateTo);
        updateDateBoxes();
    }

    private void enterDocRefMode(final AbstractField field, final Condition condition) {
        setActiveWidgets(docRefWidget);

        if (docRefPresenter != null) {
            docRefPresenter.setAllowFolderSelection(false);
            if (Condition.IN_DICTIONARY.equals(condition)) {
                docRefPresenter.setIncludedTypes("Dictionary");
            } else if (Condition.IN_FOLDER.equals(condition)) {
                docRefPresenter.setIncludedTypes("Folder");
                docRefPresenter.setAllowFolderSelection(true);
            } else if (field instanceof DocRefField) {
                docRefPresenter.setIncludedTypes(((DocRefField) field).getDocRefType());
            }
            docRefPresenter.setSelectedEntityReference(term.getDocRef());
        }
    }

    private void setActiveWidgets(final Widget... widgets) {
        for (final Widget widget : activeWidgets) {
            widget.setVisible(false);
        }
        activeWidgets.clear();
        for (final Widget widget : widgets) {
            activeWidgets.add(widget);
            widget.setVisible(true);
        }
    }

    private void updateTextBoxes() {
        if (term.getValue() != null) {
            // Set the current data.
            final String[] vals = term.getValue().split(",");
            if (vals.length > 0) {
                if (value != null) {
                    value.setValue(vals[0]);
                }
                if (valueFrom != null) {
                    valueFrom.setValue(vals[0]);
                }
            }
            if (vals.length > 1) {
                if (valueTo != null) {
                    valueTo.setValue(vals[1]);
                }
            }
        }
    }

    private void updateDateBoxes() {
        if (term.getValue() != null) {
            // Set the current data.
            final String[] vals = term.getValue().split(",");
            if (vals.length > 0) {
                if (date != null) {
                    date.setValue(vals[0]);
                }
                if (dateFrom != null) {
                    dateFrom.setValue(vals[0]);
                }
            }
            if (vals.length > 1) {
                if (dateTo != null) {
                    dateTo.setValue(vals[1]);
                }
            }
        }


//        if (term.getValue() != null) {
//            // Set the current data.
//            final String[] vals = term.getValue().split(",");
//            if (vals.length > 0) {
//                if (date != null) {
//                    final Date d = date.getFormat().parse(date, vals[0], false);
//                    if (d != null) {
//                        date.setValue(d);
//                    }
//                }
//                if (dateFrom != null) {
//                    final Date d = dateFrom.getFormat().parse(dateFrom, vals[0], false);
//                    if (d != null) {
//                        dateFrom.setValue(d);
//                    }
//                }
//            }
//            if (vals.length > 1) {
//                if (dateTo != null) {
//                    final Date d = dateTo.getFormat().parse(dateTo, vals[1], false);
//                    if (d != null) {
//                        dateTo.setValue(d);
//                    }
//                }
//            }
//        }
    }

    private void bind() {
        final KeyDownHandler keyDownHandler = event -> {
            if (event.getNativeKeyCode() == KeyCodes.KEY_ENTER) {
                if (uiHandlers != null) {
                    uiHandlers.search();
                }
            }
        };

        registerHandler(value.addKeyDownHandler(keyDownHandler));
        registerHandler(valueFrom.addKeyDownHandler(keyDownHandler));
        registerHandler(valueTo.addKeyDownHandler(keyDownHandler));

        registerHandler(date.addKeyDownHandler(keyDownHandler));
        registerHandler(dateFrom.addKeyDownHandler(keyDownHandler));
        registerHandler(dateTo.addKeyDownHandler(keyDownHandler));

        registerHandler(value.addDomHandler(e -> fireDirty(), InputEvent.getType()));
        registerHandler(valueFrom.addDomHandler(e -> fireDirty(), InputEvent.getType()));
        registerHandler(valueTo.addDomHandler(e -> fireDirty(), InputEvent.getType()));
        registerHandler(date.addDomHandler(e -> fireDirty(), InputEvent.getType()));
        registerHandler(dateFrom.addDomHandler(e -> fireDirty(), InputEvent.getType()));
        registerHandler(dateTo.addDomHandler(e -> fireDirty(), InputEvent.getType()));

        registerHandler(date.addValueChangeHandler(event -> fireDirty()));
        registerHandler(dateFrom.addValueChangeHandler(event -> fireDirty()));
        registerHandler(dateTo.addValueChangeHandler(event -> fireDirty()));

        if (docRefPresenter != null) {
            registerHandler(docRefPresenter.addDataSelectionHandler(event -> {
                final DocRef selection = docRefPresenter.getSelectedEntityReference();
                if (!EqualsUtil.isEquals(term.getDocRef(), selection)) {
                    write(term);
                    fireDirty();
                }
            }));

        }

        registerHandler(fieldListBox.addSelectionHandler(event -> {
            if (!reading) {
                write(term);
                changeField(event.getSelectedItem(), true);
                fireDirty();
            }
        }));
        registerHandler(conditionListBox.addSelectionHandler(event -> {
            if (!reading) {
                write(term);
                changeCondition(fieldListBox.getSelectedItem(), event.getSelectedItem());
                fireDirty();
            }
        }));
    }

    private void unbind() {
        for (final HandlerRegistration handlerRegistration : registrations) {
            handlerRegistration.removeHandler();
        }
        registrations.clear();
    }

    private void registerHandler(final HandlerRegistration handlerRegistration) {
        registrations.add(handlerRegistration);
    }

    private ItemListBox<AbstractField> createFieldBox() {
        final ItemListBox<AbstractField> fieldListBox = new ItemListBox<>();
        fixStyle(fieldListBox, 160);
        return fieldListBox;
    }

    private ItemListBox<Condition> createConditionBox() {
        final ItemListBox<Condition> conditionListBox = new ItemListBox<>();
        fixStyle(conditionListBox, 120);
        return conditionListBox;
    }

    private SuggestBox createTextBox(final int width) {
        final SuggestBox textBox = new SuggestBox(suggestOracle);
        textBox.addDomHandler(e -> {
            if (!textBox.isSuggestionListShowing()) {
                textBox.showSuggestionList();
            }
        }, ClickEvent.getType());
        fixStyle(textBox, width);
        return textBox;
    }

    private MyDateBox createDateBox(final int width) {
        final MyDateBox dateBox = new MyDateBox();
        fixStyle(dateBox, width);
        return dateBox;
    }

    private Label createLabel(final String text) {
        final Label label = new Label(text, false);
        label.addStyleName("termEditor-label");
        return label;
    }

    private void fixStyle(final Widget widget, final int width) {
        widget.addStyleName("termEditor-item");
        widget.getElement().getStyle().setWidth(width, Unit.PX);
    }

    private void fireDirty() {
        if (!reading) {
            if (uiHandlers != null) {
                uiHandlers.fireDirty();
            }
        }
    }

    public void setUiHandlers(final ExpressionUiHandlers uiHandlers) {
        this.uiHandlers = uiHandlers;
    }
}
