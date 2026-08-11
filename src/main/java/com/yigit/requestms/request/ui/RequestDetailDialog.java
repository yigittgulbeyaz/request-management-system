package com.yigit.requestms.request.ui;

import com.vaadin.flow.component.button.Button;
import com.vaadin.flow.component.dialog.Dialog;
import com.vaadin.flow.component.html.H3;
import com.vaadin.flow.component.html.Paragraph;
import com.vaadin.flow.component.orderedlayout.VerticalLayout;
import com.vaadin.flow.theme.lumo.LumoUtility;
import com.yigit.requestms.request.dto.CustomerRequestDetailDto;
import com.yigit.requestms.request.enums.RequestStatus;

// Shows one request in full, including the description that list projections
// leave out. Carries no score or workflow stage: those belong to the product
// owner's view of the same request, not the customer's.
public class RequestDetailDialog extends Dialog {

    public RequestDetailDialog(CustomerRequestDetailDto request) {
        setHeaderTitle(request.title());
        setWidth("640px");

        VerticalLayout content = new VerticalLayout();
        content.setPadding(false);
        content.add(CustomerStatusPresentation.badge(request.status()));
        content.add(new Paragraph(request.description()));

        if (request.status() == RequestStatus.REJECTED && request.rejectionReason() != null) {
            content.add(rejectionSection(request.rejectionReason()));
        }

        add(content);
        getFooter().add(new Button("Close", e -> close()));
    }

    private VerticalLayout rejectionSection(String reason) {
        H3 heading = new H3("Reason");
        heading.addClassNames(LumoUtility.FontSize.MEDIUM);

        VerticalLayout section = new VerticalLayout(heading, new Paragraph(reason));
        section.setPadding(false);
        section.setSpacing(false);
        return section;
    }
}