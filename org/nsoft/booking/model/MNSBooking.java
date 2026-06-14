package org.nsoft.booking.model;

import java.math.BigDecimal;
import java.sql.ResultSet;
import java.util.List;
import java.util.Properties;
import java.util.UUID;
import org.compiere.model.Query;
import org.compiere.model.ModelValidationEngine;
import org.compiere.model.ModelValidator;
import org.compiere.process.DocAction;
import org.compiere.process.DocumentEngine;
import org.compiere.util.CLogger;

/**
 * Custom Model for Vehicle Booking System (Master Document)
 * Automatically generated extensions must extend X_NS_Booking
 */
public class MNSBooking extends X_NS_Booking implements DocAction {

    private static final long serialVersionUID = 1L;
    private static final CLogger log = CLogger.getCLogger(MNSBooking.class);

    private String m_processMsg = null;

    /** Standard Constructor */
    public MNSBooking(Properties ctx, int NS_Booking_ID, String trxName) {
        super(ctx, NS_Booking_ID, trxName);
        if (NS_Booking_ID == 0) {
            setDocStatus(DOCSTATUS_Drafted);
            setDocAction(DOCACTION_Complete);
            setProcessed(false);
            setProcessing(false);
            setIsApproved(false);
        }
    }

    /** Load Constructor */
    public MNSBooking(Properties ctx, ResultSet rs, String trxName) {
        super(ctx, rs, trxName);
    }

    /**
     * Get Lines (Daftar Penumpang)
     * @return array of MNSBookingLine
     */
    public MNSBookingLine[] getLines() {
        String whereClause = "NS_Booking_ID=?";
        List<MNSBookingLine> list = new Query(getCtx(), X_NS_BookingLine.Table_Name, whereClause, get_TrxName())
                .setParameters(getNS_Booking_ID())
                .setOrderBy(X_NS_BookingLine.COLUMNNAME_Line)
                .list();
        
        return list.toArray(new MNSBookingLine[list.size()]);
    }

    @Override
protected boolean beforeSave(boolean newRecord) {
    // 1. Validasi standar tanggal
    if (getEndDate().before(getStartDate())) {
        log.saveError("Error", "Waktu selesai tidak boleh sebelum waktu mulai.");
        return false;
    }

    // 2. Cek Overlap hanya dengan dokumen yang SUDAH COMPLETED (Sudah Resmi Terbooking)
    if (isOverlapWithConfirmedBooking()) {
        log.saveError("Error", "Kendaraan ini sudah resmi di-booking oleh user lain pada rentang waktu tersebut.");
        return false;
    }

    return true;
}
private boolean isOverlapWithConfirmedBooking() {
    // LOGIKA IoT: 
    // Jika StopDate sudah terisi (mobil sudah lewat gerbang masuk), gunakan StopDate.
    // Jika StopDate masih null, periksa: apakah waktu sekarang sudah melewati EndDate?
    // Jika YA (artinya mobil telat pulang), gunakan waktu sekarang (NOW()) sebagai batas akhir, karena mobil masih di jalan!
    // Jika TIDAK (mobil masih dalam rentang waktu normal), gunakan estimasi EndDate.

    String whereClause = "NS_Booking_ID != ? AND S_Resource_ID = ? "
                       + "AND DocStatus = 'CO' " 
                       + "AND ( "
                       + "  (StartDate <= ? AND CASE "
                       + "                      WHEN StopDate IS NOT NULL THEN StopDate "
                       + "                      WHEN NOW() > EndDate THEN NOW() "
                       + "                      ELSE EndDate "
                       + "                    END >= ?) OR " 
                       + "  (StartDate <= ? AND CASE "
                       + "                      WHEN StopDate IS NOT NULL THEN StopDate "
                       + "                      WHEN NOW() > EndDate THEN NOW() "
                       + "                      ELSE EndDate "
                       + "                    END >= ?) OR "
                       + "  (? <= StartDate AND ? >= CASE "
                       + "                             WHEN StopDate IS NOT NULL THEN StopDate "
                       + "                             WHEN NOW() > EndDate THEN NOW() "
                       + "                             ELSE EndDate "
                       + "                           END) "
                       + ")";

    int count = new Query(getCtx(), Table_Name, whereClause, get_TrxName())
            .setParameters(
                getNS_Booking_ID(), 
                getS_Resource_ID(),
                getStartDate(), getStartDate(),
                getEndDate(), getEndDate(),
                getStartDate(), getEndDate()
            )
            .count();

    return count > 0;
}

    // =========================================================================
    // IMPLEMENTASI INTERFACE DOCACTION (WORKFLOW & APPROVAL READY)
    // =========================================================================

    @Override
    public boolean processIt(String action) throws Exception {
        m_processMsg = null;
        DocumentEngine engine = new DocumentEngine(this, getDocStatus());
        return engine.processIt(action, getDocAction());
    }
@Override
public String prepareIt() {
    log.info("prepareIt - " + toString());
    
    // Cek ulang saat akan di-approve, siapa tahu selama dokumen ini jadi Draft,
    // sudah ada orang lain yang menyalip dan statusnya sudah Completed duluan.
    if (isOverlapWithConfirmedBooking()) {
        m_processMsg = "Gagal memproses. Kendaraan sudah terlanjur disetujui untuk booking user lain.";
        return DocAction.STATUS_Invalid;
    }
    
    return DocAction.STATUS_InProgress;
}
    //@Override
   // public String prepareIt() {
       // log.info("prepareIt - " + toString());
      //  m_processMsg = ModelValidationEngine.get().fireDocValidate(this, ModelValidator.TIMING_BEFORE_PREPARE);
      //  if (m_processMsg != null)
        //    return DocAction.STATUS_Invalid;

        // Tambahkan logika validasi di sini jika diperlukan sebelum masuk ke workflow
        // Contoh: Cek ketersediaan mobil (S_Resource_ID) di rentang tanggal tersebut.

      //  if (!DOCACTION_Complete.equals(getDocAction()))
         //   setDocAction(DOCACTION_Complete);
        
      //  return DocAction.STATUS_InProgress;
 //   }

    @Override
    public boolean approveIt() {
        log.info("approveIt - " + toString());
        setIsApproved(true);
        return true;
    }

    @Override
    public boolean rejectIt() {
        log.info("rejectIt - " + toString());
        setIsApproved(false);
        return true;
    }
    
    @Override
    public String completeIt() {
    log.info("completeIt - " + toString());
    
        // -------------------------------------------------------------------------
        // LOGIKA SINKRONISASI KE CORE IDEMPIERE (MResourceAssignment)
        // -------------------------------------------------------------------------
        MResourceAssignment assignment = null;
    
        // Cek apakah record ini sudah pernah sinkron sebelumnya (mencegah duplikasi saat re-activate)
        int existingAssignmentID = get_ValueAsInt("S_ResourceAssignment_ID"); 
    
        if (existingAssignmentID > 0) {
            assignment = new MResourceAssignment(getCtx(), existingAssignmentID, get_TrxName());
        } else {
            assignment = new MResourceAssignment(getCtx(), 0, get_TrxName());
        }
    
        // Isi data Core dari data Custom NS_Booking Anda
        assignment.setAD_Org_ID(getAD_Org_ID());
        assignment.setS_Resource_ID(getS_Resource_ID());
        assignment.setName("Booking Kendaraan: " + getDocumentNo());
        assignment.setDescription(getDescription());
        assignment.setAssignDateFrom(getStartDate());
        assignment.setAssignDateTo(getEndDate());
    
        // Simpan ke tabel core
        if (assignment.save(get_TrxName())) {
           // Simpan balik ID Resource Assignment ke tabel NS_Booking Anda sebagai referensi (FK balik)
           this.set_ValueNoCheck("S_ResourceAssignment_ID", assignment.getS_ResourceAssignment_ID());
        } else {
           m_processMsg = "Gagal menyinkronkan data ke sistem Core Resource Assignment.";
           return DocAction.STATUS_Invalid;
        }
        // -------------------------------------------------------------------------
    
        setProcessed(true);
        setDocAction(DOCACTION_Close);
        return DocAction.STATUS_Completed;
    }
    
    @Override
    public boolean voidIt() {
      log.info("voidIt - " + toString());
    
      // Hapus atau batalkan alokasi di tabel core jika ada
      int existingAssignmentID = get_ValueAsInt("S_ResourceAssignment_ID");
      if (existingAssignmentID > 0) {
        MResourceAssignment assignment = new MResourceAssignment(getCtx(), existingAssignmentID, get_TrxName());
        
          // Opsi 1: Hapus datanya dari core
          assignment.delete(true, get_TrxName());
        
          // Opsi 2: Jika tidak mau dihapus, Anda bisa set tanggalnya jadi null / diubah namanya menjadi "CANCELED"
          this.set_ValueNoCheck("S_ResourceAssignment_ID", null);
       }
    
        setProcessed(true);
        setDocAction(DOCACTION_None);
        return true;
    }
    
    @Override
    public boolean closeIt() {
        log.info("closeIt - " + toString());
        setDocAction(DOCACTION_None);
        return true;
    }

    @Override
    public boolean reverseCorrectIt() {
        return voidIt();
    }

    @Override
    public boolean reverseAccntIt() {
        return false;
    }

    @Override
    public boolean reActivateIt() {
        log.info("reActivateIt - " + toString());
        setDocAction(DOCACTION_Complete);
        setProcessed(false);
        return true;
    }

    @Override
    public boolean unlockIt() {
        log.info("unlockIt - " + toString());
        setProcessing(false);
        return true;
    }

    @Override
    public boolean invalidateIt() {
        log.info("invalidateIt - " + toString());
        setDocStatus(DOCSTATUS_Invalid);
        return true;
    }

    @Override
    public String getSummary() {
        return getDocumentNo() + " - " + getDescription();
    }

    @Override
    public int getAD_Window_ID() {
        // TODO: Ganti angka 0 dengan AD_Window_ID dari Window kustom "Vehicle Booking" Anda
        return 0; 
    }

    @Override
    public UUID getUUID() {
        String uuid = get_ValueAsString("NS_Booking_UU");
        return uuid != null && !uuid.isEmpty() ? UUID.fromString(uuid) : null;
    }

    @Override
    public String getProcessMsg() {
        return m_processMsg;
    }

    @Override
    public BigDecimal getApprovalAmt() {
        return BigDecimal.ZERO;
    }

    @Override
    public int getC_Currency_ID() {
        return 0;
    }
}
