package com.uj.appstorysautopaymanager.data.local.database

import android.content.Context
import androidx.room.Database
import androidx.room.Room
import androidx.room.RoomDatabase
import androidx.room.migration.Migration
import androidx.sqlite.db.SupportSQLiteDatabase
import com.uj.appstorysautopaymanager.data.local.dao.BillDao
import com.uj.appstorysautopaymanager.data.local.dao.CategoryDao
import com.uj.appstorysautopaymanager.data.local.dao.MandateDao
import com.uj.appstorysautopaymanager.data.local.dao.TransactionDao
import com.uj.appstorysautopaymanager.data.local.entity.Bill
import com.uj.appstorysautopaymanager.data.local.entity.Category
import com.uj.appstorysautopaymanager.data.local.entity.Mandate
import com.uj.appstorysautopaymanager.data.local.entity.Transaction
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import javax.inject.Provider

@Database(
    entities = [Bill::class, Transaction::class, Mandate::class, Category::class],
    version = 2,
    exportSchema = false
)
abstract class AppDatabase : RoomDatabase() {
    abstract fun billDao(): BillDao
    abstract fun transactionDao(): TransactionDao
    abstract fun mandateDao(): MandateDao
    abstract fun categoryDao(): CategoryDao

    companion object {
        private val MIGRATION_1_2 = object : Migration(1, 2) {
            override fun migrate(db: SupportSQLiteDatabase) {
                db.execSQL("ALTER TABLE mandates ADD COLUMN category TEXT NOT NULL DEFAULT ''")
                db.execSQL("ALTER TABLE mandates ADD COLUMN paymentApp TEXT NOT NULL DEFAULT ''")
            }
        }

        fun buildDatabase(context: Context): AppDatabase {
            return Room.databaseBuilder(
                context.applicationContext,
                AppDatabase::class.java,
                "autopay_manager_db"
            )
            .addMigrations(MIGRATION_1_2)
            .addCallback(object : RoomDatabase.Callback() {
                override fun onCreate(db: SupportSQLiteDatabase) {
                    super.onCreate(db)
                    db.execSQL("INSERT INTO categories (name, iconName, isSystem) VALUES ('OTT', 'Tv', 1)")
                    db.execSQL("INSERT INTO categories (name, iconName, isSystem) VALUES ('Music', 'MusicNote', 1)")
                    db.execSQL("INSERT INTO categories (name, iconName, isSystem) VALUES ('Mobile', 'PhoneAndroid', 1)")
                    db.execSQL("INSERT INTO categories (name, iconName, isSystem) VALUES ('EMI', 'AccountBalance', 1)")
                    db.execSQL("INSERT INTO categories (name, iconName, isSystem) VALUES ('Credit Card', 'CreditCard', 1)")
                    db.execSQL("INSERT INTO categories (name, iconName, isSystem) VALUES ('Electricity', 'ElectricBolt', 1)")
                    db.execSQL("INSERT INTO categories (name, iconName, isSystem) VALUES ('Gas', 'LocalGasStation', 1)")
                    db.execSQL("INSERT INTO categories (name, iconName, isSystem) VALUES ('Water', 'WaterDrop', 1)")
                    db.execSQL("INSERT INTO categories (name, iconName, isSystem) VALUES ('Insurance', 'Shield', 1)")
                    db.execSQL("INSERT INTO categories (name, iconName, isSystem) VALUES ('Others', 'Category', 1)")

                    // Default AutoPay Mandates matching Image 4
                    db.execSQL("INSERT INTO mandates (merchant, amount, frequency, nextExpectedDebit, bank, status, referenceNumber, category, paymentApp) VALUES ('Netflix', 630.0, 'Manual', 1786500000000, 'HDFC Bank', 'ACTIVE', 'MAN1001', 'OTT', '')")
                    db.execSQL("INSERT INTO mandates (merchant, amount, frequency, nextExpectedDebit, bank, status, referenceNumber, category, paymentApp) VALUES ('Spotify', 480.0, 'Automatic', 1786759200000, 'ICICI Bank', 'ACTIVE', 'MAN1002', 'Music', '')")
                    db.execSQL("INSERT INTO mandates (merchant, amount, frequency, nextExpectedDebit, bank, status, referenceNumber, category, paymentApp) VALUES ('Amazon Prime', 999.0, 'Manual', 1787191200000, 'SBI Bank', 'ACTIVE', 'MAN1003', 'OTT', '')")
                    db.execSQL("INSERT INTO mandates (merchant, amount, frequency, nextExpectedDebit, bank, status, referenceNumber, category, paymentApp) VALUES ('Hulu', 720.0, 'Automatic', 1787623200000, 'Axis Bank', 'ACTIVE', 'MAN1004', 'OTT', '')")
                    db.execSQL("INSERT INTO mandates (merchant, amount, frequency, nextExpectedDebit, bank, status, referenceNumber, category, paymentApp) VALUES ('Disney+', 899.0, 'Manual', 1788055200000, 'Kotak Bank', 'ACTIVE', 'MAN1005', 'OTT', '')")

                    // Default Passbook Transactions matching Image 5
                    db.execSQL("INSERT INTO transactions (smsId, merchant, amount, date, bankName, accountNumber, referenceNumber, transactionType, category, smsBody, isAutoPay) VALUES ('sms_1', 'State Bank of India . XX...', 27.0, 1783900800000, 'SBI BANK', 'XX1234', 'REF001', 'DEBIT', 'Others', 'Rs.27 debited from SBI account', 1)")
                    db.execSQL("INSERT INTO transactions (smsId, merchant, amount, date, bankName, accountNumber, referenceNumber, transactionType, category, smsBody, isAutoPay) VALUES ('sms_2', 'State Bank of India . XX...', 27.0, 1783814400000, 'SBI BANK', 'XX1234', 'REF002', 'DEBIT', 'Others', 'Rs.27 debited from SBI account', 1)")
                    db.execSQL("INSERT INTO transactions (smsId, merchant, amount, date, bankName, accountNumber, referenceNumber, transactionType, category, smsBody, isAutoPay) VALUES ('sms_3', 'HDFC Bank . AB...', 15.0, 1783641600000, 'HDFC BANK', 'AB5678', 'REF003', 'DEBIT', 'Others', 'Rs.15 debited from HDFC account', 1)")
                    db.execSQL("INSERT INTO transactions (smsId, merchant, amount, date, bankName, accountNumber, referenceNumber, transactionType, category, smsBody, isAutoPay) VALUES ('sms_4', 'HDFC Bank . AB...', 15.0, 1783641600000, 'HDFC BANK', 'AB5678', 'REF004', 'DEBIT', 'Others', 'Rs.15 debited from HDFC account', 1)")
                    db.execSQL("INSERT INTO transactions (smsId, merchant, amount, date, bankName, accountNumber, referenceNumber, transactionType, category, smsBody, isAutoPay) VALUES ('sms_5', 'ICICI Bank . CD...', 22.0, 1783382400000, 'ICICI BANK', 'CD9012', 'REF005', 'DEBIT', 'Others', 'Rs.22 debited from ICICI account', 1)")
                    db.execSQL("INSERT INTO transactions (smsId, merchant, amount, date, bankName, accountNumber, referenceNumber, transactionType, category, smsBody, isAutoPay) VALUES ('sms_6', 'Axis Bank . EF...', 30.0, 1783123200000, 'AXIS BANK', 'EF3456', 'REF006', 'DEBIT', 'Others', 'Rs.30 debited from Axis account', 1)")
                }
            })
            .build()
        }
    }
}
