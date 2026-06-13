CREATE TABLE NS_Booking (
    -- Primary Key
    NS_Booking_ID NUMERIC(10) NOT NULL,
    
    -- iDempiere Mandatori / Audit Columns
    AD_Client_ID NUMERIC(10) NOT NULL,
    AD_Org_ID NUMERIC(10) NOT NULL,
    IsActive CHAR(1) DEFAULT 'Y' NOT NULL,
    Created TIMESTAMP DEFAULT NOW() NOT NULL,
    CreatedBy NUMERIC(10) NOT NULL,
    Updated TIMESTAMP DEFAULT NOW() NOT NULL,
    UpdatedBy NUMERIC(10) NOT NULL,
    NS_Booking_UU VARCHAR(36) DEFAULT NULL,
    
    -- Identitas Dokumen
    DocumentNo VARCHAR(30) NOT NULL,               -- Nomor dokumen otomatis (misal: BKG-2026-0001)
    C_DocType_ID NUMERIC(10),                       -- Tipe dokumen (untuk pengelompokan sequence)
    Description VARCHAR(255),                      -- Tujuan / keperluan perjalanan
    
    -- Kolom Utama Bisnis (Spesifik Booking Kendaraan)
    S_Resource_ID NUMERIC(10) NOT NULL,            -- FK ke tabel Kendaraan (S_Resource)
    AD_User_ID NUMERIC(10) NOT NULL,               -- Driver / Pemesan Utama
    StartDate TIMESTAMP NOT NULL,                  -- Waktu Mulai Pinjam
    EndDate TIMESTAMP NOT NULL,                    -- Perkiraan Waktu Kembali
    
    -- Kolom Wajib untuk Workflow / Approval (DocAction Ready)
    DocStatus CHAR(2) DEFAULT 'DR' NOT NULL,       -- Status Dokumen (DR=Draft, CO=Completed, IP=In Progress, dll)
    DocAction CHAR(2) DEFAULT 'CO' NOT NULL,       -- Tombol Aksi Dokumen (CO=Complete, VO=Void, dll)
    Processing CHAR(1) DEFAULT 'N' NOT NULL,       -- Flag saat dokumen sedang diproses latar belakang
    Processed CHAR(1) DEFAULT 'N' NOT NULL,        -- Flag jika dokumen sudah selesai/terkunci (Y/N)
    IsApproved CHAR(1) DEFAULT 'Y' NOT NULL,       -- Flag status persetujuan (Y/N)
    
    CONSTRAINT ns_booking_pk PRIMARY KEY (NS_Booking_ID),
    CONSTRAINT ns_booking_documentno_uq UNIQUE (AD_Client_ID, DocumentNo),
    CONSTRAINT ns_booking_resource_fk FOREIGN KEY (S_Resource_ID) REFERENCES S_Resource(S_Resource_ID)
);
