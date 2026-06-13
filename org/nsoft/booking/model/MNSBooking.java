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
        // Validasi Bisnis: Pastikan Waktu Selesai tidak mendahului Waktu Mulai
        if (getEndDate().before(getStartDate())) {
            log.saveError("Error", "Waktu selesai (End Date) tidak boleh sebelum waktu mulai (Start Date).");
            return false;
        }
        return true;
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
        m_processMsg = ModelValidationEngine.get().fireDocValidate(this, ModelValidator.TIMING_BEFORE_PREPARE);
        if (m_processMsg != null)
            return DocAction.STATUS_Invalid;

        // Tambahkan logika validasi di sini jika diperlukan sebelum masuk ke workflow
        // Contoh: Cek ketersediaan mobil (S_Resource_ID) di rentang tanggal tersebut.

        if (!DOCACTION_Complete.equals(getDocAction()))
            setDocAction(DOCACTION_Complete);
        
        return DocAction.STATUS_InProgress;
    }

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
        
        // Logika Eksekusi Utama Saat Dokumen Disetujui (Completed)
        // Di sinilah tim Anda nanti bisa menyisipkan pemicu (trigger) untuk Push Notification.
        
        setProcessed(true);
        setDocAction(DOCACTION_Close);
        return DocAction.STATUS_Completed;
    }

    @Override
    public boolean voidIt() {
        log.info("voidIt - " + toString());
        // Batalkan pesanan, buka kembali slot resource kendaraan
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
