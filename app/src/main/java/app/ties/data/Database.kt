package app.ties.data

import android.content.Context
import androidx.room.Dao
import androidx.room.Database
import androidx.room.Delete
import androidx.room.Entity
import androidx.room.ForeignKey
import androidx.room.Index
import androidx.room.Insert
import androidx.room.PrimaryKey
import androidx.room.Query
import androidx.room.Room
import androidx.room.RoomDatabase
import androidx.room.Transaction
import androidx.room.Update
import kotlinx.coroutines.flow.Flow

enum class Circle { FAMILY, FRIENDS, CHILDHOOD, WORK, OTHER }

enum class TouchKind { CALL, MESSAGE, MEETING }

@Entity(tableName = "people")
data class Person(
    @PrimaryKey(autoGenerate = true) val id: Long = 0,
    val name: String,
    val circle: Circle,
    val phone: String? = null,
    /** Desired rhythm: get in touch at least once every this many days. */
    val everyDays: Int,
    val birthdayMonth: Int? = null,
    val birthdayDay: Int? = null,
    /** Things worth remembering about the person (new university, kids' names, ...). */
    val notes: String = "",
    val createdAt: Long,
    /** When the countdown starts if nothing was logged yet: the user's estimate of the last contact. */
    val sinceAt: Long,
    /** Latest logged touch; kept in sync with [Touch] rows by [PeopleDao]. */
    val lastTouchAt: Long? = null,
)

@Entity(
    tableName = "touches",
    foreignKeys = [ForeignKey(entity = Person::class, parentColumns = ["id"], childColumns = ["personId"], onDelete = ForeignKey.CASCADE)],
    indices = [Index("personId")],
)
data class Touch(
    @PrimaryKey(autoGenerate = true) val id: Long = 0,
    val personId: Long,
    val at: Long,
    val kind: TouchKind,
    val note: String = "",
)

@Dao
abstract class PeopleDao {
    @Query("SELECT * FROM people ORDER BY name COLLATE NOCASE")
    abstract fun observeAll(): Flow<List<Person>>

    @Query("SELECT * FROM people")
    abstract suspend fun all(): List<Person>

    @Query("SELECT * FROM people WHERE id = :id")
    abstract fun observe(id: Long): Flow<Person?>

    @Query("SELECT * FROM people WHERE id = :id")
    abstract suspend fun get(id: Long): Person?

    @Insert
    abstract suspend fun insert(person: Person): Long

    @Update
    abstract suspend fun update(person: Person)

    @Delete
    abstract suspend fun delete(person: Person)

    @Query("SELECT * FROM touches WHERE personId = :personId ORDER BY at DESC")
    abstract fun observeTouches(personId: Long): Flow<List<Touch>>

    @Insert
    protected abstract suspend fun insertTouch(touch: Touch): Long

    @Delete
    protected abstract suspend fun deleteTouch(touch: Touch)

    @Query("UPDATE people SET lastTouchAt = (SELECT MAX(at) FROM touches WHERE personId = :personId) WHERE id = :personId")
    protected abstract suspend fun syncLastTouch(personId: Long)

    @Transaction
    open suspend fun logTouch(touch: Touch) {
        insertTouch(touch)
        syncLastTouch(touch.personId)
    }

    @Transaction
    open suspend fun removeTouch(touch: Touch) {
        deleteTouch(touch)
        syncLastTouch(touch.personId)
    }
}

@Database(entities = [Person::class, Touch::class], version = 1)
abstract class TiesDatabase : RoomDatabase() {
    abstract fun people(): PeopleDao

    companion object {
        fun create(context: Context): TiesDatabase =
            Room.databaseBuilder(context.applicationContext, TiesDatabase::class.java, "ties.db").build()
    }
}
