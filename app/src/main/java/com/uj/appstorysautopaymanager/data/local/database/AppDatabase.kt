package com.uj.appstorysautopaymanager.data.local.database

import android.content.Context
import androidx.room.Database
import androidx.room.Room
import androidx.room.RoomDatabase
import androidx.room.migration.Migration
import androidx.sqlite.db.SupportSQLiteDatabase
import com.uj.appstorysautopaymanager.data.local.dao.AuthTokenDao
import com.uj.appstorysautopaymanager.data.local.dao.BillDao
import com.uj.appstorysautopaymanager.data.local.dao.CategoryDao
import com.uj.appstorysautopaymanager.data.local.dao.MandateDao
import com.uj.appstorysautopaymanager.data.local.dao.NotificationDao
import com.uj.appstorysautopaymanager.data.local.dao.TransactionDao
import com.uj.appstorysautopaymanager.data.local.entity.AuthTokenEntity
import com.uj.appstorysautopaymanager.data.local.entity.Bill
import com.uj.appstorysautopaymanager.data.local.entity.Category
import com.uj.appstorysautopaymanager.data.local.entity.Mandate
import com.uj.appstorysautopaymanager.data.local.entity.NotificationEntity
import com.uj.appstorysautopaymanager.data.local.entity.Transaction
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import javax.inject.Provider

@Database(
    entities = [Bill::class, Transaction::class, Mandate::class, Category::class, NotificationEntity::class, AuthTokenEntity::class],
    version = 11,
    exportSchema = false
)
abstract class AppDatabase : RoomDatabase() {
    abstract fun billDao(): BillDao
    abstract fun transactionDao(): TransactionDao
    abstract fun mandateDao(): MandateDao
    abstract fun categoryDao(): CategoryDao
    abstract fun notificationDao(): NotificationDao
    abstract fun authTokenDao(): AuthTokenDao

    companion object {
        private val MIGRATION_1_2 = object : Migration(1, 2) {
            override fun migrate(db: SupportSQLiteDatabase) {
                db.execSQL("ALTER TABLE mandates ADD COLUMN category TEXT NOT NULL DEFAULT ''")
                db.execSQL("ALTER TABLE mandates ADD COLUMN paymentApp TEXT NOT NULL DEFAULT ''")
            }
        }

        // Drops exact-smsId duplicate rows (safe: identical smsId can only mean the same
        // physical SMS was inserted twice, e.g. a redelivered SMS_RECEIVED broadcast) before
        // enforcing uniqueness so the CREATE UNIQUE INDEX below can't fail on existing data.
        // Does NOT catch duplicates from the older address+timestamp smsId scheme, where the
        // live-received and inbox-backfill copies of the same SMS got two different smsId
        // strings - those need a fresh scan (smsId is now content-based, see SmsReceiver /
        // TransactionViewModel) to stop recurring; a data clear/reinstall flushes old ones.
        private val MIGRATION_2_3 = object : Migration(2, 3) {
            override fun migrate(db: SupportSQLiteDatabase) {
                db.execSQL("DELETE FROM transactions WHERE id NOT IN (SELECT MIN(id) FROM transactions GROUP BY smsId)")
                db.execSQL("CREATE UNIQUE INDEX IF NOT EXISTS index_transactions_smsId ON transactions(smsId)")
            }
        }

        private val MIGRATION_3_4 = object : Migration(3, 4) {
            override fun migrate(db: SupportSQLiteDatabase) {
                db.execSQL(
                    """CREATE TABLE IF NOT EXISTS notifications (
                        id INTEGER PRIMARY KEY AUTOINCREMENT NOT NULL,
                        title TEXT NOT NULL,
                        body TEXT NOT NULL,
                        timestamp INTEGER NOT NULL,
                        category TEXT NOT NULL,
                        isWarning INTEGER NOT NULL,
                        isUnread INTEGER NOT NULL
                    )"""
                )
            }
        }

        private val MIGRATION_4_5 = object : Migration(4, 5) {
            override fun migrate(db: SupportSQLiteDatabase) {
                db.execSQL(
                    """CREATE TABLE IF NOT EXISTS auth_token (
                        uid TEXT PRIMARY KEY NOT NULL,
                        phoneNumber TEXT NOT NULL,
                        idToken TEXT NOT NULL,
                        issuedAt INTEGER NOT NULL
                    )"""
                )
            }
        }

        private val MIGRATION_5_6 = object : Migration(5, 6) {
            override fun migrate(db: SupportSQLiteDatabase) {
                db.execSQL("ALTER TABLE auth_token ADD COLUMN accessToken TEXT NOT NULL DEFAULT ''")
                db.execSQL("ALTER TABLE auth_token ADD COLUMN refreshToken TEXT NOT NULL DEFAULT ''")
            }
        }

        private val MIGRATION_6_7 = object : Migration(6, 7) {
            override fun migrate(db: SupportSQLiteDatabase) {
                db.execSQL("ALTER TABLE transactions ADD COLUMN synced INTEGER NOT NULL DEFAULT 0")
                db.execSQL("ALTER TABLE transactions ADD COLUMN backendPaymentId TEXT")
            }
        }

        private val MIGRATION_7_8 = object : Migration(7, 8) {
            override fun migrate(db: SupportSQLiteDatabase) {
                db.execSQL("ALTER TABLE auth_token ADD COLUMN name TEXT NOT NULL DEFAULT ''")
            }
        }

        // Google sign-in replaces phone OTP - store the account email instead of a phone number.
        // Additive: the old phoneNumber column stays (written "") so existing rows don't break.
        private val MIGRATION_8_9 = object : Migration(8, 9) {
            override fun migrate(db: SupportSQLiteDatabase) {
                db.execSQL("ALTER TABLE auth_token ADD COLUMN email TEXT NOT NULL DEFAULT ''")
            }
        }

        // Mandate.source - how the mandate was created (see the entity).
        private val MIGRATION_9_10 = object : Migration(9, 10) {
            override fun migrate(db: SupportSQLiteDatabase) {
                db.execSQL("ALTER TABLE mandates ADD COLUMN source TEXT NOT NULL DEFAULT ''")
            }
        }

        // Back-fill Mandate.source from `bank`: EmailParser set it to "Google Play" for Play-billed
        // subs and to the vendor display name for every other email source; AutoPayScreen left it
        // "". Its own migration (not folded into 9_10) so devices already at v10 pick it up - a
        // Gmail re-sync can't, GmailSyncWorker skips already-processed message ids.
        private val MIGRATION_10_11 = object : Migration(10, 11) {
            override fun migrate(db: SupportSQLiteDatabase) {
                db.execSQL("UPDATE mandates SET source = 'GOOGLE_PLAY' WHERE bank = 'Google Play'")
                db.execSQL("UPDATE mandates SET source = 'EMAIL' WHERE source = '' AND bank <> ''")
                db.execSQL("UPDATE mandates SET source = 'MANUAL' WHERE source = '' AND bank = ''")
            }
        }

        fun buildDatabase(context: Context): AppDatabase {
            return Room.databaseBuilder(
                context.applicationContext,
                AppDatabase::class.java,
                "autopay_manager_db"
            )
            .addMigrations(MIGRATION_1_2, MIGRATION_2_3, MIGRATION_3_4, MIGRATION_4_5, MIGRATION_5_6, MIGRATION_6_7, MIGRATION_7_8, MIGRATION_8_9, MIGRATION_9_10, MIGRATION_10_11)
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
                }
            })
            .build()
        }
    }
}
