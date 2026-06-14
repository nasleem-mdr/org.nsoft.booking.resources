package org.nsoft.booking.dashboard;

import java.util.List;
import org.adempiere.webui.apps.AEnv;
import org.adempiere.webui.component.Combobox;
import org.adempiere.webui.component.Comboitem;
import org.adempiere.webui.dashboard.DashboardRunnable;
import org.compiere.model.MResource;
import org.compiere.model.MResourceAssignment;
import org.compiere.model.Query;
import org.compiere.util.Env;
import org.zkoss.calendar.Calendars;
import org.zkoss.calendar.impl.SimpleCalendarEvent;
import org.zkoss.calendar.impl.SimpleCalendarModel;
import org.zkoss.zk.ui.Component;
import org.zkoss.zk.ui.event.Event;
import org.zkoss.zk.ui.event.EventListener;
import org.zkoss.zk.ui.event.Events;
import org.zkoss.zul.Div;
import org.zkoss.zul.Hlayout;

import org.nsoft.booking.model.MNSBooking;
import org.nsoft.booking.model.MNSBookingLine;

public class NSResourceTimelineDashboard extends Div implements DashboardRunnable {

    private Calendars calendar;
    private SimpleCalendarModel calendarModel;
    private Combobox resourceFilter;
    private int selectedResourceID = 0; // 0 berarti "Semua Kendaraan"

    public NSResourceTimelineDashboard() {
        initUI();
        loadTimelineData();
    }

    private void initUI() {
        // Layout Atas untuk Panel Kontrol / Filter
        Hlayout filterPanel = new Hlayout();
        filterPanel.setStyle("margin-bottom: 10px;");
        
        // Buat Dropdown Filter Resource Standar iDempiere
        resourceFilter = new Combobox();
        resourceFilter.setPlaceholder("--- Pilih Kendaraan (Semua) ---");
        resourceFilter.setAutodrop(true);
        resourceFilter.setWidth("250px");
        
        // Isi pilihan Dropdown langsung dari database S_Resource yang aktif
        Comboitem allItem = new Comboitem("--- Semua Kendaraan ---");
        allItem.setValue(0);
        resourceFilter.appendChild(allItem);
        resourceFilter.setSelectedItem(allItem);
        
        List<MResource> resources = new Query(Env.getCtx(), MResource.Table_Name, "IsActive='Y'", null).list();
        for (MResource res : resources) {
            Comboitem item = new Comboitem(res.getName());
            item.setValue(res.getS_Resource_ID());
            resourceFilter.appendChild(item);
        }
        
        // Deteksi Aksi Perubahan Filter Dropdown
        resourceFilter.addEventListener(Events.ON_SELECT, new EventListener<Event>() {
            @Override
            public void onEvent(Event event) throws Exception {
                Comboitem selectedItem = resourceFilter.getSelectedItem();
                if (selectedItem != null) {
                    selectedResourceID = (int) selectedItem.getValue();
                    loadTimelineData(); // Tarik ulang data kalender berdasarkan filter terpilih
                }
            }
        });
        
        filterPanel.appendChild(resourceFilter);
        this.appendChild(filterPanel);

        // Konfigurasi Kalender Waktu Standar iDempiere
        calendar = new Calendars();
        calendar.setMold("day");
        calendar.setDays(1);
        calendar.setBeginTime(6);
        calendar.setEndTime(22);
        
        calendarModel = new SimpleCalendarModel();
        calendar.setModel(calendarModel);
        
        this.appendChild(calendar);
    }

    private void loadTimelineData() {
        calendarModel.clear();

        // -------------------------------------------------------------------------
        // KONDISI FILTER QUERY: Apakah memilih kendaraan tertentu atau semua?
        // -------------------------------------------------------------------------
        String resourceFilterClause = "";
        if (selectedResourceID > 0) {
            resourceFilterClause = " AND S_Resource_ID = " + selectedResourceID;
        }

        // ==========================================
        // 1. QUERY DATA DRAFT (Dari Tabel Kustom)
        // ==========================================
        String sqlDraft = "IsActive='Y' AND StartDate >= CURRENT_DATE AND StartDate < CURRENT_DATE + 1"
                        + " AND DocStatus='DR'" + resourceFilterClause;
        
        List<MNSBooking> draftBookings = new Query(Env.getCtx(), MNSBooking.Table_Name, sqlDraft, null).list();
        for (MNSBooking booking : draftBookings) {
            SimpleCalendarEvent sce = new SimpleCalendarEvent();
            sce.setBeginDate(booking.getStartDate());
            sce.setEndDate(booking.getEndDate());
            
            MResource resource = MResource.get(Env.getCtx(), booking.getS_Resource_ID());
            sce.setTitle("[DRAFT] " + resource.getName() + " - " + booking.getDocumentNo());
            sce.setContent(String.valueOf(booking.getNS_Booking_ID()));
            
            sce.setHeaderColor("#E6A23C"); // Oranye Penanda Draft
            sce.setContentColor("#FDF6EC");
            calendarModel.add(sce);
        }

        // ==========================================
        // 2. QUERY DATA COMPLETE (Dari Tabel Core S_ResourceAssignment)
        // ==========================================
        String sqlComplete = "IsActive='Y' AND AssignStart >= CURRENT_DATE AND AssignStart < CURRENT_DATE + 1" 
                           + resourceFilterClause;
        
        List<MResourceAssignment> officialAssignments = new Query(Env.getCtx(), MResourceAssignment.Table_Name, sqlComplete, null).list();
        for (MResourceAssignment ra : officialAssignments) {
            SimpleCalendarEvent sce = new SimpleCalendarEvent();
            sce.setBeginDate(ra.getAssignStart());
            sce.setEndDate(ra.getAssignEnd());
            
            MResource resource = MResource.get(Env.getCtx(), ra.getS_Resource_ID());
            sce.setTitle("[READY] " + resource.getName() + " - " + ra.getName());
            
            int nsBookingID = ra.get_ValueAsInt("NS_Booking_ID");
            sce.setContent(String.valueOf(nsBookingID));
            
            sce.setHeaderColor("#67C23A"); // Hijau Penanda Resmi
            sce.setContentColor("#F0F9EB");
            calendarModel.add(sce);
        }
    }

    @Override
    public Component getComponent() {
        return this;
    }

    @Override
    public void refreshWindow() {
        loadTimelineData();
    }
}
