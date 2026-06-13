package org.nsoft.booking.model;

import java.sql.ResultSet;
import java.util.Properties;
import org.compiere.util.CLogger;

/**
 * Custom Model for Vehicle Booking Line (Passenger Detail)
 * Automatically generated extensions must extend X_NS_BookingLine
 */
public class MNSBookingLine extends X_NS_BookingLine {

    private static final long serialVersionUID = 1L;
    private static final CLogger log = CLogger.getCLogger(MNSBookingLine.class);

    /** Standard Constructor */
    public MNSBookingLine(Properties ctx, int NS_BookingLine_ID, String trxName) {
        super(ctx, NS_BookingLine_ID, trxName);
        if (NS_BookingLine_ID == 0) {
            setLine(10); // Default nomor urut awal
        }
    }

    /** Load Constructor */
    public MNSBookingLine(Properties ctx, ResultSet rs, String trxName) {
        super(ctx, rs, trxName);
    }

    @Override
    protected boolean beforeSave(boolean newRecord) {
        // 1. Ambil data dari dokumen Master-nya
        MNSBooking parent = new MNSBooking(getCtx(), getNS_Booking_ID(), get_TrxName());

        // 2. Validasi Bisnis: Mencegah penambahan penumpang jika dokumen utama sudah dikunci/diproses
        if (parent.isProcessed()) {
            log.saveError("Error", "Tidak dapat menambah atau mengubah data penumpang karena dokumen Booking sudah diproses/selesai.");
            return false;
        }

        // 3. Validasi Bisnis: Mencegah pemesan utama mendaftarkan dirinya sendiri lagi di tabel detail
        if (getAD_User_ID() == parent.getAD_User_ID()) {
            log.saveError("Error", "User ini sudah terdaftar sebagai Pemesan Utama / Driver di dokumen Master.");
            return false;
        }

        return true;
    }

    @Override
    protected boolean beforeDelete() {
        // Validasi Bisnis: Mencegah penghapusan penumpang jika dokumen utama sudah diproses
        MNSBooking parent = new MNSBooking(getCtx(), getNS_Booking_ID(), get_TrxName());
        if (parent.isProcessed()) {
            log.saveError("Error", "Data penumpang tidak dapat dihapus karena dokumen Booking sudah diproses/selesai.");
            return false;
        }
        return true;
    }
}
