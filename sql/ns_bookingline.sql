CREATE TABLE NS_BookingLine (
    -- Primary Key
    NS_BookingLine_ID NUMERIC(10) NOT NULL,
    
    -- Foreign Key ke Master (Parent)
    NS_Booking_ID NUMERIC(10) NOT NULL,
    
    -- iDempiere Mandatori / Audit Columns
    AD_Client_ID NUMERIC(10) NOT NULL,
    AD_Org_ID NUMERIC(10) NOT NULL,
    IsActive CHAR(1) DEFAULT 'Y' NOT NULL,
    Created TIMESTAMP DEFAULT NOW() NOT NULL,
    CreatedBy NUMERIC(10) NOT NULL,
    Updated TIMESTAMP DEFAULT NOW() NOT NULL,
    UpdatedBy NUMERIC(10) NOT NULL,
    NS_BookingLine_UU VARCHAR(36) DEFAULT NULL,
    
    -- Kolom Detail Bisnis
    Line NUMERIC(10) NOT NULL,                     -- Nomor Urut Line (10, 20, 30...)
    AD_User_ID NUMERIC(10) NOT NULL,               -- FK ke AD_User (User yang ikut menumpang)
    Description VARCHAR(255),                      -- Catatan tambahan/keperluan penumpang tersebut
    
    CONSTRAINT ns_bookingline_pk PRIMARY KEY (NS_BookingLine_ID),
    CONSTRAINT ns_bookingline_parent_fk FOREIGN KEY (NS_Booking_ID) REFERENCES NS_Booking(NS_Booking_ID) ON DELETE CASCADE
);
